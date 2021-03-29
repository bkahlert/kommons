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
import koodies.concurrent.process.ProcessTerminationCallback
import koodies.concurrent.process.ProcessingMode
import koodies.concurrent.process.ProcessingMode.Companion.ProcessingModeContext
import koodies.concurrent.process.Processor
import koodies.concurrent.process.attach
import koodies.concurrent.process.process
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions.Companion.BlockLoggingOptionsContext
import koodies.logging.LoggingOptions.CompactLoggingOptions
import koodies.logging.LoggingOptions.CompactLoggingOptions.Companion.CompactLoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions
import koodies.logging.LoggingOptions.SmartLoggingOptions.Companion.SmartLoggingOptionsContext
import koodies.logging.RenderingLogger
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.ANSI.Formatter
import koodies.text.Semantics
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate

public interface Executable {
    public val summary: String
    public fun toProcess(
        expectedExitValue: Int? = 0,
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
    private val logger: RenderingLogger?,
    private val executable: Executable,
) {
    private var processor: Processor<ManagedProcess>? = null

    public fun executeWithOptionalProcessor(init: (OptionsContext.() -> Processor<ManagedProcess>?)?): ManagedProcess =
        executeWithOptionallyStoredProcessor { init?.let { processor = it() } }

    private fun executeWithOptionallyStoredProcessor(init: Init<OptionsContext>): ManagedProcess =
        with(Options(init)) {
            loggingOptions.render(logger, executable.summary) {
                executable.toProcess(expectedExitValue, processTerminationCallback).let {
                    it.process(processingMode, processor = processor ?: it.attach(this))
                }
            }
        }

    /**
     * Options used to [execute] an [Executable].
     */
    public data class Options(
        val expectedExitValue: Int? = 0,
        val processTerminationCallback: ProcessTerminationCallback? = null,
        val loggingOptions: LoggingOptions = SmartLoggingOptions(),
        val processingMode: ProcessingMode = ProcessingMode { sync },
    ) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @ExecutionDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val expectedExitValue: SkippableCapturingBuilderInterface<() -> Int?, Int?> by builder()
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

                public val processing: SkippableCapturingBuilderInterface<ProcessingModeContext.() -> ProcessingMode, ProcessingMode?> by ProcessingMode
            }

            override fun BuildContext.build(): Options = ::OptionsContext{
                Options(
                    ::expectedExitValue.evalOrDefault(0),
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
