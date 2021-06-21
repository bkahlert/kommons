package koodies.tracing.rendering

import io.opentelemetry.api.common.Attributes
import koodies.toBaseName
import koodies.tracing.CurrentSpan
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import koodies.tracing.toAttributes

/**
 * Component to render events of a [Span].
 */
public interface Renderer {

    public fun start(traceId: TraceId, spanId: SpanId, name: CharSequence)
    public fun event(name: CharSequence, attributes: Attributes = Attributes.empty())
    public fun exception(exception: Throwable, attributes: Attributes = Attributes.empty())
    public fun <R> end(result: Result<R>)

    public fun customizedChild(customize: Settings.() -> Settings = { this }): Renderer
    public fun injectedChild(provider: (Settings, Printer) -> Renderer): Renderer
    public fun printChild(text: CharSequence)
    public val muted: Renderer get() = noop()

    public companion object {

        public fun noop(): Renderer = object : Renderer {
            override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence): Unit = Unit
            override fun event(name: CharSequence, attributes: Attributes): Unit = Unit
            override fun exception(exception: Throwable, attributes: Attributes): Unit = Unit
            override fun <R> end(result: Result<R>): Unit = Unit

            override fun customizedChild(customize: Settings.() -> Settings): Renderer = noop()
            override fun injectedChild(provider: (Settings, Printer) -> Renderer): Renderer = noop()
            override fun printChild(text: CharSequence): Unit = Unit

            override fun toString(): String = "NOOP"
        }
    }
}

/**
 * Renders an event using the given [name], [description] and optional [attributes].
 *
 * Attributes with a `null` value are removed and rendered using the provided [transform],
 * that calls [CharSequence.toString] by default.
 */
public fun Renderer.event(
    name: CharSequence,
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
): Unit = event(
    name,
    arrayOf(CurrentSpan.Description to description, *attributes).toAttributes(),
)

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
public inline fun Renderer.log(
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
): Unit = event(description.toBaseName(), description, *attributes)

/**
 * Renders the given [exception] using the optional [attributes].
 */
public fun Renderer.exception(
    exception: Throwable,
    vararg attributes: Pair<CharSequence, Any?>,
): Unit = exception(exception, attributes.toAttributes())
