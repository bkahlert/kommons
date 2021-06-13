package koodies.logging

import koodies.asString
import koodies.exception.toCompactString
import koodies.text.ANSI.Formatter

/**
 * Logs like a [BlockRenderingLogger] unless only the result is logged.
 * In that case the result will be logged on the same line as the name instead of a new one.
 */
public open class SmartRenderingLogger(
    // TODO extract proper logger interface and solely delegate; no inheritance
    name: CharSequence,
    parent: SimpleRenderingLogger?,
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
    name.toString(),
    null,
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

    private val logger: SimpleRenderingLogger by lazy {
        if (!loggingResult) BlockRenderingLogger(
            name,
            parent,
            log,
            contentFormatter,
            decorationFormatter,
            returnValueFormatter,
            border,
            statusInformationColumn,
            statusInformationPadding,
            statusInformationColumns)
        else CompactRenderingLogger(name, parent, contentFormatter, decorationFormatter, returnValueFormatter, log)
    }

    override fun render(block: () -> CharSequence) {
        logger.render(block)
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

    override fun <R> logResult(result: Result<R>): R {
        loggingResult = ReturnValue.of(result).successful != null
        return logger.logResult(result)
    }

    override fun toString(): String = asString {
        ::open to open
        ::name to name
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::returnValueFormatter to returnValueFormatter
        ::border to border
        ::started to started
        ::logger to if (started) logger else "not initialized yet"
    }
}
