package koodies.logging

import koodies.asString
import koodies.concurrent.process.IO
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.text.ANSI.Colors.red
import koodies.text.ANSI.Formatter
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.Semantics.Symbols.Computation
import koodies.text.prefixLinesWith
import koodies.text.takeUnlessBlank
import kotlin.properties.Delegates

public open class BlockRenderingLogger(
    caption: CharSequence,
    log: ((String) -> Unit)? = null,
    contentFormatter: Formatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = DEFAULT_BORDER,
    statusInformationColumn: Int? = null,
    statusInformationPadding: Int? = null,
    statusInformationColumns: Int? = null,
    width: Int? = null,
) : FixedWidthRenderingLogger(caption.toString(),
    log,
    contentFormatter,
    decorationFormatter,
    returnValueFormatter,
    border,
    statusInformationColumn,
    statusInformationPadding,
    statusInformationColumns,
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

    private fun ReturnValue.withSymbol(symbol: String) = object : ReturnValue by this {
        override val symbol: String = symbol
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val result = block()
        val returnValue = ReturnValue.of(result)
        val formatted = if (closed) returnValueFormatter(returnValue.withSymbol(Computation)).format() else getBlockEnd(returnValue)
        formatted.takeUnlessBlank()?.let { render(true) { it.asAnsiString().wrapNonUriLines(totalColumns) } }
        open = false
        return result.getOrThrow()
    }

    override fun logException(block: () -> Throwable): Unit = block().let {
        render(true) {
            val message = IO.Error(it).formatted
            if (closed) message
            else message.prefixLinesWith(prefix, ignoreTrailingSeparator = false)
        }
        open = false
    }

    override fun toString(): String = asString {
        ::open to open
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
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = Border.DEFAULT,
    block: FixedWidthRenderingLogger.() -> R,
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
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = Border.DEFAULT,
    block: FixedWidthRenderingLogger.() -> R,
): R =
    if (this is FixedWidthRenderingLogger) blockLogging(caption, contentFormatter, decorationFormatter, returnValueFormatter, border, block)
    else koodies.logging.blockLogging(caption, contentFormatter, decorationFormatter, returnValueFormatter, border, block)
