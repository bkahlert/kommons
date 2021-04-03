package koodies.logging

import koodies.asString
import koodies.concurrent.process.IO
import koodies.logging.BorderedRenderingLogger.Border
import koodies.regex.RegularExpressions
import koodies.terminal.AnsiString.Companion.asAnsiString
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Formatter
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.addColumn
import koodies.text.prefixLinesWith
import koodies.text.takeUnlessBlank
import koodies.text.truncate
import koodies.text.wrapLines
import kotlin.properties.Delegates

public open class BlockRenderingLogger(
    caption: CharSequence,
    parent: BorderedRenderingLogger? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    border: Border = DEFAULT_BORDER,
    width: Int? = null,
) : BorderedRenderingLogger(caption.toString(),
    parent,
    contentFormatter,
    decorationFormatter,
    returnValueFormatter,
    border,
    width,
    border.prefix(decorationFormatter)) {


    private fun getBlockStart(): String = border.header(caption, decorationFormatter)

    override var initialized: Boolean by Delegates.observable(false) { _, oldValue, newValue ->
        if (!oldValue && newValue) {
            render(true) { getBlockStart() }
        }
    }

    protected fun getBlockEnd(returnValue: ReturnValue): CharSequence = border.footer(returnValue, returnValueFormatter, decorationFormatter)

    override fun logText(block: () -> CharSequence) {
        contentFormatter(block()).takeIf { it.isNotBlank() }?.run {
            render(false) {
                if (closed) this
                else asAnsiString().prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = true)
            }
        }
    }

    override fun logLine(block: () -> CharSequence) {
        contentFormatter(block()).takeIf { it.isNotBlank() }?.run {
            render(true) {
                val wrapped = wrapNonUriLines(totalColumns)
                if (closed) wrapped
                else wrapped.prefixLinesWith(prefix = prefix, ignoreTrailingSeparator = false)
            }
        }
    }

    override fun logStatus(items: List<HasStatus>, block: () -> CharSequence): Unit {
        block().takeUnlessBlank()?.let { contentFormatter(it) }?.run {
            render(true) {
                val leftColumn = wrapNonUriLines(statusInformationColumn).asAnsiString()
                val statusColumn =
                    items.renderStatus().asAnsiString().truncate(maxLength = statusInformationColumns - 1, MIDDLE)
                val twoColumns = leftColumn.addColumn(statusColumn, columnWidth = statusInformationColumn + statusInformationPadding)
                if (closed) twoColumns
                else twoColumns.prefixLinesWith(prefix = prefix)
            }
        }
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        val returnValue = ReturnValue.of(result)
        val formatted = if (closed) returnValueFormatter(returnValue) else getBlockEnd(returnValue)
        formatted.takeUnlessBlank()?.let { render(true) { it.asAnsiString().wrapNonUriLines(totalColumns) } }
        open = false
        return result.getOrThrow()
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) {
            val message = IO.ERR(it).formatted
            if (closed) message
            else message.prefixLinesWith(prefix, ignoreTrailingSeparator = false)
        }
        open = false
    }

    private fun CharSequence.wrapNonUriLines(length: Int): CharSequence {
        return if (RegularExpressions.uriRegex.containsMatchIn(this)) this else asAnsiString().wrapLines(length)
    }

    override fun toString(): String = asString {
        ::open to open
        ::parent to parent?.caption
        ::caption to caption
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::border to border
        ::prefix to prefix
        ::statusInformationColumn to statusInformationColumn
        ::statusInformationPadding to statusInformationPadding
        ::statusInformationColumns to statusInformationColumns
    }

    public companion object {
        public val DEFAULT_BORDER: Border = Border.DEFAULT
        public fun prefixFor(border: Boolean?, decorationFormatter: Formatter?): String = Border.from(border).prefix(decorationFormatter)
    }
}


/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <R> blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    border: Border = Border.DEFAULT,
    block: BorderedRenderingLogger.() -> R,
): R = BlockRenderingLogger(caption, null, contentFormatter, decorationFormatter, returnValueFormatter, border).runLogging(block)

/**
 * Creates a logger which serves for logging a sub-process and all of its corresponding events.
 *
 * This logger uses at least one line per log event. If less room is available [compactLogging] is more suitable.
 */
@Deprecated("only use member function")
@RenderingLoggingDsl
public fun <T : RenderingLogger?, R> T.blockLogging(
    caption: CharSequence,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = Formatter { it.red() },
    returnValueFormatter: ((ReturnValue) -> String)? = null,
    border: Border = Border.DEFAULT,
    block: BorderedRenderingLogger.() -> R,
): R =
    if (this is BorderedRenderingLogger) blockLogging(caption, contentFormatter, decorationFormatter, returnValueFormatter, border, block)
    else koodies.logging.blockLogging(caption, contentFormatter, decorationFormatter, returnValueFormatter, border, block)
