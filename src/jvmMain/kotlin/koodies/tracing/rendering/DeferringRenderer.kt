package koodies.tracing.rendering

import koodies.asString
import koodies.tracing.SpanId
import koodies.tracing.TraceId

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
        renderer(settings.copy(printer = ::printChild)) { DeferringRenderer(it) }

    override fun printChild(text: CharSequence) {
        defer { printChild(text) }
    }

    override fun toString(): String = asString {
        ::calls to calls
        ::renderer to if (::renderer.isInitialized) renderer else "renderer not initialized"
        ::settings to settings
    }
}
