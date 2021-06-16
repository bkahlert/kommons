package koodies.tracing.rendering

import koodies.asString
import koodies.logging.ReturnValue
import koodies.regex.RegularExpressions
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.lineSequence
import koodies.text.LineSeparators.wrapLines
import koodies.text.formatColumns
import koodies.text.maxColumns
import koodies.text.takeUnlessEmpty
import koodies.toSimpleClassName
import koodies.tracing.Span.State.Ended
import koodies.tracing.Span.State.Started

public class BlockRenderer(
    private val style: Style = Styles.DEFAULT,
    private val format: ColumnsFormat = ColumnsFormat(),
    private val contentFormatter: Formatter = Formatter.PassThrough,
    private val decorationFormatter: Formatter = Formatter.PassThrough,
    private val returnValueFormatter: ((ReturnValue) -> ReturnValue)? = null,
    private val handleOverflow: (CharSequence?, Int) -> CharSequence = { text, maxColumns -> wrapNonUriLines(text, maxColumns) },
    private val printer: Printer,
) : Renderer {

    override fun start(name: CharSequence, started: Started) {
        val formatted = style.header(name, contentFormatter)
        printer(formatted)
    }

    override fun event(name: CharSequence, description: CharSequence, attributes: Map<CharSequence, CharSequence>) {
        format.extract(description, attributes)
            .map { (text, maxColumns) -> text?.let { contentFormatter(it) }?.asAnsiString() to maxColumns }
            .let { formatColumns(*it.toTypedArray(), paddingColumns = format.gap, wrapLines = handleOverflow) }
            .lineSequence()
            .map { style.line(it, decorationFormatter) }
            .forEach(printer)
    }

    override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>) {
        val formatted = exception.stackTraceToString()
        if (attributes.isEmpty()) {
            (if (formatted.maxColumns() > format.maxColumns) handleOverflow(formatted, format.maxColumns) else formatted)
                .lineSequence()
                .map { style.line(it, decorationFormatter) }
                .map { it.ansi.red }
                .forEach(printer)
        } else event(exception::class.toSimpleClassName(), formatted, attributes)
    }

    override fun spanning(name: CharSequence): Renderer = spanning()

    public fun spanning(
        style: Style = this.style,
        contentFormatter: Formatter = this.contentFormatter,
        decorationFormatter: Formatter = this.decorationFormatter,
        returnValueFormatter: ((ReturnValue) -> ReturnValue)? = this.returnValueFormatter,
    ): BlockRenderer =
        BlockRenderer(style, format.shrinkBy(style.indent), contentFormatter, decorationFormatter, returnValueFormatter, handleOverflow) {
            it.lineSequence()
                .map { style.parentLine(it, decorationFormatter) }
                .forEach(printer)
        }

    override fun end(ended: Ended) {
        val returnValue = ReturnValue.of(ended)
        val formatted = style.footer(returnValue, returnValueFormatter ?: { it }, decorationFormatter)
        formatted.takeUnlessEmpty()?.let {
            val wrapped = if (it.maxColumns() > format.maxColumns) handleOverflow(it, format.maxColumns) else formatted
            printer(wrapped)
        }
    }

    override fun toString(): String = asString {
        ::format to format
        ::style to style
        ::handleOverflow to handleOverflow
        ::contentFormatter to contentFormatter
        ::decorationFormatter to decorationFormatter
        ::returnValueFormatter to returnValueFormatter
    }

    public companion object {
        private fun wrapNonUriLines(text: CharSequence?, maxColumns: Int): CharSequence =
            if (text == null) "" else if (RegularExpressions.uriRegex.containsMatchIn(text)) text else text.wrapLines(maxColumns)
    }
}
