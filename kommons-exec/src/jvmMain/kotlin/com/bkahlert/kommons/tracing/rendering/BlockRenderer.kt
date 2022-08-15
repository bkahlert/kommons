package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.debug.renderType
import com.bkahlert.kommons.text.ANSI
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.LineSeparators.lineSequence
import com.bkahlert.kommons.text.UriRegex
import com.bkahlert.kommons.text.graphemeCount
import com.bkahlert.kommons.text.takeUnlessEmpty
import com.bkahlert.kommons.tracing.SpanId
import com.bkahlert.kommons.tracing.TraceId
import com.github.ajalt.mordant.rendering.OverflowWrap.BREAK_WORD
import com.github.ajalt.mordant.rendering.VerticalAlign.TOP
import com.github.ajalt.mordant.rendering.Whitespace.PRE_LINE
import com.github.ajalt.mordant.table.Borders.NONE
import com.github.ajalt.mordant.table.ColumnWidth
import com.github.ajalt.mordant.table.grid

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
            ?.let {
                ANSI.terminal.render(
                    grid {
                        cellBorders = NONE
                        padding(0, 0, 0, 0)
                        it.forEachIndexed { i, (_, maxWidth) ->
                            column(i) {
                                if (i == 0) {
                                    width = ColumnWidth.Fixed(maxWidth)
                                    padding(0)
                                } else {
                                    width = ColumnWidth.Fixed(maxWidth + this@BlockRenderer.style.layout.gap)
                                    padding(0, this@BlockRenderer.style.layout.gap)
                                }
                            }
                        }
                        row {
                            whitespace = PRE_LINE
                            verticalAlign = TOP
                            overflowWrap = BREAK_WORD
                            cellsFrom(it.map { it.first })
                        }
                    }
                )
            }
            ?.lineSequence()
            ?.map { it.trimEnd() }
            ?.mapNotNull { style.content(it, settings.decorationFormatter) }
            ?.forEach(settings.printer)
    }

    override fun exception(exception: Throwable, attributes: RenderableAttributes) {
        val formatted = exception.stackTraceToString()
        if (attributes.isEmpty()) {
            (if (formatted.graphemeCount() > style.layout.totalWidth) wrapNonUriLines(formatted, style.layout.totalWidth) else formatted)
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
            ?.let { if (it.toString().graphemeCount() > style.layout.totalWidth) wrapNonUriLines(it, style.layout.totalWidth) else formatted }
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
