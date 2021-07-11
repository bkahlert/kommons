package koodies.tracing.rendering

import io.opentelemetry.api.trace.Span
import koodies.toBaseName
import koodies.tracing.Key.KeyValue
import koodies.tracing.SpanId
import koodies.tracing.TraceId

/**
 * Component to render events of a [Span].
 */
public interface Renderer {

    /**
     * Renders the start of a span.
     */
    public fun start(traceId: TraceId, spanId: SpanId, name: CharSequence)

    /**
     * Renders an event using the given [name] and optional [attributes].
     */
    public fun event(name: CharSequence, attributes: RenderableAttributes)

    /**
     * Renders the given [exception] using the optional [attributes].
     */
    public fun exception(exception: Throwable, attributes: RenderableAttributes)

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
    public fun childRenderer(renderer: RendererProvider = { default -> default(this) }): Renderer

    /**
     * Renders the output of a nested renderer.
     */
    public fun printChild(text: CharSequence)

    public companion object {

        /**
         * Renders an event using the given [name], [description] and optional [attributes].
         *
         * Attributes with a `null` value are removed.
         */
        public fun Renderer.event(
            name: CharSequence,
            description: CharSequence,
            vararg attributes: KeyValue<*, *>,
        ): Unit = event(name, RenderableAttributes.of(RenderingAttributes.DESCRIPTION to description, *attributes))

        /**
         * Renders an event using the given [description] and optional [attributes].
         *
         * Attributes with a `null` value are removed.
         *
         * ***Note:** This is a convenience method to facilitate migrating from an existing logger. The effectively required event name is derived from the description.
         * This can lead to a high cardinality (esp. if the description contains variables).
         * If too many different event names are created the value of the recorded data for later analysis is considerably reduced.
         * Consider using [event] instead.*
         */
        public fun Renderer.log(
            description: CharSequence,
            vararg attributes: KeyValue<*, *>,
        ): Unit = event(description.toBaseName(), description, *attributes)

        public object NOOP : Renderer {
            override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence): Unit = Unit
            override fun event(name: CharSequence, attributes: RenderableAttributes): Unit = Unit
            override fun exception(exception: Throwable, attributes: RenderableAttributes): Unit = Unit
            override fun <R> end(result: Result<R>): Unit = Unit

            override fun childRenderer(renderer: RendererProvider): Renderer = this
            override fun printChild(text: CharSequence): Unit = Unit

            override fun toString(): String = "NOOP"
        }
    }
}
