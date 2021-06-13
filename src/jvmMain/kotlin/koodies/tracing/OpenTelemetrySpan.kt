package koodies.tracing

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import koodies.asString
import koodies.text.ANSI.ansiRemoved
import koodies.time.Now
import koodies.tracing.Span.State
import koodies.tracing.Span.State.Ended
import koodies.tracing.Span.State.Initializing
import koodies.tracing.Span.State.Started
import java.time.Instant

public class OpenTelemetrySpan(
    override val name: String,
    override val parent: OpenTelemetrySpan? = null,
) : Span {
    public constructor(name: CharSequence, parent: OpenTelemetrySpan? = null) : this(name.ansiRemoved, parent)

    private lateinit var scope: Scope
    private val span: io.opentelemetry.api.trace.Span by lazy {
        Started(Now.instant).run {
            state = this
            Tracer.spanBuilder(name.ansiRemoved)
                .setStartTimestamp(timestamp)
                .apply {
                    parent?.also { setParent(Context.current().with(it.span)) } ?: setNoParent()
                }
                .startSpan().also { scope = it.makeCurrent() }
        }
    }

    override var state: State = Initializing(Now.instant)
        private set(value) {
            value.checkLegalTransitionFrom(field)
            if (value is Ended) {
                scope.close()
                span.end(value.timestamp)
            }
            field = value
        }

    override val traceId: TraceId by lazy { TraceId(span.spanContext.traceId) }
    override val spanId: SpanId by lazy { SpanId(span.spanContext.spanId) }

    override fun event(name: String, vararg attributes: Pair<String, String>) {
        span.addEvent(name, attributesOf(*attributes))
    }

    override fun exception(exception: Throwable, vararg attributes: Pair<String, String>) {
        span.recordException(exception, attributesOf(*attributes))
    }

    override fun <R> end(result: Result<R>, timestamp: Instant) {
        if (state !is Ended) {
            state = result.fold({
                span.setStatus(OK)
                Ended.Succeeded(it, timestamp)
            }, {
                it.message?.also { message -> span.setStatus(ERROR, message) } ?: span.setStatus(ERROR)
                Ended.Failed(it, timestamp)
            })
        }
    }

    override fun toString(): String = asString(::name, ::parent, ::state)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OpenTelemetrySpan

        if (name != other.name) return false
        if (parent != other.parent) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + state.hashCode()
        return result
    }
}

@Suppress("NOTHING_TO_INLINE")
public inline fun attributesOf(vararg attributes: Pair<String, String>): Attributes =
    Attributes.builder().apply {
        attributes.forEach { (key, value) -> put(key, value) }
    }.build()

public fun Span.event(name: CharSequence, vararg attributes: Pair<CharSequence, Any>): Unit =
    event(name.ansiRemoved, *attributes.map { (key, value) ->
        key.ansiRemoved to value.toString().ansiRemoved
    }.toTypedArray())

public fun Span.event(name: CharSequence, description: CharSequence, vararg attributes: Pair<CharSequence, Any>): Unit =
    event(name.ansiRemoved, "description" to description.ansiRemoved, *attributes.map { (key, value) ->
        key.ansiRemoved to value.toString().ansiRemoved
    }.toTypedArray())
