package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
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
import koodies.tracing.SpanId
import koodies.tracing.TraceId

/**
 * Renderer that renders events along one or more columns,
 * each displaying one customizable event attribute.
 */
public class BlockRenderer(
    private val settings: Settings,
) : Renderer {

    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence) {
        settings.blockStyle.start(name, settings.decorationFormatter)?.let(settings.printer)
    }

    override fun event(name: CharSequence, attributes: Attributes) {
        val extractedColumns = settings.layout.extract(attributes)
        if (extractedColumns.none { it.first != null }) return
        extractedColumns
            .map { (text, maxColumns) -> text?.let { settings.contentFormatter(it) }?.asAnsiString() to maxColumns }
            .takeIf { it.any { (text, _) -> text != null } }
            ?.let { formatColumns(*it.toTypedArray(), paddingColumns = settings.layout.gap, wrapLines = ::wrapNonUriLines) }
            ?.lineSequence()
            ?.mapNotNull { settings.blockStyle.content(it, settings.decorationFormatter) }
            ?.forEach(settings.printer)
    }

    override fun exception(exception: Throwable, attributes: Attributes) {
        val formatted = exception.stackTraceToString()
        if (attributes.isEmpty) {
            (if (formatted.maxColumns() > settings.layout.totalWidth) wrapNonUriLines(formatted, settings.layout.totalWidth) else formatted)
                .lineSequence()
                .mapNotNull { settings.blockStyle.content(it, settings.decorationFormatter) }
                .map { it.ansi.red }
                .forEach(settings.printer)
        } else {
            event(exception::class.toSimpleClassName(), Attributes.builder().putAll(attributes).put(settings.layout.primaryAttributeKey, formatted).build())
        }
    }

    override fun <R> end(result: Result<R>) {
        val returnValue = ReturnValue.of(result)
        val formatted = settings.blockStyle.end(returnValue, settings.returnValueFormatter, settings.decorationFormatter)
        formatted?.takeUnlessEmpty()
            ?.let { if (it.maxColumns() > settings.layout.totalWidth) wrapNonUriLines(it, settings.layout.totalWidth) else formatted }
            ?.let(settings.printer)
    }

    override fun nestedRenderer(renderer: RendererProvider): Renderer =
        renderer(settings.copy(layout = settings.layout.shrinkBy(settings.blockStyle.indent), printer = ::printChild)) { BlockRenderer(it) }

    override fun printChild(text: CharSequence) {
        text.lineSequence()
            .mapNotNull { settings.blockStyle.parent(it, settings.decorationFormatter) }
            .forEach(settings.printer)
    }

    override fun toString(): String = asString {
        ::settings to settings
    }

    public companion object {
        private fun wrapNonUriLines(text: CharSequence?, maxColumns: Int): CharSequence =
            if (text == null) "" else if (RegularExpressions.uriRegex.containsMatchIn(text)) text else text.wrapLines(maxColumns)
    }
}
