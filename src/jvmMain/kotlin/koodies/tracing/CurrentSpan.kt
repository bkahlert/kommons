package koodies.tracing

import io.opentelemetry.api.common.Attributes
import koodies.text.ANSI.ansiRemoved

@DslMarker
public annotation class TracingDsl

/**
 * The currently active span.
 */
@TracingDsl
public interface CurrentSpan {

    /** Records an event using the given [name] and optional [attributes]. */
    public fun event(name: CharSequence, attributes: Map<CharSequence, Any> = emptyMap()): CurrentSpan

    /** Records the given [exception] using the given optional [attributes]. */
    public fun exception(exception: Throwable, attributes: Map<CharSequence, Any> = emptyMap()): CurrentSpan

    public companion object {
        public const val Description: String = "description"
    }
}

/**
 * Records an event using the given [name], [description] and optional [attributes].
 *
 * Attributes with a `null` value are removed; and together with the [name] and [description] rendered using the provided [transform].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun CurrentSpan.event(
    name: CharSequence,
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
): CurrentSpan = event(name, CurrentSpan.Description to description, *attributes)

/**
 * Records an event using the given [name] and optional [attributes].
 *
 * Attributes with a `null` value are removed; and together with the [name] rendered using the provided [transform].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun CurrentSpan.event(
    name: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
): CurrentSpan = event(name, attributes.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())

/**
 * Records an event using the given [description] and optional [attributes].
 *
 * Attributes with a `null` value are removed; and together with the [description] rendered using the provided [transform].
 *
 * ***Note:** This is a convenience method to facilitate migrating from an existing logger. The effectively required event name is derived from the description.
 * This can lead to a high cardinality (esp. if the description contains variables).
 * If too many different event names are created the value of the recorded data for later analysis is considerably reduced.
 * Consider using [event] instead.*
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun CurrentSpan.log(
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
): CurrentSpan = event("log", description, *attributes)

/**
 * Records the given [exception] using the optional [attributes].
 */
@Suppress("NOTHING_TO_INLINE")
public fun CurrentSpan.exception(
    exception: Throwable,
    vararg attributes: Pair<CharSequence, Any?>,
): CurrentSpan = exception(exception, attributes.mapNotNull { (key, value) -> value?.let { key to it } }.toMap())

@Suppress("NOTHING_TO_INLINE")
public inline fun Array<out Pair<CharSequence, Any?>>.toAttributes(): Attributes =
    Attributes.builder().apply {
        forEach { (key, value) -> value?.let { put(key.ansiRemoved, it.toString()) } }
    }.build()

@Suppress("NOTHING_TO_INLINE")
public inline fun Map<out CharSequence, Any>.toAttributes(): Attributes =
    Attributes.builder().apply {
        entries.forEach { (key, value) -> put(key.ansiRemoved, value.toString().ansiRemoved) }
    }.build()
