package koodies.tracing.rendering

import koodies.asString
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

    private val style = settings.blockStyle(settings.layout, settings.indent)

    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence) {
        settings.nameFormatter(name)
            ?.let { style.start(it, settings.decorationFormatter) }
            ?.let(settings.printer)
    }

    override fun event(name: CharSequence, attributes: RenderableAttributes) {
        val extractedColumns = style.layout.extract(attributes)
        if (extractedColumns.none { it.first != null }) return
        extractedColumns
            .map { (text, maxColumns) -> text?.let { settings.contentFormatter(it) }?.asAnsiString() to maxColumns }
            .takeIf { it.any { (text, _) -> text != null } }
            ?.let { formatColumns(*it.toTypedArray(), paddingColumns = style.layout.gap, wrapLines = ::wrapNonUriLines) }
            ?.lineSequence()
            ?.mapNotNull { style.content(it, settings.decorationFormatter) }
            ?.forEach(settings.printer)
    }

    override fun exception(exception: Throwable, attributes: RenderableAttributes) {
        val formatted = exception.stackTraceToString()
        if (attributes.isEmpty()) {
            (if (formatted.maxColumns() > style.layout.totalWidth) wrapNonUriLines(formatted, style.layout.totalWidth) else formatted)
                .lineSequence()
                .mapNotNull { style.content(it, settings.decorationFormatter) }
                .map { it.ansi.red }
                .forEach(settings.printer)
        } else {
            event(exception::class.toSimpleClassName(),
                RenderableAttributes.of(*attributes.toList().toTypedArray(), style.layout.primaryKey to formatted))
        }
    }

    override fun <R> end(result: Result<R>) {
        val returnValue = ReturnValue.of(result)
        val formatted = style.end(returnValue, settings.returnValueTransform, settings.decorationFormatter)
        formatted?.takeUnlessEmpty()
            ?.let { if (it.maxColumns() > style.layout.totalWidth) wrapNonUriLines(it, style.layout.totalWidth) else formatted }
            ?.let(settings.printer)
    }

    override fun childRenderer(renderer: RendererProvider): Renderer =
        renderer(settings.copy(layout = settings.layout, indent = settings.indent + style.indent, printer = ::printChild)) { BlockRenderer(it) }

    override fun printChild(text: CharSequence) {
        text.lineSequence()
            .mapNotNull { style.parent(it, settings.decorationFormatter) }
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
