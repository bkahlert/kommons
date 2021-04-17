package koodies.docker

import koodies.CallableProperty
import koodies.builder.Builder
import koodies.builder.Init
import koodies.builder.mapBuild
import koodies.jvm.daemon
import koodies.concurrent.execute
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.concurrent.process.IO
import koodies.exec.Exec
import koodies.exec.ExecTerminationCallback
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.output
import koodies.concurrent.process.process
import koodies.concurrent.process.processSilently
import koodies.concurrent.process.terminationLoggingProcessor
import koodies.concurrent.scriptOutputContains
import koodies.concurrent.toExec
import koodies.docker.DockerImage.ImageContext
import koodies.docker.DockerRunCommandLine.Companion
import koodies.logging.RenderingLogger
import koodies.math.absoluteValue
import koodies.math.div
import koodies.math.toBigDecimal
import koodies.number.formatToExactDecimals
import koodies.provideDelegate
import koodies.unit.Size
import koodies.unit.UnitPrefix
import koodies.unit.getSymbol
import java.math.BigDecimal
import java.math.RoundingMode.FLOOR
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Entrypoint to ease discovery of Docker related features.
 */
public object Docker {

    /**
     * Entry point for [DockerImage] related features like pulling images.
     */
    public val images: DockerImage.Companion = DockerImage.Companion

    /**
     * Entry point for [DockerContainer] related features like starting containers.
     */
    public val containers: DockerContainer.Companion = DockerContainer.Companion

    /**
     * Whether the Docker engine itself is running.
     */
    public val engineRunning: Boolean get() = !scriptOutputContains("docker info", "error")

    /**
     * Whether a Docker container with the given [name] is running.
     *
     * @see DockerContainer
     */
    public fun containerRunning(name: String): Boolean = DockerContainer.from(name).isRunning
    

//    /**
//     * Builds a [DockerSearchCommandLine] and executes it.
//     */
//    public val search: (Init<SearchContext> /* = koodies.docker.DockerSearchCommandLine.Companion.SearchContext.() -> kotlin.Unit */)
//    -> Exec by DockerSearchCommandLine.mapBuild { it.execute().output() }
//
//    /**
//     * Builds a [DockerSearchCommandLine] and executes it using `this` [RenderingLogger].
//     */
//    public val RenderingLogger?.search: Builder<Init<SearchContext>, Exec> by CallableProperty { thisRef: RenderingLogger?, _ ->
//        DockerSearchCommandLine.mapBuild { it.execute(processor = thisRef.toProcessor()) }
//    }


    /**
     * Builds a [DockerStartCommandLine] and executes it.
     */
    @Deprecated("no command line builders")
    public val start: (Init<DockerStartCommandLine.Companion.CommandContext> /* = koodies.docker.DockerStartCommandLine.Companion.StartContext.() -> kotlin.Unit */)
    -> Exec by DockerStartCommandLine.mapBuild { it.execute { null } }

    /**
     * Builds a [DockerStartCommandLine] and executes it using `this` [RenderingLogger].
     */
    @Deprecated("no command line builders")
    public val RenderingLogger?.start: Builder<Init<DockerStartCommandLine.Companion.CommandContext>, Exec> by CallableProperty { thisRef: RenderingLogger?, _ ->
        DockerStartCommandLine.mapBuild { with(thisRef) { it.execute { null } } }
    }

    /**
     * Builds a [DockerRunCommandLine] and executes it.
     */
    public val run: (Init<Companion.CommandContext> /* = koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext.() -> kotlin.Unit */)
    -> Exec by DockerRunCommandLine.mapBuild { it.execute { null } }

    /**
     * Builds a [DockerRunCommandLine] and executes it using `this` [RenderingLogger].
     */
    public val RenderingLogger?.run: Builder<Init<Companion.CommandContext>, Exec> by CallableProperty { thisRef: RenderingLogger?, _ ->
        DockerRunCommandLine.mapBuild { with(thisRef) { it.execute { null } } }
    }

    /**
     * Builds a [DockerStopCommandLine].
     */
    @Deprecated("no command line builders")
    public val stop: (Init<DockerStopCommandLine.Companion.CommandContext> /* = koodies.docker.DockerStopCommandLine.Companion.StopContext.() -> kotlin.Unit */) -> DockerStopCommandLine by DockerStopCommandLine

    /**
     * Builds a [DockerRemoveCommandLine].
     */
    @Deprecated("no command line builders")
    public val remove: (Init<DockerRemoveCommandLine.Companion.CommandContext> /* = koodies.docker.DockerRemoveCommandLine.Companion.RemoveContext.() -> kotlin.Unit */) -> DockerRemoveCommandLine by DockerRemoveCommandLine

    /**
     * Micro DSL to build a [DockerImage] in the style of:
     * - `DockerImage { "bkahlert" / "libguestfs" }`
     * - `DockerImage { "bkahlert" / "libguestfs" tag "latest" }`
     * - `DockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
     *
     * Convenience alias for [DockerImage].
     */
//    @Suppress("SpellCheckingInspection")
//    public fun image(init: ImageContext.() -> DockerImage): DockerImage = DockerImage(init)

    @Deprecated("use docker instead", replaceWith = ReplaceWith("docker"))
    public fun options(init: Init<DockerRunCommandLine.Options.Companion.OptionsContext>): DockerRunCommandLine.Options =
        DockerRunCommandLine.Options(init)

    @Deprecated("use docker instead", replaceWith = ReplaceWith("docker"))
    public fun commandLine(init: Init<CommandLineContext>): CommandLine =
        CommandLine(init)

    @Deprecated("use docker instead", replaceWith = ReplaceWith("docker"))
    public fun commandLine(image: DockerImage, options: DockerRunCommandLine.Options, commandLine: CommandLine): DockerRunCommandLine =
        DockerRunCommandLine(image, options, commandLine)

    /**
     * Explicitly stops the Docker container with the given [name] **asynchronously**.
     */
    public fun stop(name: String): Unit = stop { containers { +name } }.fireAndForget()

    /**
     * Explicitly (stops and) removes the Docker container with the given [name] **synchronously**.
     *
     * If needed even [forcibly].
     */
    public fun remove(name: String, forcibly: Boolean = false): String = remove {
        options { force using forcibly }
        containers { +name }
    }.execute { noopProcessor() }
        .apply { onExit.orTimeout(8, TimeUnit.SECONDS).get() }
        .output()
}

/**
 * Runs this command line in a daemon thread asynchronously and silently.
 *
 * Only if something goes wrong an exception is logged on the console.
 */
private fun DockerCommandLine.fireAndForget(
    execTerminationCallback: ExecTerminationCallback? = null,
) {
    daemon {
        toExec(null, execTerminationCallback)
            .processSilently().apply { waitFor() }
    }
}

private fun Path.dockerRunCommandLine(
    imageInit: ImageContext.() -> DockerImage,
    optionsInit: Init<DockerRunCommandLine.Options.Companion.OptionsContext>,
    arguments: Array<out String>,
) = DockerRunCommandLine {
    image(imageInit)
    options(optionsInit)
    commandLine {
        workingDirectory { this@dockerRunCommandLine }
        command { "" }
        arguments { addAll(arguments) }
    }
}

/* ALL DOCKER METHODS BELOW ALWAYS START THE PROCESS AND AND PROCESS IT ASYNCHRONOUSLY */

/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageInit]
 * - [DockerRunCommandLineOptions] built by the specified [optionsInit]
 * - specified [arguments]
 * in `this` [Path].
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
public fun Path.docker(
    imageInit: ImageContext.() -> DockerImage,
    optionsInit: Init<DockerRunCommandLine.Options.Companion.OptionsContext>,
    vararg arguments: String,
    execTerminationCallback: ExecTerminationCallback? = null,
): DockerProcess =
    dockerRunCommandLine(imageInit, optionsInit, arguments)
        .toExec(execTerminationCallback)
        .processSilently().apply { waitFor() }

/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageInit]
 * - [DockerRunCommandLineOptions] built by the specified [optionsInit]
 * - specified [arguments]
 * in `this` [Path].
 *
 * The output of the [DockerProcess] will be processed by the specified [processor].
 * You can use one of the provided [Processors] or implement one on your own, e.g.
 * - `docker(..., [Processors.loggingProcessor])` to prints all [IO] to the console (default)
 * - `docker(...) { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
public fun Path.docker(
    imageInit: ImageContext.() -> DockerImage,
    optionsInit: Init<DockerRunCommandLine.Options.Companion.OptionsContext>,
    vararg arguments: String,
    execTerminationCallback: ExecTerminationCallback? = null,
    processor: Processor<Exec>?,
): DockerProcess =
    dockerRunCommandLine(imageInit, optionsInit, arguments)
        .toExec(execTerminationCallback)
        .let { it.process({ sync }, processor ?: it.terminationLoggingProcessor()) }

/**
 * Runs a Docker process using the [DockerRunCommandLine] built by the
 * specified [init].
 *
 * The output of the [DockerProcess] will be processed by the specified [processor].
 * You can use one of the provided [Processors] or implement one on your own, e.g.
 * - `docker(..., [Processors.loggingProcessor])` to prints all [IO] to the console
 * - `docker(...) { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
public fun docker(
    init: Init<Companion.CommandContext>,
    execTerminationCallback: ExecTerminationCallback? = null,
    processor: Processor<DockerProcess>?,
): DockerProcess =
    DockerRunCommandLine(init)
        .toExec(execTerminationCallback)
        .let { it.process({ sync }, processor ?: it.terminationLoggingProcessor()) }


/**
 * Runs a Docker process using the [DockerRunCommandLine] built by the
 * specified [init].
 *
 * The output of the [DockerProcess] will be processed by the specified [processor].
 * You can use one of the provided [Processors] or implement one on your own, e.g.
 * - `docker(..., [Processors.loggingProcessor])` to prints all [IO] to the console (default)
 * - `docker(...) { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
public fun docker(
    processor: Processor<DockerProcess>?,
    execTerminationCallback: ExecTerminationCallback? = null,
    init: Init<Companion.CommandContext>,
): DockerProcess =
    DockerRunCommandLine(init)
        .toExec(execTerminationCallback)
        .let { it.process({ sync }, processor ?: it.terminationLoggingProcessor()) }
