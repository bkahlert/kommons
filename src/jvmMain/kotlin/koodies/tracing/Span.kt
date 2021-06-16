package koodies.tracing

import koodies.time.Now
import koodies.toBaseName
import koodies.tracing.Span.State
import java.time.Instant
import kotlin.reflect.KClass

/**
 * A span as used for tracing.
 */
public interface Span {

    /** Name of this span. */
    public val name: CharSequence

    /** Parent of this span. */
    public val parent: Span?

    /** State of this span. */
    public val state: State

    /** Trace ID of this span. */
    public val traceId: TraceId?

    /** ID of this span. */
    public val spanId: SpanId?

    /** Starts this span with the given [timestamp] (default: now). */
    public fun start(timestamp: Instant = Now.instant)

    /** Records an event using the given [name], optional [attributes] and [timestamp] (default: now). */
    public fun event(name: CharSequence, vararg attributes: Pair<CharSequence, CharSequence>, timestamp: Instant = Now.instant)

    /** Records the given [exception] using the given [name], optional [attributes] and [timestamp] (default: now). */
    public fun exception(exception: Throwable, vararg attributes: Pair<CharSequence, CharSequence>, timestamp: Instant = Now.instant)

    /** Run the given [block] in a child [Span] using the given [name] using [timestamp] (default: now). */
    public fun <R> spanning(name: CharSequence, timestamp: Instant = Now.instant, block: Span.() -> R): R

    /** Ends this span with the given [result] and [timestamp] (default: now). */
    public fun <R> end(result: Result<R>, timestamp: Instant = Now.instant): TraceId

    /**
     * State a [Span] can have.
     */
    public sealed class State(private vararg val allowedPreviousStates: KClass<out State>) {

        /**
         * Moment in time this state activated.
         */
        public abstract val timestamp: Instant

        public fun checkLegalTransitionFrom(previousState: State) {
            if (allowedPreviousStates.none { it == previousState::class }) {
                error("Illegal transition from $previousState to $this")
            }
        }

        /**
         * Initial state that does not record any events yet.
         */
        public data class Initialized(override val timestamp: Instant) : State()

        /**
         * A started span is recording itself and all events that take place until
         * the state changes to [Ended].
         */
        public data class Started(override val timestamp: Instant) : State(Initialized::class)

        /**
         * Final state in which the final processing of recorded data takes place.
         */
        public sealed class Ended : State(Started::class) {

            /**
             * State of a successfully ended span.
             */
            public data class Succeeded(val value: Any?, override val timestamp: Instant) : Ended()

            /**
             * State of a span that failed with an exception.
             */
            public data class Failed(val exception: Throwable, override val timestamp: Instant) : Ended()
        }
    }

    public companion object AttributeKeys {
        public const val Description: String = "description"
    }
}

/** Whether this span is at the top of the hierarchy. */
public val Span.isRoot: Boolean get() = parent == null

/** Whether this state has started. */
public val Span.started: Boolean get() = state is State.Started

/** Whether this state has ended. */
public val Span.ended: Boolean get() = state is State.Ended

/** Whether this state has succeeded. */
public val Span.succeeded: Boolean get() = state is State.Ended.Succeeded

/** Whether this state has failed. */
public val Span.failed: Boolean get() = state is State.Ended.Failed

/**
 * Records an event using the given [name], optional [attributes] and [timestamp] (default: now).
 *
 * Attributes with a `null` value are removed; and together with the [name] rendered using the provided [transform].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Span> T.event(
    name: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: Any.() -> CharSequence = { toString() },
): Unit =
    event(name.transform(), *attributes.mapNotNull { (key, value) ->
        value?.let { key.transform() to it.transform() }
    }.toTypedArray(), timestamp = timestamp)

/**
 * Records an event using the given [name], [description], optional [attributes] and [timestamp] (default: now).
 *
 * Attributes with a `null` value are removed; and together with the [name] and [description] rendered using the provided [transform].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Span> T.event(
    name: CharSequence,
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: Any.() -> CharSequence = { toString() },
): Unit = event(name, Span.Description to description.transform(), *attributes, timestamp = timestamp, transform = transform)

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
public inline fun <T : Span> T.log(
    description: CharSequence,
    vararg attributes: Pair<CharSequence, Any?>,
    timestamp: Instant = Now.instant,
    transform: Any.() -> CharSequence = { toString() },
): Unit = event(description.toBaseName(), description, *attributes, timestamp = timestamp, transform = transform)

/** Ends this span with the given [value] and [timestamp] (default: now). */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Span, reified R> T.end(value: R, timestamp: Instant = Now.instant): TraceId =
    end(Result.success(value), timestamp)

/** Ends this span with no value, thus [Unit], and the given [timestamp] (default: now). */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Span> T.end(timestamp: Instant = Now.instant): TraceId =
    end(Result.success(Unit), timestamp)

/** Run the given [block] as specified by [runCatching] but as a side effect also records an eventually thrown exception. */
public inline fun <T : Span, R> T.runExceptionRecording(block: () -> R): Result<R> =
    runCatching(block).onFailure { exception(it) }
