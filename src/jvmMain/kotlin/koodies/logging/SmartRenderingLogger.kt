package koodies.logging

import koodies.asString
import koodies.exception.toCompactString
import koodies.logging.BlockRenderingLogger.Companion.DEFAULT_BORDER
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.text.ANSI.Formatter

/**
 * Logs like a [BlockRenderingLogger] unless only the result is logged.
 * In that case the result will be logged on the same line as the caption instead of a new one.
 */
public open class SmartRenderingLogger(
    // TODO extract proper logger interface and solely delegate; no inheritance
    caption: CharSequence,
    log: ((String) -> Unit)? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = Border.DEFAULT,
    statusInformationColumn: Int? = null,
    statusInformationPadding: Int? = null,
    statusInformationColumns: Int? = null,
    prefix: String,
) : FixedWidthRenderingLogger(
    caption.toString(),
    { error("Implementation misses to delegate log messages; consider refactoring") },
    contentFormatter,
    decorationFormatter,
    returnValueFormatter,
    border,
    statusInformationColumn,
    statusInformationPadding,
    statusInformationColumns,
    prefix = prefix,
) {

    private var loggingResult: Boolean = false

    private val logger: RenderingLogger by lazy {
        if (!loggingResult) BlockRenderingLogger(
            caption,
            log,
            contentFormatter,
            decorationFormatter,
            returnValueFormatter,
            border,
            statusInformationColumn,
            statusInformationPadding,
            statusInformationColumns)
        else CompactRenderingLogger(caption, contentFormatter, decorationFormatter, returnValueFormatter, log)
    }

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {
        logger.render(trailingNewline, block)
    }

    override fun logText(block: () -> CharSequence) {
        logger.logText(block)
    }

    override fun logLine(block: () -> CharSequence) {
        logger.logLine(block)
    }

    override fun logStatus(items: List<CharSequence>, block: () -> CharSequence) {
        when (val logger = logger) {
            is FixedWidthRenderingLogger -> logger.logStatus(items, block)
            is CompactRenderingLogger -> logger.logStatus(items, block)
            is MicroLogger -> logger.logStatus(items, block)
            else -> logger.logText { block().toString() + items.format(Formatter.fromScratch { red }) { toCompactString() } }
        }
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        loggingResult = ReturnValue.of(block()).successful != null
        return logger.logResult(block)
    }

    override fun logException(block: () -> Throwable) {
        logger.logException(block)
    }

    override fun toString(): String = asString {
        ::open to open
        ::caption to caption
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::returnValueFormatter to returnValueFormatter
        ::border to border
        ::initialized to initialized
        ::logger to if (initialized) logger else "not initialized yet"
    }
}

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <R> logging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = DEFAULT_BORDER,
    block: FixedWidthRenderingLogger.() -> R,
): R = SmartRenderingLogger(
    caption,
    { BACKGROUND.logText { it } },
    contentFormatter,
    decorationFormatter,
    returnValueFormatter,
    border,
    prefix = BACKGROUND.prefix,
).runLogging(block)
