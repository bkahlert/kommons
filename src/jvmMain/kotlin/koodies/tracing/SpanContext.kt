package koodies.tracing

import koodies.time.Now
import koodies.toBaseName
import java.time.Instant

@DslMarker
public annotation class TracingDsl

@TracingDsl
public interface SpanContext {

    /** Records an event using the given [name], optional [attributes] and [timestamp] (default: now). */
    public fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence> = emptyMap(), timestamp: Instant = Now.instant)

    /** Records the given [exception] using the given [name], optional [attributes] and [timestamp] (default: now). */
    public fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence> = emptyMap(), timestamp: Instant = Now.instant)
}


/**
 * Records an event using the given [name], [description], optional [attributes] and [timestamp] (default: now).
 *
 * Attributes with a `null` value are removed; and together with the [name] and [description] rendered using the provided [transform].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun SpanContext.event(
    name: CharSequence,
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: (Any) -> CharSequence = { (it as? CharSequence) ?: it.toString() },
): Unit = event(name, Span.Description to description, *attributes, timestamp = timestamp, transform = transform)

/**
 * Records an event using the given [name], optional [attributes] and [timestamp] (default: now).
 *
 * Attributes with a `null` value are removed; and together with the [name] rendered using the provided [transform].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun SpanContext.event(
    name: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: (Any) -> CharSequence = { (it as? CharSequence) ?: it.toString() },
): Unit = event(name, attributes.mapNotNull { (key, value) -> value?.let { key to transform(it) } }.toMap(), timestamp = timestamp)

/**
 * Records an event using the given [description], optional [attributes] and [timestamp] (default: now).
 *
 * Attributes with a `null` value are removed; and together with the [description] rendered using the provided [transform].
 *
 * ***Note:** This is a convenience method to facilitate migrating from an existing logger. The effectively required event name is derived from the description.
 * This can lead to a high cardinality (esp. if the description contains variables).
 * If too many different event names are created the value of the recorded data for later analysis is considerably reduced.
 * Consider using [event] instead.*
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun SpanContext.log(
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: (Any) -> CharSequence = { (it as? CharSequence) ?: it.toString() },
): Unit = event(description.toBaseName(), description, *attributes, timestamp = timestamp, transform = transform)

/**
 * Records the given [exception] using the optional [attributes].
 */
@Suppress("NOTHING_TO_INLINE")
public fun SpanContext.exception(
    exception: Throwable,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: (Any) -> CharSequence = { (it as? CharSequence) ?: it.toString() },
): Unit = exception(exception, attributes.mapNotNull { (key, value) -> value?.let { key to transform(it) } }.toMap(), timestamp = timestamp)
