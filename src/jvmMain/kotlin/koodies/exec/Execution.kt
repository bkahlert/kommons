package koodies.exec

import koodies.builder.BuilderTemplate
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.concurrent.process.IO
import koodies.concurrent.process.ProcessingMode
import koodies.concurrent.process.ProcessingMode.Companion.ProcessingModeContext
import koodies.concurrent.process.Processor
import koodies.exec.Execution.Options
import koodies.exec.Execution.Options.Companion.OptionsContext
import koodies.logging.LoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions
import koodies.logging.LoggingOptions.BlockLoggingOptions.Companion.BlockLoggingOptionsContext
import koodies.logging.LoggingOptions.CompactLoggingOptions
import koodies.logging.LoggingOptions.CompactLoggingOptions.Companion.CompactLoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions
import koodies.logging.LoggingOptions.SmartLoggingOptions.Companion.SmartLoggingOptionsContext
import koodies.logging.RenderingLogger
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate


/**
 * Helper to collect an optional [RenderingLogger], build [Options] and an optional [Processor]
 * to [exec] the given [CommandLine].
 */
@Deprecated("delete")
public class Execution(
) {

    /**
     * Options used to [execute] an [Executable].
     */
    public data class Options(
        val execTerminationCallback: ExecTerminationCallback? = null,
        val loggingOptions: LoggingOptions = SmartLoggingOptions(),
        val processingMode: ProcessingMode = ProcessingMode { sync },
    ) {
        public companion object : BuilderTemplate<OptionsContext, Options>() {
            @ExecutionDsl
            public class OptionsContext(override val captures: CapturesMap) : CapturingContext() {
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

                public val processing: SkippableCapturingBuilderInterface<ProcessingModeContext.() -> ProcessingMode, ProcessingMode?> by ProcessingMode
            }

            override fun BuildContext.build(): Options = ::OptionsContext{
                Options(
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
