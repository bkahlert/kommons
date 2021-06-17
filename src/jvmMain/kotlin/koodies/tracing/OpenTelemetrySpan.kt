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
import koodies.tracing.rendering.CompactRenderer
import koodies.tracing.rendering.Printer
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.Settings
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import io.opentelemetry.api.trace.Tracer as TracerAPI

/**
 * A span that is implemented using [OpenTelemetrySpan] with the additional feature of
 * rendering all recorded data. Only events with no provided description are not rendered.
 */
public class OpenTelemetrySpan private constructor(
    override val name: CharSequence,
    override val parent: OpenTelemetrySpan?,
    private val renderer: Renderer,
    private val tracer: TracerAPI,
) : Span {

    /**
     * Creates a new root span with optional [renderer] (default: [Renderer.noop]).
     */
    public constructor(
        name: CharSequence,
        tracer: TracerAPI = Tracer,
        renderer: Renderer? = null,
    ) : this(name, null, renderer ?: Renderer.noop(), tracer)

    /**
     * Creates a new child span with optional renderer (default: [Renderer.nestedRenderer].
     */
    public constructor(
        name: CharSequence,
        parent: OpenTelemetrySpan,
        renderer: Renderer? = null,
    ) : this(name, parent, renderer ?: parent.renderer.nestedRenderer(name), parent.tracer)

    /**
     * Creates a new child span with renderer of which the [Settings] are transformed with [customize].
     */
    public constructor(
        name: CharSequence,
        parent: OpenTelemetrySpan,
        customize: Settings.() -> Settings,
    ) : this(name, parent, parent.renderer.nestedRenderer(name, customize), parent.tracer)

    private var span: io.opentelemetry.api.trace.Span? = null
    private var scope: Scope? = null

    override var state: State = Initialized(Now.instant)
        private set(value) {
            when (value) {
                is Initialized -> error("Already initialized")

                is Started -> {
                    field = value
                    parent?.start(value.timestamp)
                    tracer.spanBuilder(name.ansiRemoved)
                        .setStartTimestamp(value.timestamp)
                        .apply {
                            parent?.also {
                                val parentSpan = it.span
                                checkNotNull(parentSpan) { "Parent has no span: $parent" }
                                setParent(Context.current().with(parentSpan))
                            } ?: setNoParent()
                        }
                        .startSpan()
                        .also { span = it }
                        .also { scope = it.makeCurrent() }
                        .also { link() }
                        .also { renderer.start(it.traceId, it.spanId, value.timestamp) }
                }

                is Ended -> {
                    field = value
                    val span = checkNotNull(span)
                    val scope = checkNotNull(scope)

                    scope.close()
                    unlink()
                    when (value) {
                        is Succeeded -> span.setStatus(OK)
                        is Failed -> value.exception.message
                            ?.also { message -> span.setStatus(ERROR, message.ansiRemoved) }
                            ?: span.setStatus(ERROR)
                    }
                    span.end(value.timestamp)
                }
            }
        }

    override val traceId: TraceId? get() = span?.traceId
    override val spanId: SpanId? get() = span?.spanId

    override fun start(timestamp: Instant) {
        if (state is Initialized) state = Started(timestamp)
    }

    override fun event(name: CharSequence, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        start(timestamp)
        checkNotNull(span).addEvent(name.ansiRemoved, attributes.toAttributes(), timestamp)
        renderer.event(name, attributes, timestamp)
    }

    override fun exception(exception: Throwable, attributes: Map<CharSequence, CharSequence>, timestamp: Instant) {
        start(timestamp)
        checkNotNull(span).recordException(exception, attributes.toAttributes())
        renderer.exception(exception, attributes, timestamp)
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
        if (spanId != other.spanId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        result = 31 * result + spanId.hashCode()
        return result
    }

    public companion object {

        @Suppress("NOTHING_TO_INLINE")
        public inline fun Map<out CharSequence, CharSequence>.toAttributes(): Attributes =
            Attributes.builder().apply {
                entries.forEach { (key, value) -> put(key.ansiRemoved, value.ansiRemoved) }
            }.build()

        private val spansLock = ReentrantLock()
        private val spans = mutableMapOf<SpanId, OpenTelemetrySpan>()
        public fun OpenTelemetrySpan.link(): Unit = spansLock.withLock { spans[spanId ?: error("Span ID missing")] = this }
        public val OpenTelemetrySpan.linked: Boolean get() = spansLock.withLock { spans.containsKey(spanId) }
        public fun OpenTelemetrySpan.unlink(): Unit = spansLock.withLock { spans.remove(spanId ?: error("Span ID missing")) }
        private val current: OpenTelemetrySpan? get() = spansLock.withLock { spans[SpanId.current] }
        private fun currentOrAttach(customize: Settings.() -> Settings, tracer: TracerAPI): OpenTelemetrySpan {
            return current ?: OpenTelemetrySpan("current", Tracer.NOOP, CompactRenderer("current", Settings().customize()) { println(it) })
        }

        private fun currentOrAttach(renderer: (Settings, Printer) -> Renderer, tracer: TracerAPI): OpenTelemetrySpan {
            return current ?: OpenTelemetrySpan("current", Tracer.NOOP, renderer(Settings()) { println(it) })
        }

        /**
         * Depending on whether a there is already an open [Span], runs the given [block]
         * inside of it as a nested span or as a new span using the given [name]
         * and optional [customize] and [timestamp].
         *
         * If a [tracer] is specified it will be used instead of the default one.
         */
        public fun <R> spanning(
            name: CharSequence,
            customize: Settings.() -> Settings = { this },
            timestamp: Instant = Now.instant,
            tracer: TracerAPI = Tracer,
            block: SpanContext.() -> R,
        ): R {
            val current = currentOrAttach(customize, tracer)

            val r = current?.let {
                check(it.tracer == tracer) { "An active span ${it.spanId} was found that uses a different tracer ${it.tracer}." }
                OpenTelemetrySpan(name, it, it.renderer.nestedRenderer(name, customize))
            } ?: OpenTelemetrySpan(name, tracer, CompactRenderer(name, Settings().customize()) { println(it) })
            return r.run(timestamp, block)
        }

        /**
         * Depending on whether a there is already an open [Span], runs the given [block]
         * inside of it as a nested span or as a new span using the given [name], [renderer]
         * and optional [timestamp].
         *
         * If a [tracer] is specified it will be used instead of the default one.
         */
        public fun <R> spanning(
            name: CharSequence,
            renderer: (Settings, Printer) -> Renderer,
            timestamp: Instant = Now.instant,
            tracer: TracerAPI = Tracer,
            block: SpanContext.() -> R,
        ): R {
            val r = current?.let {
                check(it.tracer == tracer) { "An active span ${it.spanId} was found that uses a different tracer ${it.tracer}." }
                OpenTelemetrySpan(name, it, it.renderer.nestedRenderer(renderer))
            } ?: OpenTelemetrySpan(name, tracer, renderer(Settings()) { println(it) })
            return r.run(timestamp, block)
        }
    }
}
