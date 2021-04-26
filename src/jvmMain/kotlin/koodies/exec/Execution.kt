package koodies.exec

import koodies.builder.BuilderTemplate
import koodies.builder.Init
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.process.IO
import koodies.concurrent.process.ProcessingMode
import koodies.concurrent.process.ProcessingMode.Companion.ProcessingModeContext
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors.loggingProcessor
import koodies.concurrent.process.process
import koodies.concurrent.process.terminationLoggingProcessor
import koodies.exec.Execution.Options
import koodies.exec.Execution.Options.Companion.OptionsContext
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions.Companion.BlockLoggingOptionsContext
import koodies.logging.LoggingOptions.CompactLoggingOptions
import koodies.logging.LoggingOptions.CompactLoggingOptions.Companion.CompactLoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions
import koodies.logging.LoggingOptions.SmartLoggingOptions.Companion.SmartLoggingOptionsContext
import koodies.logging.RenderingLogger
import koodies.logging.ReturnValue
import koodies.logging.runLogging
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate


/**
 * Helper to collect an optional [RenderingLogger], build [Options] and an optional [Processor]
 * to [execute] the given [CommandLine].
 */
public class Execution(
    private val parentLogger: RenderingLogger?,
    private val executable: Executable,
) {
    private var processor: Processor<Exec>? = null

    public fun executeWithOptionalProcessor(init: (OptionsContext.() -> Processor<Exec>?)?): Exec =
        executeWithOptionallyStoredProcessor { init?.let { processor = it() } }

    private fun executeWithOptionallyStoredProcessor(init: Init<OptionsContext>): Exec =
        with(Options(init)) {
            val processLogger: RenderingLogger = loggingOptions.newLogger(parentLogger, executable.summary)
            val exec = executable.toProcess(exitStateHandler, execTerminationCallback)
            if (processingMode.isSync) {
                processLogger.runLogging {
                    exec.process(processingMode, processor = processor ?: loggingProcessor(processLogger))
                }
            } else {
                processLogger.logResult { Result.success(exec) }
                exec.process(processingMode, processor = processor ?: exec.terminationLoggingProcessor(processLogger))
            }
        }

    /**
     * Options used to [execute] an [Executable].
     */
    public data class Options(
        val exitStateHandler: ExitStateHandler? = null,
        val execTerminationCallback: ExecTerminationCallback? = null,
        val loggingOptions: LoggingOptions = SmartLoggingOptions(),
        val processingMode: ProcessingMode = ProcessingMode { sync },
    ) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @ExecutionDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val exitStateHandler: SkippableCapturingBuilderInterface<() -> ExitStateHandler?, ExitStateHandler?> by builder()
                public val execTerminationCallback: SkippableCapturingBuilderInterface<() -> ExecTerminationCallback, ExecTerminationCallback?> by builder()

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
                                it.takeUnless { it is IO.META }?.ansiRemoved?.run {
                                    val step = substringAfter(":").trim().run {
                                        takeIf { length < maxMessageLength } ?: split(Regex("\\s+")).last().truncate(maxMessageLength, strategy = MIDDLE)
                                    }
                                    Symbols.PointNext + " $step"
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
                    val none = object : ReturnValue {
                        override val successful: Boolean = true
                        override val symbol: String = ""
                        override val textRepresentation: String? = null
                    }
                    block {
                        this.caption { "" }
                        border = NONE
                        contentFormatter by Formatter {
                            (it as? IO.ERR)?.let { err -> "$caption: $err" } ?: ""
                        }
                        decorationFormatter by Formatter { "" }
                        returnValueFormatter { { if (it.successful == false) it else none } }
                    }
                }

                public val processing: SkippableCapturingBuilderInterface<ProcessingModeContext.() -> ProcessingMode, ProcessingMode?> by ProcessingMode

                /**
                 * Can be used to return a [Processor] to process the [IO].
                 *
                 * Alternatively you would have to write: `; { io -> … }`, that is put a semicolon in front of your lambda.
                 */
                public fun process(processor: Processor<Exec>): Processor<Exec> = processor

                /**
                 * Can be used to return a [Processor] to process only the [IO]
                 * passing the [predicate].
                 */
                public fun processOnly(predicate: Exec.(IO) -> Boolean, processor: Processor<Exec>): Processor<Exec> =
                    { io -> if (predicate(io)) processor(io) }

                /**
                 * Can be used to return a [Processor] to process only [IO]
                 * of the given type [T].
                 */
                public inline fun <reified T : IO> processOnly(crossinline processor: Processor<Exec>): Processor<Exec> =
                    { io -> if (io is T) processor(io) }
            }

            override fun BuildContext.build(): Options = ::OptionsContext{
                Options(
                    ::exitStateHandler.evalOrNull(),
                    ::execTerminationCallback.evalOrNull(),
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
public val Executable.execute: ((OptionsContext.() -> Processor<Exec>?)?) -> Exec
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
public val RenderingLogger?.execute: Executable.((OptionsContext.() -> Processor<Exec>?)?) -> Exec
    get() = { Execution(this@execute, this).executeWithOptionalProcessor(it) }
