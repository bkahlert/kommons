package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.tracing.SpanId
import com.bkahlert.kommons.tracing.TraceId

/**
 * Renderer that defers all invocations until the
 * moment the actual renderer is chosen using [render].
 */
public open class DeferringRenderer(
    private val settings: Settings,
) : Renderer {

    private val calls = mutableListOf<Renderer.() -> Unit>()

    private lateinit var renderer: Renderer

    private fun defer(call: Renderer.() -> Unit) {
        if (::renderer.isInitialized) renderer.call()
        else calls.add(call)
    }

    public fun render(renderer: Renderer): Boolean {
        if (this::renderer.isInitialized) return false
        this.renderer = renderer
        calls.forEach { call -> renderer.call() }
        return true
    }

    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence): Unit =
        defer { start(traceId, spanId, name) }

    override fun event(name: CharSequence, attributes: RenderableAttributes) {
        defer { event(name, attributes) }
    }

    override fun exception(exception: Throwable, attributes: RenderableAttributes) {
        defer { exception(exception, attributes) }
    }

    override fun <R> end(result: Result<R>) {
        defer { end(result) }
    }

    override fun childRenderer(renderer: RendererProvider): Renderer =
        renderer(
            settings.copy(
                indent = settings.indent + settings.style(settings.layout, settings.indent).indent,
                printer = ::printChild,
            )
        ) {
            DeferringRenderer(it)
        }

    override fun printChild(text: CharSequence) {
        defer { printChild(text) }
    }

    override fun toString(): String = asString {
        put(::calls, calls)
        put(::renderer, if (::renderer.isInitialized) renderer else "renderer not initialized")
        put(::settings, settings)
    }

    public companion object : RendererFactory {

        override fun create(settings: Settings): Renderer {
            return BlockRenderer(settings)
        }
    }
}
