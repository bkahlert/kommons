package koodies.tracing.rendering

import io.opentelemetry.api.trace.Tracer
import koodies.asString
import koodies.exception.toCompactString
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators
import koodies.tracing.CurrentSpan
import koodies.tracing.Key.KeyValue
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
        settings.nameFormatter(name)
            ?.let { settings.oneLineStyle.start(it, settings.decorationFormatter) }
            ?.also { messages.add(it) }
    }

    override fun event(name: CharSequence, attributes: RenderableAttributes) {
        attributes[settings.layout.primaryKey]
            ?.let { settings.contentFormatter.invoke(it) }
            ?.let { settings.oneLineStyle.content(it, settings.decorationFormatter) }
            ?.also { messages.add(it) }
    }

    override fun exception(exception: Throwable, attributes: RenderableAttributes) {
        settings.contentFormatter.invoke(exception.toCompactString())
            ?.let { settings.oneLineStyle.content(it, settings.decorationFormatter)?.ansi?.red }
            ?.also { messages.add(it) }
    }

    override fun <R> end(result: Result<R>) {
        ReturnValue.of(result)
            .let { settings.oneLineStyle.end(it, settings.returnValueTransform, settings.decorationFormatter) }
            ?.also { messages.add(it) }

        messages.takeUnless { it.isEmpty() }
            ?.joinToString("") { LineSeparators.unify(it, "⏎") }
            ?.let(settings.printer)
    }

    override fun childRenderer(renderer: RendererProvider): Renderer =
        renderer(settings.copy(printer = ::printChild)) { OneLineRenderer(it) }

    override fun printChild(text: CharSequence) {
        settings.oneLineStyle.parent(text, settings.decorationFormatter)
            ?.also { messages.add(it) }
    }

    override fun toString(): String = asString {
        ::settings to settings
    }
}

/**
 * Creates a new nested span inside of the currently active span,
 * and runs [block] with this newly creates span as its [CurrentSpan] in the receiver.
 *
 * This method behaves like [spanning] with one difference:
 * Logging is rendered by a [OneLineRenderer].
 */
@TracingDsl
public fun <R> spanningLine(
    name: CharSequence,
    vararg attributes: KeyValue<*, *>,
    tracer: Tracer = koodies.tracing.Tracer,

    nameFormatter: FilteringFormatter<CharSequence>? = null,
    contentFormatter: FilteringFormatter<CharSequence>? = null,
    decorationFormatter: Formatter<CharSequence>? = null,
    returnValueTransform: ((ReturnValue) -> ReturnValue?)? = null,
    layout: ColumnsLayout? = null,
    blockStyle: ((ColumnsLayout, Int) -> BlockStyle)? = null,
    oneLineStyle: Style? = null,
    printer: Printer? = null,

    block: CurrentSpan.() -> R,
): R = spanning(
    name = name,
    attributes = attributes,
    renderer = { OneLineRenderer(this) },
    tracer = tracer,
    nameFormatter = nameFormatter,
    contentFormatter = contentFormatter,
    decorationFormatter = decorationFormatter,
    returnValueTransform = returnValueTransform,
    layout = layout,
    blockStyle = blockStyle,
    oneLineStyle = oneLineStyle,
    printer = printer,
    block = block
)
