package koodies.concurrent

import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.Execution.Options
import koodies.concurrent.Execution.Options.Companion.OptionsContext
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Process.ExitState.ExitStateHandler
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.concurrent.process.ProcessingMode
import koodies.concurrent.process.ProcessingMode.Companion.ProcessingModeContext
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors.loggingProcessor
import koodies.concurrent.process.process
import koodies.concurrent.process.terminationLoggingProcessor
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions.Companion.BlockLoggingOptionsContext
import koodies.logging.LoggingOptions.CompactLoggingOptions
import koodies.logging.LoggingOptions.CompactLoggingOptions.Companion.CompactLoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions
import koodies.logging.LoggingOptions.SmartLoggingOptions.Companion.SmartLoggingOptionsContext
import koodies.logging.RenderingLogger
import koodies.logging.runLogging
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.ANSI.Formatter
import koodies.text.Semantics
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate

/**
 * An executable is something that can be run using the [ManagedProcess]
 * return by [toProcess].
 */
public interface Executable {
    /**
     * Brief description of that this executable is doing.
     */
    public val summary: String

    /**
     * Creates a [ManagedProcess] to run this executable.
     *
     * @param exitStateHandler if specified, the process's exit state is delegated to it
     * @param processTerminationCallback if specified, will be called with the process's final exit state
     */
    public fun toProcess(
        exitStateHandler: ExitStateHandler? = null,
        processTerminationCallback: ProcessTerminationCallback? = null,
    ): ManagedProcess
}

@DslMarker
public annotation class ExecutionDsl

/**
 * Helper to collect an optional [RenderingLogger], build [Options] and an optional [Processor]
 * to [execute] the given [CommandLine].
 */
public class Execution(
    private val parentLogger: RenderingLogger?,
    private val executable: Executable,
) {
    private var processor: Processor<ManagedProcess>? = null

    public fun executeWithOptionalProcessor(init: (OptionsContext.() -> Processor<ManagedProcess>?)?): ManagedProcess =
        executeWithOptionallyStoredProcessor { init?.let { processor = it() } }

    private fun executeWithOptionallyStoredProcessor(init: Init<OptionsContext>): ManagedProcess =
        with(Options(init)) {
            val processLogger = loggingOptions.newLogger(parentLogger, executable.summary)
            val managedProcess = executable.toProcess(exitStateHandler, processTerminationCallback)
            if (processingMode.isSync) {
                processLogger.runLogging {
                    managedProcess.process(processingMode, processor = processor ?: loggingProcessor(processLogger))
                }
            } else {
                processLogger.logResult { Result.success(managedProcess) }
                managedProcess.process(processingMode, processor = processor ?: managedProcess.terminationLoggingProcessor(processLogger))
            }
        }

    /**
     * Options used to [execute] an [Executable].
     */
    public data class Options(
        val exitStateHandler: ExitStateHandler? = null,
        val processTerminationCallback: ProcessTerminationCallback? = null,
        val loggingOptions: LoggingOptions = SmartLoggingOptions(),
        val processingMode: ProcessingMode = ProcessingMode { sync },
    ) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @ExecutionDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val exitStateHandler: SkippableCapturingBuilderInterface<() -> ExitStateHandler?, ExitStateHandler?> by builder<ExitStateHandler?>()
                public val processTerminationCallback: SkippableCapturingBuilderInterface<() -> ProcessTerminationCallback, ProcessTerminationCallback?> by builder()

                public val block: SkippableCapturingBuilderInterface<BlockLoggingOptionsContext.() -> Unit, BlockLoggingOptions?> by BlockLoggingOptions
                public val compact: SkippableCapturingBuilderInterface<CompactLoggingOptionsContext.() -> Unit, CompactLoggingOptions?> by CompactLoggingOptions
                public val smart: SkippableCapturingBuilderInterface<SmartLoggingOptionsContext.() -> Unit, SmartLoggingOptions?> by SmartLoggingOptions

                /**
                 * Formats the output in a compact fashion with each message generically shortened using the following rules:
                 * - remove meta messages
                 * - messages containing a colon (e.g. `first: second`) are reduced to the part after the colon (e.g. `second`)
                 * - if the reduced message is still longer than the given [maxMessageLength], the message is truncated
                 *
                 * Example output: `Pulling busybox ➜ latest ➜ library/busybox ➜ sha256:ce2…af390a2ac ➜ busybox:latest ➜ latest ✔`
                 */
                public fun summary(caption: String, maxMessageLength: Int = 20) {
                    compact {
                        this.caption by caption
                        contentFormatter {
                            Formatter {
                                it.takeUnless { it is IO.META }?.removeEscapeSequences()?.run {
                                    val step = substringAfter(":").trim().run {
                                        takeIf { length < maxMessageLength } ?: split(Regex("\\s+")).last().truncate(maxMessageLength, strategy = MIDDLE)
                                    }
                                    Semantics.PointNext + " $step"
                                } ?: ""
                            }
                        }
                    }
                }

                /**
                 * Formats the output by hiding all details, that is, only the caption and an eventual occurring exception is displayed.
                 *
                 * Example output: `Listing images ✔`
                 */
                public fun noDetails(caption: String) {
                    compact {
                        this.caption by caption
                        contentFormatter by { "" }
                    }
                }

                /**
                 * Filters all IO but errors.
                 *
                 * Example output: `ϟ Process 64207 terminated with exit code 255.`
                 */
                public fun errorsOnly(caption: String) {
                    block {
                        this.caption { "" }
                        border = NONE
                        contentFormatter by Formatter {
                            (it as? IO.ERR)?.let { err -> "$caption: $err" } ?: ""
                        }
                        decorationFormatter by Formatter { "" }
                        returnValueFormatter { { if (it.successful == false) "${Semantics.Error} ${it.format()}" else "" } }
                    }
                }

                public val processing: SkippableCapturingBuilderInterface<ProcessingModeContext.() -> ProcessingMode, ProcessingMode?> by ProcessingMode

                /**
                 * Can be used to return a [Processor] to process the [IO].
                 *
                 * Alternatively you would have to write: `; { io -> … }`, that is put a semicolon in front of your lambda.
                 */
                public fun process(processor: Processor<ManagedProcess>): Processor<ManagedProcess> = processor

                /**
                 * Can be used to return a [Processor] to process only the [IO]
                 * passing the [predicate].
                 */
                public fun processOnly(predicate: ManagedProcess.(IO) -> Boolean, processor: Processor<ManagedProcess>): Processor<ManagedProcess> =
                    { io -> if (predicate(io)) processor(io) }

                /**
                 * Can be used to return a [Processor] to process only [IO]
                 * of the given type [T].
                 */
                public inline fun <reified T : IO> processOnly(crossinline processor: Processor<ManagedProcess>): Processor<ManagedProcess> =
                    { io -> if (io is T) processor(io) }
            }

            override fun BuildContext.build(): Options = ::OptionsContext{
                Options(
                    ::exitStateHandler.evalOrNull(),
                    ::processTerminationCallback.evalOrNull(),
                    ::block.evalOrNull<BlockLoggingOptions>()
                        ?: ::compact.evalOrNull<CompactLoggingOptions>()
                        ?: ::smart.evalOrNull<SmartLoggingOptions>()
                        ?: SmartLoggingOptions(),
                    ::processing.evalOrNull() ?: ProcessingMode { sync }
                )
            }
        }
    }
}

/**
 * Runs `this` [Executable] using `this` optional [RenderingLogger]
 * and built [Options].
 *
 * If a [Processor] is returned at the end of the [Options] build,
 * it will be used to process the process's [IO]. Otherwise the [IO] will be logged
 * either to the console or if present, `this` [RenderingLogger].
 */
@ExecutionDsl
public val Executable.execute: ((OptionsContext.() -> Processor<ManagedProcess>?)?) -> ManagedProcess
    get() = { Execution(null, this@execute).executeWithOptionalProcessor(it) }

/**
 * Runs `this` [Executable] using `this` optional [RenderingLogger]
 * and built [Options].
 *
 * If a [Processor] is returned at the end of the [Options] build,
 * it will be used to process the process's [IO]. Otherwise the [IO] will be logged
 * either to the console or if present, `this` [RenderingLogger].
 */
@ExecutionDsl
public val RenderingLogger?.execute: Executable.((OptionsContext.() -> Processor<ManagedProcess>?)?) -> ManagedProcess
    get() = { Execution(this@execute, this).executeWithOptionalProcessor(it) }
