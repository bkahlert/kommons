package koodies.logging

import koodies.asString
import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.YesNo
import koodies.builder.BooleanBuilder.YesNo.Context
import koodies.builder.BuilderTemplate
import koodies.builder.context.CapturesMap
import koodies.builder.context.CapturingContext
import koodies.builder.context.SkippableCapturingBuilderInterface
import koodies.logging.LoggingOptions.BlockLoggingOptions.Companion.BlockLoggingOptionsContext
import koodies.logging.LoggingOptions.CompactLoggingOptions.Companion.CompactLoggingOptionsContext
import koodies.logging.LoggingOptions.Companion.LoggingOptionsContext
import koodies.logging.LoggingOptions.SmartLoggingOptions.Companion.SmartLoggingOptionsContext
import koodies.text.ANSI

/**
 * Logs like a [BlockRenderingLogger] unless only the result is logged.
 * In that case the result will be logged on the same line as the caption instead of a new one.
 */
public class SmartRenderingLogger(
    caption: CharSequence,
    override val contentFormatter: Formatter = { it },
    override val decorationFormatter: Formatter = { it },
    override val bordered: Boolean,
    override val statusInformationColumn: Int = 100,
    override val statusInformationPadding: Int = 5,
    override val statusInformationColumns: Int = 45,
    parent: RenderingLogger? = null,
    public val blockRenderingLogger: (SmartRenderingLogger) -> BlockRenderingLogger,
) : BorderedRenderingLogger(caption.toString(), parent, { throw IllegalStateException("All log calls must be delegated to the encapsulated logger.") }) {

    init {
        closed = true
    }
    
    private var logged: Boolean = false

    override val prefix: String
        get() {
            logged = true
            return (logger as? BlockRenderingLogger)?.prefix ?: ""
        }

    private val logger: RenderingLogger by lazy {
        if (logged) blockRenderingLogger(this)
        else CompactRenderingLogger(caption, contentFormatter, this) {
            parent?.apply {
                logLine { it }
            } ?: println(it)
        }
    }

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {
        logger.render(trailingNewline, block)
    }

    override fun logText(block: () -> CharSequence) {
        logged = true
        logger.logText(block)
    }

    override fun logLine(block: () -> CharSequence) {
        logged = true
        logger.logLine(block)
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        logged = true
        logger.logStatus(items, block)
    }

    override fun logException(block: () -> Throwable) {
        logged = true
        logger.logException(block)
    }

    override fun <R> logResult(block: () -> Result<R>): R =
        logger.logResult(block)

    override fun toString(): String = asString {
        ::parent to parent?.caption
        ::caption to caption
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::bordered to bordered
        ::statusInformationColumn to statusInformationColumn
        ::statusInformationPadding to statusInformationPadding
        ::statusInformationColumns to statusInformationColumns
        ::blockRenderingLogger to blockRenderingLogger
        ::logged to logged
        ::logger to if (logged) logger else "not initialized yet"
    }
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public fun <T : MutedRenderingLogger, R> T.logging(
    caption: CharSequence,
    contentFormatter: Formatter = { it },
    decorationFormatter: Formatter = { it },
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    block: T.() -> R,
): R = runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public fun <T : BorderedRenderingLogger, R> T.logging(
    caption: CharSequence,
    contentFormatter: Formatter = this.contentFormatter,
    decorationFormatter: Formatter = this.decorationFormatter,
    bordered: Boolean = this.bordered,
    block: SmartRenderingLogger.() -> R,
): R = SmartRenderingLogger(
    caption, contentFormatter, decorationFormatter, bordered,
    statusInformationColumn = statusInformationColumn - prefix.length,
    statusInformationPadding = statusInformationPadding,
    statusInformationColumns = statusInformationColumns - prefix.length,
    parent = this,
) { parent ->
    BlockRenderingLogger(
        // TODO simplify
        caption, parent, contentFormatter, decorationFormatter, bordered,
        statusInformationColumn = statusInformationColumn - prefix.length,
        statusInformationPadding = statusInformationPadding,
        statusInformationColumns = statusInformationColumns - prefix.length,
    ) { output -> logText { output } }
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public fun <T : RenderingLogger, R> T.logging(
    caption: CharSequence,
    contentFormatter: Formatter = (this as? BorderedRenderingLogger)?.contentFormatter ?: { it },
    decorationFormatter: Formatter = (this as? BorderedRenderingLogger)?.decorationFormatter ?: { it },
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    block: SmartRenderingLogger.() -> R,
): R = SmartRenderingLogger(caption, contentFormatter, decorationFormatter, bordered, parent = this) { parent ->
    BlockRenderingLogger(caption, parent, contentFormatter, decorationFormatter, bordered) { output -> logText { output } }
}.runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@RenderingLoggingDsl
public fun <R> logging(
    caption: CharSequence,
    contentFormatter: Formatter = { it },
    decorationFormatter: Formatter = { it },
    bordered: Boolean = false,
    block: SmartRenderingLogger.() -> R,
): R = SmartRenderingLogger(caption, contentFormatter, decorationFormatter, bordered, parent = null) { parent ->
    BlockRenderingLogger(caption, parent, contentFormatter, decorationFormatter, bordered)
}.runLogging(block) // TODO apply formatter

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@JvmName("nullableLogging")
@RenderingLoggingDsl
public fun <T : RenderingLogger?, R> T.logging(
    caption: CharSequence,
    contentFormatter: Formatter = (this as? BorderedRenderingLogger)?.contentFormatter ?: { it },
    decorationFormatter: Formatter = (this as? BorderedRenderingLogger)?.decorationFormatter ?: { it },
    bordered: Boolean = (this as? BorderedRenderingLogger)?.bordered ?: false,
    block: SmartRenderingLogger.() -> R,
): R =
    if (this is RenderingLogger) logging(caption, contentFormatter, decorationFormatter, bordered, block)
    else koodies.logging.logging(caption, contentFormatter, decorationFormatter, bordered, block)

/**
 * Options that define how a [RenderingLogger] renders log messages.
 */
public sealed class LoggingOptions {

    public abstract fun <R> render(logger: RenderingLogger?, fallbackCaption: String, block: RenderingLogger.() -> R): R

    /**
     * Renders log messages line-by-line.
     */
    public class BlockLoggingOptions(
        public val caption: CharSequence? = null,
        public val contentFormatter: Formatter = DEFAULT_CONTENT_FORMATTER,
        public val decorationFormatter: Formatter = DEFAULT_DECORATION_FORMATTER,
        public val bordered: Boolean = false,
    ) : LoggingOptions() {
        override fun <R> render(logger: RenderingLogger?, fallbackCaption: String, block: RenderingLogger.() -> R): R =
            logger.blockLogging(caption ?: fallbackCaption, contentFormatter, decorationFormatter, bordered) { block() }

        public companion object : BuilderTemplate<BlockLoggingOptionsContext, BlockLoggingOptions>() {

            public class BlockLoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val caption: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
                public val contentFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_CONTENT_FORMATTER
                public val decorationFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_DECORATION_FORMATTER
                public val border: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): BlockLoggingOptions = ::BlockLoggingOptionsContext {
                BlockLoggingOptions(::caption.eval(), ::contentFormatter.eval(), ::decorationFormatter.eval(), ::border.eval())
            }
        }
    }

    /**
     * Renders log messages in a single line.
     */
    public class CompactLoggingOptions(
        public val caption: CharSequence? = null,
        public val contentFormatter: Formatter = DEFAULT_CONTENT_FORMATTER,
    ) : LoggingOptions() {
        override fun <R> render(logger: RenderingLogger?, fallbackCaption: String, block: RenderingLogger.() -> R): R =
            logger.compactLogging(caption ?: fallbackCaption, contentFormatter) { block() }

        public companion object : BuilderTemplate<CompactLoggingOptionsContext, CompactLoggingOptions>() {

            public class CompactLoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val caption: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
                public val contentFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_CONTENT_FORMATTER
            }

            override fun BuildContext.build(): CompactLoggingOptions = ::CompactLoggingOptionsContext {
                CompactLoggingOptions(::caption.eval(), ::contentFormatter.eval())
            }
        }
    }

    /**
     * Renders log messages depending on how many messages are logged.
     *
     * Renders like [Block] unless nothing but a result is logged. In the latter case renders like [Compact].
     */
    public class SmartLoggingOptions(
        public val caption: CharSequence? = null,
        public val contentFormatter: Formatter = DEFAULT_CONTENT_FORMATTER,
        public val decorationFormatter: Formatter = DEFAULT_DECORATION_FORMATTER,
        public val bordered: Boolean = false,
    ) : LoggingOptions() {
        override fun <R> render(logger: RenderingLogger?, fallbackCaption: String, block: RenderingLogger.() -> R): R =
            logger.logging(caption ?: fallbackCaption, contentFormatter, decorationFormatter, bordered) { block() }

        public companion object : BuilderTemplate<SmartLoggingOptionsContext, SmartLoggingOptions>() {

            public class SmartLoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
                public val caption: SkippableCapturingBuilderInterface<() -> String, String?> by builder()
                public val contentFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_CONTENT_FORMATTER
                public val decorationFormatter: SkippableCapturingBuilderInterface<() -> Formatter?, Formatter?> by builder<Formatter?>() default DEFAULT_DECORATION_FORMATTER
                public val border: SkippableCapturingBuilderInterface<Context.() -> BooleanValue, Boolean> by YesNo default false
            }

            override fun BuildContext.build(): SmartLoggingOptions = ::SmartLoggingOptionsContext {
                SmartLoggingOptions(::caption.eval(), ::contentFormatter.eval(), ::decorationFormatter.eval(), ::border.eval())
            }
        }
    }

    public companion object : BuilderTemplate<LoggingOptionsContext, LoggingOptions>() {

        public val DEFAULT_CONTENT_FORMATTER: Formatter = { it }
        public val DEFAULT_DECORATION_FORMATTER: Formatter = { ANSI.Colors.brightBlue(it) }

        public class LoggingOptionsContext(override val captures: CapturesMap) : CapturingContext() {
            public val block: SkippableCapturingBuilderInterface<BlockLoggingOptionsContext.() -> Unit, BlockLoggingOptions?> by BlockLoggingOptions
            public val compact: SkippableCapturingBuilderInterface<CompactLoggingOptionsContext.() -> Unit, CompactLoggingOptions?> by CompactLoggingOptions
            public val smart: SkippableCapturingBuilderInterface<SmartLoggingOptionsContext.() -> Unit, SmartLoggingOptions?> by SmartLoggingOptions
        }

        override fun BuildContext.build(): LoggingOptions = ::LoggingOptionsContext {
            ::block.evalOrNull<BlockLoggingOptions>()
                ?: ::compact.evalOrNull<CompactLoggingOptions>()
                ?: ::smart.evalOrNull<SmartLoggingOptions>()
                ?: SmartLoggingOptions()
        }
    }
}

public typealias Formatter = (CharSequence) -> CharSequence?
