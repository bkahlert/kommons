package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
import koodies.asString
import koodies.tracing.SpanId
import koodies.tracing.TraceId

/**
 * Renderer that behaves like a [BlockRenderer] with two exceptions:
 * - If [end] is called with no events in between, the result is logged with a [OneLineRenderer].
 * - If [end] is called with no events but nested compact renderers with no results, the result is logged with a [OneLineRenderer].
 *
 * The goal of this renderer is to not waste space for immediately returning spans.
 */
public class CompactRenderer(
    private val settings: Settings,
    private val printer: Printer,
) : DeferringRenderer(settings, printer) {

    private var isOneLine: Boolean? = null

    private fun blockRenderer() {
        if (render(BlockRenderer(settings, printer))) isOneLine = false
    }

    private fun oneLineRenderer() {
        if (render(OneLineRenderer(settings, printer))) isOneLine = true
    }

    override fun event(name: CharSequence, attributes: Attributes) {
        blockRenderer()
        super.event(name, attributes)
    }

    override fun exception(exception: Throwable, attributes: Attributes) {
        blockRenderer()
        super.exception(exception, attributes)
    }

    override fun <R> end(result: Result<R>) {
        oneLineRenderer()
        super.end(result)
    }

    override fun customizedChild(customize: Settings.() -> Settings): Renderer {
        lateinit var child: CompactRenderer
        child = CompactRenderer(settings.customize()) {
            if (child.isOneLine != true) blockRenderer()
            printChild(it)
        }
        return child
    }

    override fun injectedChild(provider: (Settings, Printer) -> Renderer): Renderer {
        lateinit var child: Renderer
        child = provider(settings) {
            if ((child as? CompactRenderer)?.isOneLine != true) blockRenderer()
            printChild(it)
        }
        return child
    }
}

/**
 * Renderer that defers all invocations until the
 * moment the actual renderer is chosen using [render].
 */
public open class DeferringRenderer(
    private val settings: Settings,
    private val printer: Printer,
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

    override fun event(name: CharSequence, attributes: Attributes) {
        defer { event(name, attributes) }
    }

    override fun exception(exception: Throwable, attributes: Attributes) {
        defer { exception(exception, attributes) }
    }

    override fun <R> end(result: Result<R>) {
        defer { end(result) }
    }

    override fun customizedChild(customize: Settings.() -> Settings): Renderer = DeferringRenderer(settings.customize()) {
        printChild(it)
    }

    override fun injectedChild(provider: (Settings, Printer) -> Renderer): Renderer = provider(settings) {
        printChild(it)
    }

    override fun printChild(text: CharSequence) {
        defer { printChild(text) }
    }

    override fun toString(): String = asString {
        ::calls to calls
        ::renderer to if (::renderer.isInitialized) renderer else "renderer not initialized"
        ::settings to settings
        ::printer to printer
    }
}
