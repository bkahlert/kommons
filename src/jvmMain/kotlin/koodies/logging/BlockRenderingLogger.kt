package koodies.logging

import koodies.asString
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.runIgnoringTrailingLineSeparator
import koodies.text.takeUnlessEmpty

public open class BlockRenderingLogger(
    name: CharSequence,
    parent: SimpleRenderingLogger?,
    log: ((String) -> Unit)? = null,
    contentFormatter: FilteringFormatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    border: Border = DEFAULT_BORDER,
    statusInformationColumn: Int? = null,
    statusInformationPadding: Int? = null,
    statusInformationColumns: Int? = null,
    width: Int? = null,
) : FixedWidthRenderingLogger(
    name.toString(),
    parent,
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

    override fun onStart(): Unit = render { border.header(name, decorationFormatter) }

    protected fun getBlockEnd(returnValue: ReturnValue): CharSequence = border.footer(returnValue, returnValueFormatter, decorationFormatter)

    override fun logText(block: () -> CharSequence) {
        block.format(contentFormatter) {
            render {
                runIgnoringTrailingLineSeparator {
                    wrapNonUriLines(totalColumns).prefixLinesWith(prefix)
                }
            }
        }
    }

    // TODO merge with logText
    override fun logLine(block: () -> CharSequence) {
        return logText(block)
    }

    private fun ReturnValue.withSymbol(symbol: String) = object : ReturnValue by this {
        override val symbol: String = symbol
    }

    override fun <R> logResult(result: Result<R>): R {
        val returnValue = ReturnValue.of(result)
        val formatted = getBlockEnd(returnValue)
        formatted.takeUnlessEmpty()?.let { render { it.asAnsiString().wrapNonUriLines(totalColumns).toString() + LF } }
        close(result)
        return result.getOrThrow()
    }

    override fun toString(): String = asString {
        ::open to open
        ::name to name
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
