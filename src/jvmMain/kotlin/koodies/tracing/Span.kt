package koodies.tracing

import koodies.time.Now
import koodies.tracing.Span.State
import java.time.Instant
import kotlin.reflect.KClass

/**
 * A span as used for tracing.
 */
public interface Span : SpanContext {

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

    public companion object {
        public const val Description: String = "description"

        public inline val <reified V> Map<CharSequence, V?>.description: V? get() = get(Description)
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

/** Ends this span with the given [value] and [timestamp] (default: now). */
@Suppress("NOTHING_TO_INLINE")
public inline fun <reified R> Span.end(value: R, timestamp: Instant = Now.instant): TraceId =
    end(Result.success(value), timestamp)

/** Ends this span with the given [exception] and [timestamp] (default: now). */
@Suppress("NOTHING_TO_INLINE")
public inline fun Span.end(exception: Throwable?, timestamp: Instant = Now.instant): TraceId =
    exception?.let { end<Unit>(Result.failure(it), timestamp) } ?: end(Result.success(Unit), timestamp)

/** Ends this span with no value, thus [Unit], and the given [timestamp] (default: now). */
@Suppress("NOTHING_TO_INLINE")
public inline fun Span.end(timestamp: Instant = Now.instant): TraceId =
    end(Result.success(Unit), timestamp)


/**
 * Runs the given [block] using this span.
 */
public inline fun <R> Span.run(timestamp: Instant, block: SpanContext.() -> R): R {
    start(timestamp)
    val result = runCatching(block)
    end(result)
    return result.getOrThrow()
}
