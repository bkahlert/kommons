package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.tracing.SpanId
import com.bkahlert.kommons.tracing.TraceId

/**
 * Renderer that behaves like a [BlockRenderer] with two exceptions:
 * - If [end] is called with no events in between, the result is logged with a [OneLineRenderer].
 * - If [end] is called with no events but nested compact renderers with no results, the result is logged with a [OneLineRenderer].
 *
 * The goal of this renderer is to not waste space for immediately returning spans.
 */
public class CompactRenderer(
    private val settings: Settings,
) : DeferringRenderer(settings) {

    private var isOneLine: Boolean? = null

    private fun blockRenderer() {
        if (render(BlockRenderer(settings))) isOneLine = false
    }

    private fun oneLineRenderer() {
        if (render(OneLineRenderer(settings))) isOneLine = true
    }

    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence) {
        if (name.isMultiline) blockRenderer()
        super.start(traceId, spanId, name)
    }

    override fun event(name: CharSequence, attributes: RenderableAttributes) {
        blockRenderer()
        super.event(name, attributes)
    }

    override fun exception(exception: Throwable, attributes: RenderableAttributes) {
        blockRenderer()
        super.exception(exception, attributes)
    }

    override fun <R> end(result: Result<R>) {
        if (ReturnValue.of(result).format().isMultiline) blockRenderer()
        else oneLineRenderer()
        super.end(result)
    }

    override fun childRenderer(renderer: RendererProvider): Renderer {
        lateinit var child: Renderer
        child = renderer(settings.copy(
            indent = settings.indent + settings.style(settings.layout, settings.indent).indent,
            printer = {
                if ((child as? CompactRenderer)?.isOneLine != true) blockRenderer()
                printChild(it)
            },
        )) { CompactRenderer(it) }
        return child
    }

    public companion object : RendererFactory {

        override fun create(settings: Settings): Renderer {
            return BlockRenderer(settings)
        }
    }
}
