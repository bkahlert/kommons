package koodies.tracing.rendering

import koodies.asString
import koodies.logging.ReturnValue
import koodies.regex.RegularExpressions
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.lineSequence
import koodies.text.LineSeparators.wrapLines
import koodies.text.formatColumns
import koodies.text.maxColumns
import koodies.text.takeUnlessEmpty
import koodies.toSimpleClassName
import koodies.tracing.Span.Companion.Description
import koodies.tracing.Span.State.Ended
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import java.time.Instant

/**
 * Renderer that renders events along one or more columns,
 * each displaying one customizable event attribute.
 */
public class BlockRenderer(
    private val name: CharSequence,
    private val settings: Settings,
    private val printer: Printer,
) : Renderer {

    override fun start(traceId: TraceId, spanId: SpanId, timestamp: Instant) {
        settings.blockStyle.start(name, settings.decorationFormatter)?.let(printer)
    }

    override fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        val extractedColumns = settings.layout.extract(attributes)
        if (extractedColumns.none { it.first != null }) return
        extractedColumns
            .map { (text, maxColumns) -> text?.let { settings.contentFormatter(it) }?.asAnsiString() to maxColumns }
            .let { formatColumns(*it.toTypedArray(), paddingColumns = settings.layout.gap, wrapLines = ::wrapNonUriLines) }
            .lineSequence()
            .mapNotNull { settings.blockStyle.content(it, settings.decorationFormatter) }
            .forEach(printer)
    }

    override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        val formatted = exception.stackTraceToString()
        if (attributes.isEmpty()) {
            (if (formatted.maxColumns() > settings.layout.totalWidth) wrapNonUriLines(formatted, settings.layout.totalWidth) else formatted)
                .lineSequence()
                .mapNotNull { settings.blockStyle.content(it, settings.decorationFormatter) }
                .map { it.ansi.red }
                .forEach(printer)
        } else event(exception::class.toSimpleClassName(), mapOf(Description to formatted, *attributes.toList().toTypedArray()), timestamp)
    }

    override fun end(ended: Ended) {
        val returnValue = ReturnValue.of(ended)
        val formatted = settings.blockStyle.end(returnValue, settings.returnValueFormatter, settings.decorationFormatter)
        formatted?.takeUnlessEmpty()
            ?.let { if (it.maxColumns() > settings.layout.totalWidth) wrapNonUriLines(it, settings.layout.totalWidth) else formatted }
            ?.let(printer)
    }

    override fun nestedRenderer(name: CharSequence, customize: Settings.() -> Settings): Renderer =
        nestedRenderer { settings, printer -> BlockRenderer(name, settings.customize(), printer) }

    override fun nestedRenderer(provider: (Settings, Printer) -> Renderer): Renderer =
        provider(settings.copy(layout = settings.layout.shrinkBy(settings.blockStyle.indent))) {
            it.lineSequence()
                .mapNotNull { settings.blockStyle.parent(it, settings.decorationFormatter) }
                .forEach(printer)
        }

    override fun toString(): String = asString {
        ::name to name
        ::settings to settings
        ::printer to printer
    }

    public companion object {
        private fun wrapNonUriLines(text: CharSequence?, maxColumns: Int): CharSequence =
            if (text == null) "" else if (RegularExpressions.uriRegex.containsMatchIn(text)) text else text.wrapLines(maxColumns)
    }
}
