package koodies.exec

import koodies.builder.build
import koodies.exec.ProcessingMode.Companion.ProcessingModeContext
import koodies.exec.ProcessingMode.Synchronicity.Async
import koodies.exec.ProcessingMode.Synchronicity.Sync
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.Companion.LoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.ReturnValue
import koodies.logging.runLogging
import java.nio.file.Path

/**
 * An executor allows to execute an [executable]
 * in three ways:
 * 1. [invoke] just executes the [executable] with no special handling.
 * 2. [logging] executes the [executable] and logs the execution with the configured [logger].
 * 3. [processing] executes the [executable] by passing the [Exec]'s [IO] to the configured [processor].
 */
public data class Executor<E : Exec>(

    /**
     * Instance of what will be executed as soon as [invoke]
     * is called.
     */
    protected val executable: Executable<E>,

    /**
     * Whether standard error is redirected to standard output during execution.
     */
    private val redirectErrorStream: Boolean = false,

    /**
     * The environment to be exposed to the [Exec] during execution.
     */
    private val environment: Map<String, String> = emptyMap(),

    /**
     * Logger used to log the execution of [executable]
     * (default: `null`; no logging).
     */
    private val logger: RenderingLogger? = null,

    /**
     * Caption used if the execution is [logging]
     * (default: [Executable.summary]; only applies if a [logger] is set).
     */
    private val caption: String = executable.summary,

    /**
     * Options applied if the execution is [logging]
     * (default: [SmartLoggingOptions]; only applies if a [logger] is set).
     */
    private val loggingOptions: LoggingOptions? = logger?.let { SmartLoggingOptions() },

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
    ): E {

        val processLogger: RenderingLogger = loggingOptions?.newLogger(logger, caption) ?: MutedRenderingLogger

        val exec: E = executable.toExec(redirectErrorStream, environment, workingDirectory, execTerminationCallback)

        when (processingMode.synchronicity) {
            Sync -> processLogger.runLogging {
                exec.process(processingMode, processor = processor ?: Processors.loggingProcessor(processLogger))
            }
            Async -> {
                processLogger.logResult {
                    Result.success<ReturnValue>(object : ReturnValue {
                        override val successful: Boolean? = null
                        override val symbol: String = " "
                        override val textRepresentation: String? = null
                        override fun format(): String = " "
                    })
                }
                exec.process(processingMode, processor = processor ?: exec.terminationLoggingProcessor(processLogger))
            }
        }

        return exec
    }

    /**
     * Executes the [executable] by logging all [IO] using the given [logger],
     * the [LoggingOptions] built by means of the optional [loggingOptionsInit].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public fun logging(
        logger: RenderingLogger = this.logger ?: BACKGROUND,
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        loggingOptionsInit: LoggingOptionsContext.(Executable<E>) -> Unit = { smart },
    ): E = copy(
        logger = logger,
        loggingOptions = LoggingOptions.build { loggingOptionsInit(this, executable) },
    ).invoke(workingDirectory, execTerminationCallback)

    /**
     * Executes the [executable] by processing all [IO] using the given [processor],
     * and the [LoggingOptions] built by means of the optional [loggingOptionsInit].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param execTerminationCallback called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    public fun processing(
        logger: RenderingLogger? = this.logger ?: BACKGROUND,
        workingDirectory: Path? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        loggingOptionsInit: LoggingOptionsContext.(Executable<E>) -> Unit = { smart },
        processor: Processor<E>,
    ): E = copy(
        logger = logger,
        loggingOptions = LoggingOptions.build { loggingOptionsInit(this, executable) },
        processor = processor
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

    /**
     * Executor that allows to execute this [Executable] [Executor.logging]
     * using `this` [RenderingLogger].
     *
     * @see exec
     */
    public val <T : RenderingLogger> T?.logging: Executor<out E> get() = Executor(this@Executable, logger = this)
}
