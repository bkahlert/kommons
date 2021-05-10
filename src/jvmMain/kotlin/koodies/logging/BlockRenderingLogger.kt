package koodies.logging

import koodies.asString
import koodies.exec.IO
import koodies.text.ANSI.Formatter
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.Semantics.Symbols.Computation
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

