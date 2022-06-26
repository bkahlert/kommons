package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.LineSeparators.lineSequence
import com.bkahlert.kommons.UriRegex
import com.bkahlert.kommons.asString
import com.bkahlert.kommons.debug.renderType
import com.bkahlert.kommons.takeUnlessEmpty
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.formatColumns
import com.bkahlert.kommons.text.maxColumns
import com.bkahlert.kommons.tracing.SpanId
import com.bkahlert.kommons.tracing.TraceId

/**
 * Renderer that renders events along one or more columns,
 * each displaying one customizable event attribute.
 */
public class BlockRenderer(
    private val settings: Settings,
) : Renderer {

    private val style = settings.style(settings.layout, settings.indent)

    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence) {
        style.start(name, settings.nameFormatter, settings.decorationFormatter)
            ?.lineSequence()
            ?.map { it.trimEnd() }
            ?.forEach(settings.printer)
    }

    override fun event(name: CharSequence, attributes: RenderableAttributes) {
        val extractedColumns = style.layout.extract(attributes)
        if (extractedColumns.none { it.first != null }) return
        extractedColumns
            .map { (text, maxColumns) -> text?.let { settings.contentFormatter(it) } to maxColumns }
            .takeIf { it.any { (text, _) -> text != null } }
            ?.let { formatColumns(*it.toTypedArray(), paddingColumns = style.layout.gap, wrapLines = ::wrapNonUriLines) }
            ?.lineSequence()
            ?.map { it.trimEnd() }
            ?.mapNotNull { style.content(it, settings.decorationFormatter) }
            ?.forEach(settings.printer)
    }

    override fun exception(exception: Throwable, attributes: RenderableAttributes) {
        val formatted = exception.stackTraceToString()
        if (attributes.isEmpty()) {
            (if (formatted.maxColumns() > style.layout.totalWidth) wrapNonUriLines(formatted, style.layout.totalWidth) else formatted)
                .lineSequence()
                .map { it.trimEnd() }
                .mapNotNull { style.content(it, settings.decorationFormatter) }
                .map { it.ansi.red }
                .forEach(settings.printer)
        } else {
            event(
                exception.renderType(),
                RenderableAttributes.of(*attributes.toList().toTypedArray(), style.layout.primaryKey to formatted)
            )
        }
    }

    override fun <R> end(result: Result<R>) {
        val returnValue = ReturnValue.of(result)
        val formatted = style.end(returnValue, settings.returnValueTransform, settings.decorationFormatter)
        formatted?.takeUnlessEmpty()
            ?.let { if (it.maxColumns() > style.layout.totalWidth) wrapNonUriLines(it, style.layout.totalWidth) else formatted }
            ?.lineSequence()
            ?.map { it.trimEnd() }
            ?.forEach(settings.printer)
    }

    override fun childRenderer(renderer: RendererProvider): Renderer =
        renderer(settings.copy(indent = settings.indent + style.indent, printer = ::printChild)) { BlockRenderer(it) }

    override fun printChild(text: CharSequence) {
        text.toAnsiString()
            .lineSequence()
            .mapNotNull { style.parent(it, settings.decorationFormatter) }
            .forEach(settings.printer)
    }

    override fun toString(): String = asString {
        put(::settings, settings)
    }

    public companion object : RendererFactory {

        override fun create(settings: Settings): Renderer {
            return BlockRenderer(settings)
        }

        private fun wrapNonUriLines(text: CharSequence?, maxColumns: Int): CharSequence =
            when {
                text == null -> ""
                Regex.UriRegex.containsMatchIn(text) -> text
                else -> Renderable.of(text).render(maxColumns, null)
            }
    }
}
