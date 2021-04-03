package koodies.logging

import koodies.asString
import koodies.logging.BlockRenderingLogger.Companion.DEFAULT_BORDER
import koodies.logging.BorderedRenderingLogger.Border
import koodies.text.ANSI.Formatter

/**
 * Logs like a [BlockRenderingLogger] unless only the result is logged.
 * In that case the result will be logged on the same line as the caption instead of a new one.
 */
public open class SmartRenderingLogger(
    // TODO extract proper logger interface and solely delegate; no inheritance
    caption: CharSequence,
    parent: BorderedRenderingLogger? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    border: Border = Border.DEFAULT,
    override val missingParentFallback: (String) -> Unit = {
        error("Implementation misses to delegate log messages; consider refactoring")
    },
) : BorderedRenderingLogger(caption.toString(), parent, contentFormatter, decorationFormatter, returnValueFormatter, border, prefix = parent?.prefix ?: "") {

    private var loggingResult: Boolean = false

    private val logger: RenderingLogger by lazy {
        if (!loggingResult) BlockRenderingLogger(caption, parent, contentFormatter, decorationFormatter, returnValueFormatter, border)
        else CompactRenderingLogger(caption, contentFormatter, decorationFormatter, returnValueFormatter, parent)
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

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence) {
        logger.logStatus(items, block)
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
        ::parent to parent?.caption
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
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    border: Border = DEFAULT_BORDER,
    block: BorderedRenderingLogger.() -> R,
): R = SmartRenderingLogger(caption, null, contentFormatter, decorationFormatter, returnValueFormatter, border).runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <T : RenderingLogger?, R> T.logging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    border: Border = DEFAULT_BORDER,
    block: BorderedRenderingLogger.() -> R,
): R =
    if (this is BorderedRenderingLogger) logging(caption, contentFormatter, decorationFormatter, returnValueFormatter, border, block)
    else koodies.logging.logging(caption, contentFormatter, decorationFormatter, returnValueFormatter, border, block)

