package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import koodies.toBaseName
import koodies.tracing.KoodiesAttributes
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import koodies.tracing.toRenderedAttributes

/**
 * Component to render events of a [Span].
 */
public interface Renderer {

    /**
     * Renders the start of a span.
     */
    public fun start(traceId: TraceId, spanId: SpanId, name: Renderable)

    /**
     * Renders an event using the given [name] and optional [attributes].
     */
    public fun event(name: CharSequence, attributes: Attributes = Attributes.empty())

    /**
     * Renders an event using the given [name], [description] and optional [attributes].
     *
     * Attributes with a `null` value are removed and rendered using the provided [transform],
     * that calls [CharSequence.toString] by default.
     */
    public fun event(
        name: CharSequence,
        description: CharSequence,
        vararg attributes: Pair<CharSequence, Any?>,
    ): Unit = event(name, arrayOf(KoodiesAttributes.DESCRIPTION.key to description, *attributes).toRenderedAttributes())

    /**
     * Renders an event using the given [description] and optional [attributes].
     *
     * Attributes with a `null` value are removed; and together with the [description] rendered using the provided [transform].
     *
     * ***Note:** This is a convenience method to facilitate migrating from an existing logger. The effectively required event name is derived from the description.
     * This can lead to a high cardinality (esp. if the description contains variables).
     * If too many different event names are created the value of the recorded data for later analysis is considerably reduced.
     * Consider using [event] instead.*
     */
    public fun log(
        description: CharSequence,
        vararg attributes: Pair<CharSequence, Any?>,
    ): Unit = event(description.toBaseName(), description, *attributes)

    /**
     * Renders the given [exception] using the optional [attributes].
     */
    public fun exception(exception: Throwable, attributes: Attributes = Attributes.empty())

    /**
     * Renders the given [exception] using the optional [attributes].
     */
    public fun Renderer.exception(
        exception: Throwable,
        vararg attributes: Pair<CharSequence, Any?>,
    ): Unit = exception(exception, attributes.toRenderedAttributes())

    /**
     * Renders the end of a span using the given [result].
     */
    public fun <R> end(result: Result<R>)

    /**
     * Creates a new nested renderer using the given [renderer].
     *
     * The current [Settings] are provided in the receiver and a default provider
     * as the only argument. Using the default provider will create a nested renderer of
     * the same type. Ignoring the default provider allows mixing different renderers.
     */
    public fun nestedRenderer(renderer: RendererProvider = { default -> default(this) }): Renderer

    /**
     * Renders the output of a nested renderer.
     */
    public fun printChild(text: CharSequence)

    public companion object {

        public object NOOP : Renderer {
            override fun start(traceId: TraceId, spanId: SpanId, name: Renderable): Unit = Unit
            override fun event(name: CharSequence, attributes: Attributes): Unit = Unit
            override fun exception(exception: Throwable, attributes: Attributes): Unit = Unit
            override fun <R> end(result: Result<R>): Unit = Unit

            override fun nestedRenderer(renderer: RendererProvider): Renderer = this
            override fun printChild(text: CharSequence): Unit = Unit

            override fun toString(): String = "NOOP"
        }
    }
}
