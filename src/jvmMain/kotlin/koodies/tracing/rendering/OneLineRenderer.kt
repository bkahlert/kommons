package koodies.tracing.rendering

import io.opentelemetry.api.trace.Tracer
import koodies.asString
import koodies.exception.toCompactString
import koodies.logging.ReturnValue
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators
import koodies.time.Now
import koodies.tracing.Span.State.Ended
import koodies.tracing.SpanContext
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import koodies.tracing.TracingDsl
import koodies.tracing.spanning
import java.time.Instant

/**
 * Renderer that renders event their primary attribute
 * as specified in [Settings.layout] in a single line.
 */
public class OneLineRenderer(
    private val name: CharSequence,
    private val settings: Settings,
    private val printer: Printer,
) : Renderer {

    private val messages = mutableListOf<CharSequence>()

    override fun start(traceId: TraceId, spanId: SpanId, timestamp: Instant) {
        settings.contentFormatter(name)
            ?.let { settings.oneLineStyle.start(it, settings.decorationFormatter) }
            ?.also { messages.add(it) }
    }

    override fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        attributes[settings.layout.primaryAttributeKey]
            ?.let { settings.contentFormatter(it) }
            ?.let { settings.oneLineStyle.content(it, settings.decorationFormatter) }
            ?.also { messages.add(it) }
    }

    override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        settings.contentFormatter(exception.toCompactString())
            ?.let { settings.oneLineStyle.content(it, settings.decorationFormatter)?.ansi?.red }
            ?.also { messages.add(it) }
    }

    override fun end(ended: Ended) {
        ReturnValue.of(ended)
            .let { settings.oneLineStyle.end(it, settings.returnValueFormatter, settings.decorationFormatter) }
            ?.also { messages.add(it) }

        messages.takeUnless { it.isEmpty() }
            ?.joinToString("") { LineSeparators.unify(it, "⏎") }
            ?.let(printer)
    }

    override fun nestedRenderer(name: CharSequence, customize: Settings.() -> Settings): Renderer =
        nestedRenderer { settings, printer -> OneLineRenderer(name, settings.customize(), printer) }

    override fun nestedRenderer(provider: (Settings, Printer) -> Renderer): Renderer =
        provider(settings) {
            settings.oneLineStyle.parent(it, settings.decorationFormatter)
                ?.also { messages.add(it) }
        }

    override fun toString(): String = asString {
        ::name to name
        ::settings to settings
        ::printer to printer
    }

    public companion object {
        private fun CharSequence.replaceLineBreaks() {
            LineSeparators.unify(this, "⏎")
        }
    }
}

@TracingDsl
public fun <R> spanningLine(
    name: CharSequence,
    customize: Settings.() -> Settings = { this },
    timestamp: Instant = Now.instant,
    tracer: Tracer = koodies.tracing.Tracer,
    block: SpanContext.() -> R,
): R = spanning(name, { settings, printer -> OneLineRenderer(name, customize(settings), printer) }, timestamp, tracer, block)
