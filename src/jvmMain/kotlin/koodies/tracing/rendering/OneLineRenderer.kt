package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Tracer
import koodies.asString
import koodies.exception.toCompactString
import koodies.logging.ReturnValue
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators
import koodies.tracing.CurrentSpan
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import koodies.tracing.TracingDsl
import koodies.tracing.spanning

/**
 * Renderer that renders event their primary attribute
 * as specified in [Settings.layout] in a single line.
 */
public class OneLineRenderer(
    private val settings: Settings,
) : Renderer {

    private val messages = mutableListOf<CharSequence>()

    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence) {
        name.let { settings.oneLineStyle.start(it, settings.decorationFormatter) }
            ?.also { messages.add(it) }
    }

    override fun event(name: CharSequence, attributes: Attributes) {
        attributes[settings.layout.primaryAttributeKey]
            ?.let { settings.contentFormatter.invoke(it) }
            ?.let { settings.oneLineStyle.content(it, settings.decorationFormatter) }
            ?.also { messages.add(it) }
    }

    override fun exception(exception: Throwable, attributes: Attributes) {
        settings.contentFormatter.invoke(exception.toCompactString())
            ?.let { settings.oneLineStyle.content(it, settings.decorationFormatter)?.ansi?.red }
            ?.also { messages.add(it) }
    }

    override fun <R> end(result: Result<R>) {
        ReturnValue.of(result)
            .let { settings.oneLineStyle.end(it, settings.returnValueFormatter, settings.decorationFormatter) }
            ?.also { messages.add(it) }

        messages.takeUnless { it.isEmpty() }
            ?.joinToString("") { LineSeparators.unify(it, "⏎") }
            ?.let(settings.printer)
    }

    override fun nestedRenderer(renderer: RendererProvider): Renderer =
        renderer(settings.copy(printer = ::printChild)) { OneLineRenderer(it) }

    override fun printChild(text: CharSequence) {
        settings.oneLineStyle.parent(text, settings.decorationFormatter)
            ?.also { messages.add(it) }
    }

    override fun toString(): String = asString {
        ::settings to settings
    }
}

@TracingDsl
public fun <R> spanningLine(
    name: CharSequence,
    customize: Settings.() -> Settings = { this },
    tracer: Tracer = koodies.tracing.Tracer,
    block: CurrentSpan.() -> R,
): R = spanning(name, renderer = { OneLineRenderer(customize()) }, tracer, block)
