package koodies.tracing.rendering

import koodies.asString
import koodies.tracing.Span.State.Ended
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import java.time.Instant

/**
 * Renderer that aims to log concisely by using a [OneLineRenderer]
 * if no [event] are rendered (so only [start] and [end] are called),
 * and falling back to a [BlockRenderer] if events are logged as well.
 */
public class CompactRenderer(
    private val name: CharSequence,
    private val settings: Settings,
    private val printer: Printer,
) : Renderer {

    private val calls = mutableListOf<Renderer.() -> Unit>()

    private var rendererPicked = false
    private fun record(call: Renderer.() -> Unit) {
        if (rendererPicked) renderer.call()
        else calls.add(call)
    }

    private var endCalled = false
    private val renderer: Renderer by lazy {
        rendererPicked = true
        (if (calls.size <= 1 && endCalled) OneLineRenderer(name, settings, printer) else BlockRenderer(name, settings, printer))
            .also { actual -> calls.forEach { call -> actual.call() } }
    }

    override fun start(traceId: TraceId, spanId: SpanId, timestamp: Instant): Unit =
        record { start(traceId, spanId, timestamp) }

    override fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        renderer.event(name, attributes, timestamp)
    }

    override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        renderer.exception(exception, attributes, timestamp)
    }

    override fun end(ended: Ended) {
        endCalled = true
        renderer.end(ended)
    }

    override fun nestedRenderer(name: CharSequence, customize: Settings.() -> Settings): Renderer =
        nestedRenderer { settings, printer -> CompactRenderer(name, settings.customize(), printer) }

    override fun nestedRenderer(provider: (Settings, Printer) -> Renderer): Renderer =
        renderer.nestedRenderer(provider)

    override fun toString(): String = asString {
        ::name to name
        ::rendererPicked to rendererPicked
        ::endCalled to endCalled
        ::settings to settings
        ::printer to printer
    }
}
