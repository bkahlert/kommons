package koodies.exec

import koodies.exec.ProcessingMode.Companion.ProcessingModeContext
import koodies.logging.LoggingOptionsOld
import koodies.tracing.rendering.RendererProvider
import java.nio.file.Path

/**
 * An executor allows to execute an [executable]
 * in three ways:
 * 1. [invoke] just executes the [executable] with no special handling.
 * 2. [logging] executes the [executable] and logs the execution with the configured [loggingOptions].
 * 3. [processing] executes the [executable] by passing the [Exec]'s [IO] to the configured [processor].
 */
public data class Executor<E : Exec>(

    /**
     * Instance of what will be executed as soon as [invoke]
     * is called.
     */
    private val executable: Executable<E>,

    /**
     * Whether standard error is redirected to standard output during execution.
     */
    private val redirectErrorStream: Boolean = false,

    /**
     * The environment to be exposed to the [Exec] during execution.
     */
    private val environment: Map<String, String> = emptyMap(),

    // TODO do not use ScriptLinks.kt for jaeger tracing (only for output/rendering)
    private val loggingOptions: LoggingOptions = LoggingOptions(executable.summary, RendererProviders.NOOP),

    /**
     * Mode that defines if the execution will be synchronous
     * or asynchronous including mode-specific options, such as if the
     * [Exec]'s [IO] is to be processed non-blocking
     * (default: synchronous execution).
     */
    private val processingMode: ProcessingMode = ProcessingMode { sync },

    /**
     * Processor used to interactively handle the [Exec]'s [IO]
     * (default: no specific handling; the [IO] is only processed to be made available
     * by [Exec.io]).
     */
    private val processor: Processor<E>? = null,
) {

    public fun env(key: String, value: String): Executor<E> =
        copy(environment = environment.plus(key to value))

    /**
     * Executes the [executable] with the current configuration.
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public operator fun invoke(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
    ): E = executable
        .toExec(redirectErrorStream, environment, workingDirectory, execTerminationCallback)
        .process(loggingOptions, processingMode, processor ?: Processors.noopProcessor())

    public fun logging(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        name: String? = loggingOptions.name,
        renderer: RendererProvider = { it(this) },
    ): E = copy(loggingOptions = LoggingOptions(name, renderer)).invoke(workingDirectory, execTerminationCallback)

    /**
     * Executes the [executable] by processing all [IO] using the given [processor],
     * and the [LoggingOptionsOld] built by means of the optional [rendererProvider].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public fun processing(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        name: String? = loggingOptions.name,
        renderer: RendererProvider? = { it(this) },
        processor: Processor<E>,
    ): E = copy(
        loggingOptions = LoggingOptions(name, renderer ?: loggingOptions.renderer),
        processor = processor,
    ).invoke(workingDirectory, execTerminationCallback)

    /**
     * Set the [mode] to [ProcessingMode.Synchronicity.Async].
     */
    public val async: Executor<E> get() = copy(processingMode = ProcessingMode { async })

    /**
     * Configures if the execution will be synchronous
     * or asynchronous including mode-specific options, such as if the
     * [Exec]'s [IO] is to be processed non-blocking
     * (default: synchronous execution).
     */
    public fun mode(processingModeInit: ProcessingModeContext.() -> ProcessingMode): Executor<E> =
        copy(processingMode = ProcessingMode(processingModeInit))
}

@DslMarker
public annotation class ExecutionDsl

/**
 * An executable is something that can be executed
 * using [exec] or any of the various options
 * provided by [Exec].
 */
@ExecutionDsl
public interface Executable<out E : Exec> {

    /**
     * Brief description of that this executable is doing.
     */
    public val summary: String

    /**
     * Creates a [CommandLine] to run this executable.
     *
     * @param environment the environment to be exposed to the [Exec] during execution
     * @param workingDirectory the working directory to be used during execution
     * @param transform applied to each argument before used to form the [CommandLine]
     */
    public fun toCommandLine(
        environment: Map<String, String> = emptyMap(),
        workingDirectory: Path? = null,
        transform: (String) -> String = { it },
    ): CommandLine

    /**
     * Creates a [Exec] to run this executable.
     *
     * @param redirectErrorStream whether standard error is redirected to standard output during execution
     * @param environment the environment to be exposed to the [Exec] during execution
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public fun toExec(
        redirectErrorStream: Boolean,
        environment: Map<String, String>,
        workingDirectory: Path?,
        execTerminationCallback: ExecTerminationCallback?,
    ): E

    /**
     * Executor that allows to execute this [Executable]
     * in three ways:
     * 1. [Executor.invoke] just executes this [Executable] with no special handling.
     * 2. [Executor.logging] executes the [Executable] and logs the execution with the configured [Executor.logger].
     * 3. [Executor.processing] executes the [Executable] by passing the [Exec]'s [IO] to the configured [Executor.processor].
     */
    public val exec: Executor<out E> get() = Executor(this)
}
