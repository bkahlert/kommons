package koodies.tracing.rendering

import koodies.time.Now
import koodies.toBaseName
import koodies.tracing.Span
import koodies.tracing.Span.State.Ended
import koodies.tracing.SpanId
import koodies.tracing.TraceId
import java.time.Instant

/**
 * Component to render events of a [Span].
 */
public interface Renderer {

    public fun start(traceId: TraceId, spanId: SpanId, timestamp: Instant = Now.instant)
    public fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence> = emptyMap(), timestamp: Instant = Now.instant)
    public fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence> = emptyMap(), timestamp: Instant = Now.instant)
    public fun end(ended: Ended)

    public fun nestedRenderer(name: CharSequence, customize: Settings.() -> Settings = { this }): Renderer
    public fun nestedRenderer(provider: (Settings, Printer) -> Renderer): Renderer
    public val muted: Renderer get() = noop()

    public companion object {

        public fun noop(): Renderer = object : Renderer {
            override fun start(traceId: TraceId, spanId: SpanId, timestamp: Instant): Unit = Unit
            override fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence>, timestamp: Instant): Unit = Unit
            override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>, timestamp: Instant): Unit = Unit
            override fun end(ended: Ended): Unit = Unit

            override fun nestedRenderer(name: CharSequence, customize: Settings.() -> Settings): Renderer = noop()
            override fun nestedRenderer(provider: (Settings, Printer) -> Renderer): Renderer = noop()
        }
    }
}

/**
 * Renders an event using the given [name], [description] and optional [attributes].
 *
 * Attributes with a `null` value are removed and rendered using the provided [transform],
 * that calls [CharSequence.toString] by default.
 */
public inline fun Renderer.event(
    name: CharSequence,
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: (Any) -> CharSequence = { (it as? CharSequence) ?: it.toString() },
): Unit = event(
    name,
    listOf(Span.Description to description, *attributes).mapNotNull { (key, value) -> value?.let { key to transform(it) } }.toMap(),
    timestamp,
)

/**
 * Renders an event using the given [description], optional [attributes] and [timestamp] (default: now).
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
    timestamp: Instant = Now.instant,
    transform: (Any) -> CharSequence = { (it as? CharSequence) ?: it.toString() },
): Unit = event(description.toBaseName(), description, *attributes, timestamp = timestamp, transform = transform)

/**
 * Renders the given [exception] using the optional [attributes].
 */
public inline fun Renderer.exception(
    exception: Throwable,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: (Any) -> CharSequence = { (it as? CharSequence) ?: it.toString() },
): Unit = exception(exception, attributes.mapNotNull { (key, value) -> value?.let { key to transform(it) } }.toMap(), timestamp = timestamp)
