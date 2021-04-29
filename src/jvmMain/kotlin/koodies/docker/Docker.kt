package koodies.docker

import koodies.CallableProperty
import koodies.builder.Builder
import koodies.builder.Init
import koodies.builder.mapBuild
import koodies.concurrent.process.IO
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.process
import koodies.concurrent.process.processSilently
import koodies.concurrent.process.terminationLoggingProcessor
import koodies.docker.DockerExitStateHandler.Failure
import koodies.docker.DockerImage.ImageContext
import koodies.docker.DockerRunCommandLine.Companion
import koodies.exec.CommandLine
import koodies.exec.CommandLine.Companion.CommandLineContext
import koodies.exec.Exec
import koodies.exec.ExecTerminationCallback
import koodies.exec.parse
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLogger
import koodies.map
import koodies.or
import koodies.provideDelegate
import koodies.regex.RegularExpressions
import koodies.text.Semantics
import koodies.text.Semantics.formattedAs
import koodies.text.joinToKebabCase
import java.nio.file.Path

/**
 * Entrypoint to ease discovery of Docker related features.
 */
public object Docker {

    /**
     * System wide information regarding the Docker installation.
     */
    public object info {

        /**
         * Returns the information identified by the given [keys].
         *
         * The [keys] can either be an array of keys,
         * or a single string consisting of `.` separated keys.
         *
         * Each key is expected to be in `kebab-case`.
         *
         * Examples:
         * - `info["server.server-version"]`
         * - `info["server", "server-version"]`
         */
        public operator fun get(vararg keys: String): String? = BACKGROUND.get(*keys)

        /**
         * Returns the information identified by the given [keys].
         *
         * The [keys] can either be an array of keys,
         * or a single string consisting of `.` separated keys.
         *
         * Each key is expected to be in `kebab-case`.
         *
         * Examples:
         * - `info["server.server-version"]`
         * - `info["server", "server-version"]`
         */
        public operator fun RenderingLogger.get(vararg keys: String): String? =
            with(keys.flatMap { it.split(".") }.map { it.unify() }.toMutableList()) {
                DockerInfoCommandLine {}.exec.logging(this@get) {
                    noDetails("Querying info ${joinToString(Semantics.FieldDelimiters.UNIT) { it.formattedAs.input }}")
                }.parse.columns<String, Failure>(1) { (line) ->
                    if (isNotEmpty() && line.substringBefore(":").unify() == first()) {
                        removeAt(0)
                        if (isEmpty()) line.substringAfter(":").trim()
                        else null
                    } else {
                        null
                    }
                }.map { singleOrNull() } or { null }
            }

        private fun String.unify() = toLowerCase().split(RegularExpressions.SPACES).filterNot { it.isEmpty() }.joinToKebabCase()
    }

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
    public val engineRunning: Boolean get() = DockerInfoCommandLine {}.exec().successful == true

    /**
     * Returns a [DockerContainer] representing a Docker container of the same
     * (sanitized) name.
     */
    public operator fun invoke(name: String): DockerContainer = DockerContainer.from(name, randomSuffix = false)


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
     * Builds a [DockerRunCommandLine] and executes it.
     */
    public val run: (Init<Companion.CommandContext> /* = koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext.() -> kotlin.Unit */)
    -> Exec by DockerRunCommandLine.mapBuild { it.exec.logging() }

    /**
     * Builds a [DockerRunCommandLine] and executes it using `this` [RenderingLogger].
     */
    public val RenderingLogger?.run: Builder<Init<Companion.CommandContext>, Exec> by CallableProperty { thisRef: RenderingLogger?, _ ->
        DockerRunCommandLine.mapBuild { if (thisRef != null) it.exec.logging(thisRef) else it.exec.logging() }
    }

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
): DockerExec =
    DockerExec.NATIVE_DOCKER_EXEC_WRAPPED.toProcess(dockerRunCommandLine(imageInit, optionsInit, arguments),
        execTerminationCallback)
        .processSilently().apply { waitFor() }

/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageInit]
 * - [DockerRunCommandLineOptions] built by the specified [optionsInit]
 * - specified [arguments]
 * in `this` [Path].
 *
 * The output of the [DockerExec] will be processed by the specified [processor].
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
): DockerExec =
    DockerExec.NATIVE_DOCKER_EXEC_WRAPPED.toProcess(dockerRunCommandLine(imageInit, optionsInit, arguments),
        execTerminationCallback)
        .let { it.process({ sync }, processor ?: it.terminationLoggingProcessor()) }

/**
 * Runs a Docker process using the [DockerRunCommandLine] built by the
 * specified [init].
 *
 * The output of the [DockerExec] will be processed by the specified [processor].
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
    processor: Processor<DockerExec>?,
): DockerExec =
    DockerExec.NATIVE_DOCKER_EXEC_WRAPPED.toProcess(DockerRunCommandLine(init),
        execTerminationCallback)
        .let { it.process({ sync }, processor ?: it.terminationLoggingProcessor()) }


/**
 * Runs a Docker process using the [DockerRunCommandLine] built by the
 * specified [init].
 *
 * The output of the [DockerExec] will be processed by the specified [processor].
 * You can use one of the provided [Processors] or implement one on your own, e.g.
 * - `docker(..., [Processors.loggingProcessor])` to prints all [IO] to the console (default)
 * - `docker(...) { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [execTerminationCallback] will be called on process
 * termination and before other [Exec.onExit] registered listeners
 * get called.
 */
public fun docker(
    processor: Processor<DockerExec>?,
    execTerminationCallback: ExecTerminationCallback? = null,
    init: Init<Companion.CommandContext>,
): DockerExec =
    DockerExec.NATIVE_DOCKER_EXEC_WRAPPED.toProcess(DockerRunCommandLine(init),
        execTerminationCallback)
        .let { it.process({ sync }, processor ?: it.terminationLoggingProcessor()) }
