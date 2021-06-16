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
import koodies.tracing.Span.State.Ended.Failed
import koodies.tracing.Span.State.Ended.Succeeded
import koodies.tracing.Span.State.Initialized
import koodies.tracing.Span.State.Started
import koodies.tracing.rendering.Renderer
import java.time.Instant

/**
 * A span that is implemented using [OpenTelemetrySpan] with the additional feature of
 * rendering all recorded data. Only events with no provided description are not rendered.
 */
public class OpenTelemetrySpan(
    override val name: CharSequence,
    override val parent: OpenTelemetrySpan? = null,
    private val renderer: Renderer = Renderer.NOOP,
) : Span {

    private var span: io.opentelemetry.api.trace.Span? = null
    private var scope: Scope? = null

    override var state: State = Initialized(Now.instant)
        private set(value) {
            value.checkLegalTransitionFrom(field)
            when (value) {
                is Initialized -> error("Already initialized")

                is Started -> {
                    field = value
                    parent?.start(value.timestamp)
                    Tracer.spanBuilder(name.ansiRemoved)
                        .setStartTimestamp(value.timestamp)
                        .apply {
                            parent?.also {
                                val parentSpan = parent.span
                                checkNotNull(parentSpan) { "Parent has no span: $parent" }
                                setParent(Context.current().with(parentSpan))
                            } ?: setNoParent()
                        }
                        .startSpan()
                        .also { span = it }
                        .also { scope = it.makeCurrent() }
                        .also { renderer.start(name, value) }
                }

                is Ended -> {
                    field = value
                    val span = checkNotNull(span)
                    val scope = checkNotNull(scope)

                    scope.close()
                    when (value) {
                        is Succeeded -> span.setStatus(OK)
                        is Failed -> value.exception.message?.also { message -> span.setStatus(ERROR, message) } ?: span.setStatus(ERROR)
                    }
                    span.end(value.timestamp)
                }
            }
        }

    override val traceId: TraceId? get() = span?.spanContext?.traceId?.let { TraceId(it) }
    override val spanId: SpanId? get() = span?.spanContext?.spanId?.let { SpanId(it) }

    override fun start(timestamp: Instant) {
        if (state is Initialized) state = Started(timestamp)
    }

    override fun event(name: CharSequence, vararg attributes: Pair<CharSequence, CharSequence>, timestamp: Instant) {
        start(timestamp)
        checkNotNull(span).addEvent(name.ansiRemoved, attributesOf(*attributes), timestamp)
        attributes.firstOrNull { it.first == Span.Description }?.also { description ->
            renderer.event(name, description.second, attributes.filter { it.first != Span.Description }.toMap())
        }
    }

    override fun exception(exception: Throwable, vararg attributes: Pair<CharSequence, CharSequence>, timestamp: Instant) {
        start(timestamp)
        checkNotNull(span).recordException(exception, attributesOf(*attributes))
        renderer.exception(exception, attributes.toMap())
    }

    override fun <R> spanning(name: CharSequence, timestamp: Instant, block: Span.() -> R): R {
        val childSpan = OpenTelemetrySpan(name, this, renderer.spanning(name))
        childSpan.start(timestamp)
        val result = childSpan.runCatching(block)
        childSpan.end(result)
        return result.getOrThrow()
    }

    override fun <R> end(result: Result<R>, timestamp: Instant): TraceId {
        start(timestamp)
        if (state !is Ended) {
            state = result.fold({
                Succeeded(it, timestamp)
            }, {
                Failed(it, timestamp)
            }).also { renderer.end(it) }
        }
        return checkNotNull(traceId)
    }

    override fun toString(): String = asString(::name, ::parent, ::state, ::traceId, ::spanId)

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
public inline fun attributesOf(vararg attributes: Pair<CharSequence, CharSequence>): Attributes =
    Attributes.builder().apply {
        attributes.forEach { (key, value) -> put(key.ansiRemoved, value.ansiRemoved) }
    }.build()
