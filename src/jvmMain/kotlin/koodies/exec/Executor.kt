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
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.Companion.LoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions
import koodies.logging.MutedRenderingLogger
import koodies.logging.RenderingLogger
import koodies.logging.runLogging

public class Executor(
    public val executable: Executable,
    private var parentLogger: RenderingLogger?,
) {

    private var loggingOptions: LoggingOptions? = parentLogger?.let { SmartLoggingOptions() }
    private var processingMode = ProcessingMode { sync }
    private var processor: Processor<Exec>? = null

    /**
     * Executes the [executable] with the current configuration,
     * and the optional [ExitStateHandler] and [ExecTerminationCallback].
     */
    public operator fun invoke(
        exitStateHandler: ExitStateHandler? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
    ): Exec = exec(exitStateHandler, execTerminationCallback)

    /**
     * Executes the [executable] with the current configuration,
     * and the optional [ExitStateHandler] and [ExecTerminationCallback].
     */
    public fun exec(
        exitStateHandler: ExitStateHandler? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
    ): Exec {

        val processLogger: RenderingLogger = loggingOptions
            ?.newLogger(parentLogger, executable.summary)
            ?: MutedRenderingLogger()

        val exec = executable.toProcess(exitStateHandler, execTerminationCallback)

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
     * and the optional [ExitStateHandler] and [ExecTerminationCallback].
     */
    public fun logging(
        parentLogger: RenderingLogger = this.parentLogger ?: BACKGROUND,
        exitStateHandler: ExitStateHandler? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        loggingOptionsInit: Init<LoggingOptionsContext> = { smart },
    ): Exec {
        this.parentLogger = parentLogger
        loggingOptions = LoggingOptions.build(loggingOptionsInit)
        return exec(exitStateHandler, execTerminationCallback)
    }

    /**
     * Executes the [executable] by processing all [IO] using the given [processor],
     * the [LoggingOptions] built by means of the optional [loggingOptionsInit],
     * and the optional [ExitStateHandler] and [ExecTerminationCallback].
     */
    public fun processing(
        parentLogger: RenderingLogger = this.parentLogger ?: BACKGROUND,
        exitStateHandler: ExitStateHandler? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
        loggingOptionsInit: Init<LoggingOptionsContext> = { smart },
        processor: Processor<Exec>,
    ): Exec {
        this.parentLogger = parentLogger
        loggingOptions = LoggingOptions.build(loggingOptionsInit)
        this.processor = processor
        return exec(exitStateHandler, execTerminationCallback)
    }

    public val async: Executor get() = also { processingMode = ProcessingMode { async } }
    public fun mode(processingModeInit: ProcessingModeContext.() -> ProcessingMode): Executor =
        also { processingMode = ProcessingMode(processingModeInit) }
}

@DslMarker
public annotation class ExecutionDsl

/**
 * An executable is something that can be run using the [Exec]
 * return by [toProcess].
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
     * Creates a [Exec] to run this executable.
     *
     * @param exitStateHandler if specified, the process's exit state is delegated to it
     * @param execTerminationCallback if specified, will be called with the process's final exit state
     */
    public fun toProcess(
        exitStateHandler: ExitStateHandler? = null,
        execTerminationCallback: ExecTerminationCallback? = null,
    ): Exec

    public val exec: Executor get() = Executor(this, null)
    public val <T : RenderingLogger> T?.logging: Executor get() = Executor(this@Executable, this)
}
