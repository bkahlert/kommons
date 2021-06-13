package koodies.logging

import koodies.builder.BuilderTemplate
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.exec.IO
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.LoggingOptions.BlockLoggingOptions.Companion.BlockLoggingOptionsContext
import koodies.logging.LoggingOptions.CompactLoggingOptions.Companion.CompactLoggingOptionsContext
import koodies.logging.LoggingOptions.Companion.LoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions.Companion.SmartLoggingOptionsContext
import koodies.text.ANSI.Colors
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import koodies.text.truncate

/**
 * Options that define how a [SimpleRenderingLogger] renders log messages.
 */
public sealed class LoggingOptions {

    public abstract fun newLogger(parent: SimpleRenderingLogger?, fallbackName: String): SimpleRenderingLogger
    public fun logTextCallOrNull(logger: SimpleRenderingLogger?): ((String) -> Unit)? = logger?.run { { logText { it } } }

    /**
     * Renders log messages line-by-line.
     */
    public class BlockLoggingOptions(
        public val name: CharSequence? = null,
        public val contentFormatter: Formatter? = DEFAULT_CONTENT_FORMATTER,
        public val decorationFormatter: Formatter? = DEFAULT_DECORATION_FORMATTER,
        public val returnValueFormatter: ((ReturnValue) -> ReturnValue)? = DEFAULT_RESULT_FORMATTER,
        public val border: Border = Border.DOTTED,
    ) : LoggingOptions() {
        override fun newLogger(parent: SimpleRenderingLogger?, fallbackName: String): SimpleRenderingLogger =
            BlockRenderingLogger(
                name ?: fallbackName,
                parent,
                logTextCallOrNull(parent),
                contentFormatter,
                decorationFormatter,
                returnValueFormatter,
                border,
                statusInformationColumn = (parent as? FixedWidthRenderingLogger)?.statusInformationColumn,
                statusInformationPadding = (parent as? FixedWidthRenderingLogger)?.statusInformationPadding,
                statusInformationColumns = (parent as? FixedWidthRenderingLogger)?.statusInformationColumns,
            )

        public companion object : BuilderTemplate<BlockLoggingOptionsContext, BlockLoggingOptions>() {

            public class BlockLoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val name: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
                public val contentFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_CONTENT_FORMATTER
                public val decorationFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_DECORATION_FORMATTER
                public val returnValueFormatter: SkippableCapturingBuilderInterface<() -> ((ReturnValue) -> ReturnValue)?, ((ReturnValue) -> ReturnValue)?> by builder<((ReturnValue) -> ReturnValue)?>() default DEFAULT_RESULT_FORMATTER
                public var border: Border by setter(Border.DOTTED)
            }

            override fun BuildContext.build(): BlockLoggingOptions = ::BlockLoggingOptionsContext {
                BlockLoggingOptions(::name.eval(), ::contentFormatter.eval(), ::decorationFormatter.eval(), ::returnValueFormatter.eval(), ::border.eval())
            }
        }
    }

    /**
     * Renders log messages in a single line.
     */
    public class CompactLoggingOptions(
        public val name: CharSequence? = null,
        public val contentFormatter: Formatter? = DEFAULT_CONTENT_FORMATTER,
        public val decorationFormatter: Formatter? = DEFAULT_DECORATION_FORMATTER,
        public val returnValueFormatter: ((ReturnValue) -> ReturnValue)? = DEFAULT_RESULT_FORMATTER,
    ) : LoggingOptions() {
        override fun newLogger(parent: SimpleRenderingLogger?, fallbackName: String): SimpleRenderingLogger =
            CompactRenderingLogger(
                name ?: fallbackName,
                parent,
                contentFormatter,
                decorationFormatter,
                returnValueFormatter,
                logTextCallOrNull(parent),
            )

        public companion object : BuilderTemplate<CompactLoggingOptionsContext, CompactLoggingOptions>() {

            public class CompactLoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val name: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
                public val contentFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_CONTENT_FORMATTER
                public val returnValueFormatter: SkippableCapturingBuilderInterface<() -> ((ReturnValue) -> ReturnValue)?, ((ReturnValue) -> ReturnValue)?> by builder<((ReturnValue) -> ReturnValue)?>() default DEFAULT_RESULT_FORMATTER
            }

            override fun BuildContext.build(): CompactLoggingOptions = ::CompactLoggingOptionsContext {
                CompactLoggingOptions(::name.eval(), ::contentFormatter.eval(), DEFAULT_DECORATION_FORMATTER, ::returnValueFormatter.eval())
            }
        }
    }

    /**
     * Renders log messages depending on how many messages are logged.
     *
     * Renders like [Block] unless nothing but a result is logged. In the latter case renders like [Compact].
     */
    public class SmartLoggingOptions(
        public val name: CharSequence? = null,
        public val contentFormatter: Formatter? = DEFAULT_CONTENT_FORMATTER,
        public val decorationFormatter: Formatter? = DEFAULT_DECORATION_FORMATTER,
        public val returnValueFormatter: ((ReturnValue) -> ReturnValue)? = DEFAULT_RESULT_FORMATTER,
        public val border: Border = Border.DOTTED,
    ) : LoggingOptions() {
        override fun newLogger(parent: SimpleRenderingLogger?, fallbackName: String): SimpleRenderingLogger =
            SmartRenderingLogger(
                name ?: fallbackName,
                parent,
                logTextCallOrNull(parent),
                contentFormatter,
                decorationFormatter,
                returnValueFormatter,
                border,
                statusInformationColumn = (parent as? FixedWidthRenderingLogger)?.statusInformationColumn,
                statusInformationPadding = (parent as? FixedWidthRenderingLogger)?.statusInformationPadding,
                statusInformationColumns = (parent as? FixedWidthRenderingLogger)?.statusInformationColumns,
                prefix = (parent as? FixedWidthRenderingLogger)?.prefix ?: "",
            )

        public companion object : BuilderTemplate<SmartLoggingOptionsContext, SmartLoggingOptions>() {

            public class SmartLoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val name: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
                public val contentFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_CONTENT_FORMATTER
                public val decorationFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_DECORATION_FORMATTER
                public val returnValueFormatter: SkippableCapturingBuilderInterface<() -> ((ReturnValue) -> ReturnValue)?, ((ReturnValue) -> ReturnValue)?> by builder<((ReturnValue) -> ReturnValue)?>() default DEFAULT_RESULT_FORMATTER
                public var border: Border by setter(Border.DOTTED)
            }

            override fun BuildContext.build(): SmartLoggingOptions = ::SmartLoggingOptionsContext {
                SmartLoggingOptions(::name.eval(), ::contentFormatter.eval(), ::decorationFormatter.eval(), ::returnValueFormatter.eval(), ::border.eval())
            }
        }
    }

    public companion object : BuilderTemplate<LoggingOptionsContext, LoggingOptions>() {

        public val DEFAULT_CONTENT_FORMATTER: Formatter = Formatter.PassThrough
        public val DEFAULT_DECORATION_FORMATTER: Formatter = Colors.brightBlue
        public val DEFAULT_RESULT_FORMATTER: (ReturnValue) -> ReturnValue = { it }

        public class LoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
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
            public fun summary(name: String, maxMessageLength: Int = 20) {
                compact {
                    this.name by name
                    contentFormatter {
                        Formatter {
                            it.takeUnless { it is IO.Meta }?.ansiRemoved?.run {
                                val step = substringAfter(":").trim().run {
                                    takeIf { length < maxMessageLength } ?: split(Regex("\\s+")).last().truncate(maxMessageLength)
                                }
                                Symbols.PointNext + " $step"
                            } ?: ""
                        }
                    }
                }
            }

            /**
             * Formats the output by hiding all details, that is, only the name and an eventual occurring exception is displayed.
             *
             * Example output: `Listing images ✔`
             */
            public fun noDetails(name: String) {
                compact {
                    this.name by name
                    contentFormatter by { "" }
                }
            }

            /**
             * Filters all IO but errors.
             *
             * Example output: `ϟ Process 64207 terminated with exit code 255.`
             */
            public fun errorsOnly(name: String) {
                val none = object : ReturnValue {
                    override val successful: Boolean = true
                    override val symbol: String = ""
                    override val textRepresentation: String? = null
                }
                block {
                    this.name { "" }
                    border = NONE
                    contentFormatter by Formatter {
                        (it as? IO.Error)?.let { err -> "$name: $err" } ?: ""
                    }
                    decorationFormatter by Formatter { "" }
                    returnValueFormatter { { if (it.successful == false) it else none } }
                }
            }
        }

        override fun BuildContext.build(): LoggingOptions = ::LoggingOptionsContext {
            ::block.evalOrNull<BlockLoggingOptions>()
                ?: ::compact.evalOrNull<CompactLoggingOptions>()
                ?: ::smart.evalOrNull<SmartLoggingOptions>()
                ?: SmartLoggingOptions()
        }
    }
}
