package koodies.exec

import io.opentelemetry.api.trace.Tracer
import koodies.exec.ProcessingMode.Companion.ProcessingModeContext
import koodies.tracing.rendering.RendererProvider
import java.nio.file.Path

/**
 * An executor allows to execute an [executable]
 * in three ways:
 * 1. [invoke] just executes the [executable] with no special handling.
 * 2. [logging] executes the [executable] and logs the execution with the configured [tracingOptions].
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

    private val tracingOptions: TracingOptions = TracingOptions(executable.summary, RendererProviders.NOOP),

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
        .process(tracingOptions, processingMode, processor ?: Processors.noopProcessor())

    public fun logging(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        name: String? = tracingOptions.name,
        renderer: RendererProvider = { it(this) },
    ): E = copy(tracingOptions = tracingOptions.copy(name = name, renderer = renderer)).invoke(workingDirectory, execTerminationCallback)

    /**
     * Executes the [executable] by processing all [IO] using the given [processor].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public fun processing(
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        name: String? = tracingOptions.name,
        renderer: RendererProvider? = { it(this) },
        processor: Processor<E>,
    ): E = copy(
        tracingOptions = tracingOptions.copy(name = name, renderer = renderer ?: tracingOptions.renderer),
        processor = processor,
    ).invoke(workingDirectory, execTerminationCallback)

    /**
     * Set the [mode] to [ProcessingMode.Synchronicity.Async].
     */
    public val async: Executor<E> get() = copy(processingMode = ProcessingMode { async })

    internal fun tracer(tracer: Tracer): Executor<E> =
        copy(tracingOptions = tracingOptions.copy(tracer = tracer))

    /**
     * Configures if the execution will be synchronous
     * or asynchronous including mode-specific options, such as if the
     * [Exec]'s [IO] is to be processed non-blocking
     * (default: synchronous execution).
     */
    public fun mode(processingModeInit: ProcessingModeContext.() -> ProcessingMode): Executor<E> =
        copy(processingMode = ProcessingMode(processingModeInit))
}
