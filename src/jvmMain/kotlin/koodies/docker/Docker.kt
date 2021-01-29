package koodies.docker

import koodies.builder.build
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.process
import koodies.concurrent.process.processSilently
import koodies.concurrent.script
import koodies.concurrent.scriptOutputContains
import koodies.time.sleep
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.time.seconds

/**
 * Provides methods to create and interact with a [DockerProcess].
 */
object Docker {
    /**
     * Whether the Docker engine itself is running.
     */
    val isEngineRunning: Boolean get() = !scriptOutputContains("docker info", "error")

    /**
     * Whether a Docker container with the given [name] is running.
     */
    fun isContainerRunning(name: String): Boolean = name.let { sanitizedName ->
        scriptOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$sanitizedName${'$'}"""", sanitizedName)
    }

    /**
     * Whether a Docker container—no matter if it's running or not—exists.
     */
    fun exists(name: String): Boolean = name.let { sanitizedName ->
        scriptOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$sanitizedName${'$'}" --all""", sanitizedName)
    }

    /**
     * Builds a [DockerImage].
     */
    @Deprecated("use dockerImage instead", replaceWith = ReplaceWith("dockerImage"))
    fun image(init: DockerImageBuilder.() -> Any): DockerImage =
        DockerImageBuilder.build(init)

    @Deprecated("use docker instead", replaceWith = ReplaceWith("docker"))
    fun options(init: DockerCommandLineOptionsBuilder.() -> Unit): DockerCommandLineOptions =
        DockerCommandLineOptionsBuilder.build(init)

    @Deprecated("use docker instead", replaceWith = ReplaceWith("docker"))
    fun commandLine(image: DockerImage, options: DockerCommandLineOptions, commandLine: CommandLine) =
        DockerCommandLine(image, options, commandLine)

    @Deprecated("use docker instead", replaceWith = ReplaceWith("docker"))
    fun commandLine(image: DockerImage, commandLine: CommandLine) =
        DockerCommandLine(image = image, options = DockerCommandLineOptions(), commandLine = commandLine)

    /**
     * Explicitly stops the Docker container with the given [name] **asynchronously**.
     */
    fun stop(name: String) {
        script(noopProcessor(), expectedExitValue = null) { !"docker stop \"$name\"" }.onExit.orTimeout(8, TimeUnit.SECONDS)
    }

    /**
     * Explicitly (stops and) removes the Docker container with the given [name] **synchronously**.
     *
     * If needed even [forcibly].
     */
    fun remove(name: String, forcibly: Boolean = false) {
        val forceOption = if (forcibly) " --force" else ""
        script(noopProcessor(), expectedExitValue = null) { !"docker rm$forceOption \"$name\"" }.onExit.orTimeout(8, TimeUnit.SECONDS)
        1.seconds.sleep()
    }
}

/**
 * Creates a [DockerImage] instances using [init].
 */
fun dockerImage(init: DockerImageBuilder.() -> Any): DockerImage =
    DockerImageBuilder.build(init)

/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageBuilder]
 * - [DockerCommandLineOptions] built by the specified [commandLineOptionsBuilder]
 * - specified [arguments]
 * in `this` [Path] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun Path.docker(
    imageBuilder: DockerImageBuilder.() -> Any,
    commandLineOptionsBuilder: DockerCommandLineOptionsBuilder.() -> Unit,
    vararg arguments: String,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): DockerProcess = DockerImageBuilder
    .build(imageBuilder)
    .buildCommandLine {
        options(DockerCommandLineOptionsBuilder.build(commandLineOptionsBuilder))
        commandLine(CommandLine(emptyMap(), this@docker, "", *arguments))
    }
    .toManagedProcess(expectedExitValue, processTerminationCallback)
    .processSilently().apply { waitForTermination() }

/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageBuilder]
 * - [DockerCommandLineOptions] built by the specified [commandLineOptionsBuilder]
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
fun Path.docker(
    imageBuilder: DockerImageBuilder.() -> Any,
    commandLineOptionsBuilder: DockerCommandLineOptionsBuilder.() -> Unit,
    vararg arguments: String,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
): DockerProcess = DockerImageBuilder
    .build(imageBuilder)
    .buildCommandLine {
        val options: DockerCommandLineOptions = commandLineOptionsBuilder.build()
        options(options)
        commandLine(CommandLine(emptyMap(), this@docker, "", *arguments))
    }
    .toManagedProcess(expectedExitValue, processTerminationCallback)
    .process(processor)

/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageBuilder]
 * - [DockerCommandLine] built by the specified [commandLineBuilder]
 * in the directory as specified by [commandLineBuilder]
 * optionally checking the specified [expectedExitValue] (default: `0`).
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
fun docker(
    imageBuilder: DockerImageBuilder.() -> Any,
    commandLineBuilder: DockerCommandLineBuilder.() -> Unit,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<DockerProcess>,
): DockerProcess = DockerImageBuilder
    .build(imageBuilder)
    .buildCommandLine(commandLineBuilder)
    .toManagedProcess(expectedExitValue, processTerminationCallback)
    .process(processor)


/**
 * Runs a Docker process using the
 * - [DockerImage] built by the specified [imageBuilder]
 * - [DockerCommandLine] built by the specified [commandLineBuilder]
 * in the directory as specified by [commandLineBuilder]
 * optionally checking the specified [expectedExitValue] (default: `0`).
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
fun docker(
    imageBuilder: DockerImageBuilder.() -> Any,
    processor: Processor<DockerProcess> = Processors.consoleLoggingProcessor(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    commandLineBuilder: DockerCommandLineBuilder.() -> Unit,
): DockerProcess = DockerImageBuilder
    .build(imageBuilder)
    .buildCommandLine(commandLineBuilder)
    .toManagedProcess(expectedExitValue, processTerminationCallback)
    .process(processor)
