package koodies.exec

import koodies.builder.Init
import koodies.builder.build
import koodies.concurrent.process.IO
import koodies.concurrent.process.ProcessingMode
import koodies.concurrent.process.ProcessingMode.Companion.ProcessingModeContext
import koodies.concurrent.process.ProcessingMode.Synchronicity.Async
import koodies.concurrent.process.ProcessingMode.Synchronicity.Sync
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.process
import koodies.concurrent.process.terminationLoggingProcessor
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.Companion.LoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.runLogging

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
    public val executable: Executable,

    /**
     * Factory responsible to provide an [Exec] of type [E]
     * that executes the [executable].
     */
    public val execFactory: ExecFactory<E>,

    /**
     * Logger used to log the execution of [executable]
     * (default: `null`; no logging).
     */
    public val logger: RenderingLogger? = null,

    /**
     * Caption used if the execution is [logging]
     * (default: [Executable.summary]; only applies if a [logger] is set).
     */
    public val caption: String = executable.summary,

    /**
     * Options applied if the execution is [logging]
     * (default: [SmartLoggingOptions]; only applies if a [logger] is set).
     */
    public val loggingOptions: LoggingOptions? = logger?.let { SmartLoggingOptions() },

    /**
     * Mode that defines if the execution will be synchronous
     * or asynchronous including mode-specific options, such as if the
     * [Exec]'s [IO] is to be processed non-blocking
     * (default: synchronous execution).
     */
    public val processingMode: ProcessingMode = ProcessingMode { sync },

    /**
     * Processor used to interactively handle the [Exec]'s [IO]
     * (default: no specific handling; the [IO] is only processed to be made available
     * by [Exec.io]).
     */
    public val processor: Processor<Exec>? = null,
) {
    @Suppress("UNCHECKED_CAST")
    public fun <T : E> with(execFactory: ExecFactory<T>): Executor<T> =
        copy(execFactory = execFactory) as Executor<T>


    // TODO setter for environemnt
    // TODO setter for WorkingDirectory

    /**
     * Executes the [executable] with the current configuration,
     * and the optional [ExecTerminationCallback].
     *
     * If set, [execTerminationCallback] will be called the moment the
     * [Exec] terminates—independent of whether [Exec] succeeds or fails.
     */
    public operator fun invoke(execTerminationCallback: ExecTerminationCallback? = null): E {

        val processLogger: RenderingLogger = loggingOptions
            ?.newLogger(logger, caption)
            ?: MutedRenderingLogger()

        val commandLine: CommandLine = executable.toCommandLine()
        val exec: E = execFactory.toProcess(commandLine, execTerminationCallback)

        when (processingMode.synchronicity) {
            Sync -> processLogger.runLogging {
                exec.process(processingMode, processor = processor ?: Processors.loggingProcessor(processLogger))
            }
            Async -> {
                processLogger.logResult { Result.success(exec) }
                exec.process(processingMode, processor = processor ?: exec.terminationLoggingProcessor(processLogger))
            }
        }

        return exec
    }

    /**
     * Executes the [executable] by logging all [IO] using the given [parentLogger],
     * the [LoggingOptions] built by means of the optional [loggingOptionsInit],
     * and the optional [ExecTerminationCallback].
     */
    public fun logging(
        parentLogger: RenderingLogger = this.logger ?: BACKGROUND,
        execTerminationCallback: ExecTerminationCallback? = null,
        loggingOptionsInit: Init<LoggingOptionsContext> = { smart },
    ): E = copy(
        logger = parentLogger,
        loggingOptions = LoggingOptions.build(loggingOptionsInit),
    ).invoke(execTerminationCallback)

    /**
     * Executes the [executable] by processing all [IO] using the given [processor],
     * the [LoggingOptions] built by means of the optional [loggingOptionsInit],
     * and the optional [ExecTerminationCallback].
     */
    public fun processing(
        parentLogger: RenderingLogger = this.logger ?: BACKGROUND,
        execTerminationCallback: ExecTerminationCallback? = null,
        loggingOptionsInit: Init<LoggingOptionsContext> = { smart },
        processor: Processor<Exec>,
    ): E = copy(
        logger = parentLogger,
        loggingOptions = LoggingOptions.build(loggingOptionsInit),
        processor = processor
    ).invoke(execTerminationCallback)

    public val async: Executor<E> get() = copy(processingMode = ProcessingMode { async })
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
public interface Executable {

    /**
     * Brief description of that this executable is doing.
     */
    public val summary: String

    /**
     * Creates a [CommandLine] that is able to run [Executable].
     */
    public fun toCommandLine(): CommandLine

    /**
     * Executor that allows to execute this [Executable]
     * in three ways:
     * 1. [Executor.invoke] just executes this [Executable] with no special handling.
     * 2. [Executor.logging] executes the [Executable] and logs the execution with the configured [Executor.logger].
     * 3. [Executor.processing] executes the [Executable] by passing the [Exec]'s [IO] to the configured [Executor.processor].
     */
    public val exec: Executor<Exec> get() = Executor(this, ExecFactory.NATIVE, null)

    /**
     * Executor that allows to execute this [Executable] [Executor.logging]
     * using `this` [RenderingLogger].
     *
     * @see exec
     */
    public val <T : RenderingLogger> T?.logging: Executor<Exec> get() = Executor(this@Executable, ExecFactory.NATIVE, this)
}
