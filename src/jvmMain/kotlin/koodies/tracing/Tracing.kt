package koodies.tracing

import koodies.time.Now
import koodies.tracing.Span.State
import java.time.Instant
import kotlin.reflect.KClass

@JvmInline
public value class TraceId(public val value: String) {
    public val valid: Boolean get() = value.any { it != '0' }
    override fun toString(): String = value
}

@JvmInline
public value class SpanId(public val value: String) {
    public val valid: Boolean get() = value.any { it != '0' }
    override fun toString(): String = value
}

public interface Span {

    public val name: CharSequence
    public val parent: Span?
    public val state: State

    public val traceId: TraceId
    public val spanId: SpanId

    public fun event(name: CharSequence, vararg attributes: Pair<CharSequence, Any?>)
    public fun exception(exception: Throwable, vararg attributes: Pair<CharSequence, Any?>)
    public fun <R> end(result: Result<R>, timestamp: Instant = Now.instant)

    public sealed class State(private vararg val allowedPreviousStates: KClass<out State>) {
        public abstract val timestamp: Instant

        public fun checkLegalTransitionFrom(previousState: State) {
            if (allowedPreviousStates.none { it == previousState::class }) {
                error("Illegal transition from $previousState to $this")
            }
        }

        public data class Initializing(override val timestamp: Instant) : State()
        public data class Started(override val timestamp: Instant) : State(Initializing::class)
        public sealed class Ended : State(Initializing::class, Started::class) {
            public data class Succeeded(val value: Any?, override val timestamp: Instant) : Ended()
            public data class Failed(val exception: Throwable, override val timestamp: Instant) : Ended()
        }
    }
}

public val Span.isRoot: Boolean get() = parent == null
public val Span.ended: Boolean get() = state is State.Ended
public val Span.succeeded: Boolean get() = state is State.Ended.Succeeded
public val Span.failed: Boolean get() = state is State.Ended.Failed

@Suppress("NOTHING_TO_INLINE")
public inline fun <T : Span> T.end(timestamp: Instant = Now.instant): Unit =
    end(Result.success(Unit), timestamp)

public inline fun <T : Span, R> T.runExceptionRecording(block: () -> R): R =
    runCatching(block).onFailure { exception(it) }.getOrThrow()
