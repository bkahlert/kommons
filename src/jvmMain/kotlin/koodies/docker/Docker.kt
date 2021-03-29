package koodies.docker

import koodies.CallableProperty
import koodies.builder.Builder
import koodies.builder.Init
import koodies.builder.mapBuild
import koodies.collections.head
import koodies.collections.tail
import koodies.concurrent.daemon
import koodies.concurrent.execute
import koodies.concurrent.output
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.attach
import koodies.concurrent.process.process
import koodies.concurrent.process.processSilently
import koodies.concurrent.script
import koodies.concurrent.scriptOutputContains
import koodies.concurrent.toManagedProcess
import koodies.docker.DockerImage.ImageContext
import koodies.docker.DockerRunCommandLine.Companion
import koodies.logging.RenderingLogger
import koodies.provideDelegate
import koodies.text.takeUnlessBlank
import java.nio.file.Path
import java.util.concurrent.TimeUnit


/**
 * Provides methods to create and interact with a [DockerProcess].
 */
public object Docker {

    public val image: DockerImage.Companion = DockerImage.Companion

    /**
     * Contains the locally existing docker images.
     */
    public val images: List<DockerImage>
        get() = script(null) { !"""docker image ls  --no-trunc --format "{{.Repository}}\t{{.Tag}}\t{{.Digest}}"""" }.output().lines()
            .map { line ->
                val (repoAndPath, tag, digest) = line.split("\t")
                val (repository, path) = repoAndPath.split("/").let { it.head to it.tail }
                DockerImage(repository, path, tag.takeUnlessBlank(), digest.takeUnlessBlank())
            }

    /**
     * Whether the Docker engine itself is running.
     */
    public val engineRunning: Boolean get() = !scriptOutputContains("docker info", "error")

    /**
     * Whether a Docker container with the given [name] is running.
     */
    public fun isContainerRunning(name: String): Boolean = name.let { sanitizedName ->
        scriptOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$sanitizedName${'$'}"""", sanitizedName)
    }

    /**
     * Whether a Docker container—no matter if it's running or not—exists.
     */
    public fun exists(name: String): Boolean = name.let { sanitizedName ->
        scriptOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$sanitizedName${'$'}" --all""", sanitizedName)
    }

//    /**
//     * Builds a [DockerSearchCommandLine] and executes it.
//     */
//    public val search: (Init<SearchContext> /* = koodies.docker.DockerSearchCommandLine.Companion.SearchContext.() -> kotlin.Unit */)
//    -> ManagedProcess by DockerSearchCommandLine.mapBuild { it.execute().output() }
//
//    /**
//     * Builds a [DockerSearchCommandLine] and executes it using `this` [RenderingLogger].
//     */
//    public val RenderingLogger?.search: Builder<Init<SearchContext>, ManagedProcess> by CallableProperty { thisRef: RenderingLogger?, _ ->
//        DockerSearchCommandLine.mapBuild { it.execute(processor = thisRef.toProcessor()) }
//    }


    /**
     * Builds a [DockerStartCommandLine] and executes it.
     */
    public val start: (Init<DockerStartCommandLine.Companion.CommandContext> /* = koodies.docker.DockerStartCommandLine.Companion.StartContext.() -> kotlin.Unit */)
    -> ManagedProcess by DockerStartCommandLine.mapBuild { it.execute { null } }

    /**
     * Builds a [DockerStartCommandLine] and executes it using `this` [RenderingLogger].
     */
    public val RenderingLogger?.start: Builder<Init<DockerStartCommandLine.Companion.CommandContext>, ManagedProcess> by CallableProperty { thisRef: RenderingLogger?, _ ->
        DockerStartCommandLine.mapBuild { with(thisRef) { it.execute { null } } }
    }

    /**
     * Builds a [DockerRunCommandLine] and executes it.
     */
    public val run: (Init<Companion.CommandContext> /* = koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext.() -> kotlin.Unit */)
    -> ManagedProcess by DockerRunCommandLine.mapBuild { it.execute { null } }

    /**
     * Builds a [DockerRunCommandLine] and executes it using `this` [RenderingLogger].
     */
    public val RenderingLogger?.run: Builder<Init<Companion.CommandContext>, ManagedProcess> by CallableProperty { thisRef: RenderingLogger?, _ ->
        DockerRunCommandLine.mapBuild { with(thisRef) { it.execute { null } } }
    }

    /**
     * Builds a [DockerStopCommandLine].
     */
    public val stop: (Init<DockerStopCommandLine.Companion.CommandContext> /* = koodies.docker.DockerStopCommandLine.Companion.StopContext.() -> kotlin.Unit */) -> DockerStopCommandLine by DockerStopCommandLine

    /**
     * Builds a [DockerRemoveCommandLine].
     */
    public val remove: (Init<DockerRemoveCommandLine.Companion.CommandContext> /* = koodies.docker.DockerRemoveCommandLine.Companion.RemoveContext.() -> kotlin.Unit */) -> DockerRemoveCommandLine by DockerRemoveCommandLine

    /**
     * Micro DSL to build a [DockerImage] in the style of:
     * - `DockerImage { "bkahlert" / "libguestfs" }`
     * - `DockerImage { "bkahlert" / "libguestfs" tag "latest" }`
     * - `DockerImage { "bkahlert" / "libguestfs" digest "sha256:f466595294e58c1c18efeb2bb56edb5a28a942b5ba82d3c3af70b80a50b4828a" }`
     *
     * Convenience alias for [DockerImage].
     */
    @Suppress("SpellCheckingInspection")
    public fun image(init: ImageContext.() -> DockerImage): DockerImage = DockerImage(init)

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
    public fun stop(name: String): Unit = stop { containers { +name } }.fireAndForget(expectedExitValue = null)

    /**
     * Explicitly (stops and) removes the Docker container with the given [name] **synchronously**.
     *
     * If needed even [forcibly].
     */
    public fun remove(name: String, forcibly: Boolean = false): String = remove {
        options { force using forcibly }
        containers { +name }
    }.execute { expectedExitValue by null; noopProcessor() }
        .apply { onExit.orTimeout(8, TimeUnit.SECONDS).get() }
        .output()
}

/**
 * Runs this command line in a daemon thread asynchronously and silently.
 *
 * Only if something goes wrong an exception is logged on the console.
 */
private fun DockerCommandLine.fireAndForget(
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
) {
    daemon {
        toManagedProcess(expectedExitValue, processTerminationCallback)
            .processSilently().apply { waitForTermination() }
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
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun Path.docker(
    imageInit: ImageContext.() -> DockerImage,
    optionsInit: Init<DockerRunCommandLine.Options.Companion.OptionsContext>,
    vararg arguments: String,
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
): DockerProcess =
    dockerRunCommandLine(imageInit, optionsInit, arguments)
        .toManagedProcess(expectedExitValue, processTerminationCallback)
        .processSilently().apply { waitForTermination() }

/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageInit]
 * - [DockerRunCommandLineOptions] built by the specified [optionsInit]
 * - specified [arguments]
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the [DockerProcess] will be processed by the specified [processor].
 * You can use one of the provided [Processors] or implement one on your own, e.g.
 * - `docker(..., [Processors.consoleLoggingProcessor])` to prints all [IO] to the console (default)
 * - `docker(...) { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun Path.docker(
    imageInit: ImageContext.() -> DockerImage,
    optionsInit: Init<DockerRunCommandLine.Options.Companion.OptionsContext>,
    vararg arguments: String,
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
    processor: Processor<ManagedProcess>?,
): DockerProcess =
    dockerRunCommandLine(imageInit, optionsInit, arguments)
        .toManagedProcess(expectedExitValue, processTerminationCallback)
        .let { it.process({ sync }, processor ?: it.attach()) }

/**
 * Runs a Docker process using the [DockerRunCommandLine] built by the
 * specified [init] checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the [DockerProcess] will be processed by the specified [processor].
 * You can use one of the provided [Processors] or implement one on your own, e.g.
 * - `docker(..., [Processors.consoleLoggingProcessor])` to prints all [IO] to the console
 * - `docker(...) { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun docker(
    init: Init<Companion.CommandContext>,
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
    processor: Processor<DockerProcess>?,
): DockerProcess =
    DockerRunCommandLine(init)
        .toManagedProcess(expectedExitValue, processTerminationCallback)
        .let { it.process({ sync }, processor ?: it.attach()) }


/**
 * Runs a Docker process using the [DockerRunCommandLine] built by the
 * specified [init] checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the [DockerProcess] will be processed by the specified [processor].
 * You can use one of the provided [Processors] or implement one on your own, e.g.
 * - `docker(..., [Processors.consoleLoggingProcessor])` to prints all [IO] to the console (default)
 * - `docker(...) { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun docker(
    processor: Processor<DockerProcess>?,
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
    init: Init<Companion.CommandContext>,
): DockerProcess =
    DockerRunCommandLine(init)
        .toManagedProcess(expectedExitValue, processTerminationCallback)
        .let { it.process({ sync }, processor ?: it.attach()) }
