package koodies.tracing

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.context.Context
import koodies.text.ANSI.ansiRemoved
import koodies.tracing.rendering.BlockRenderer
import koodies.tracing.rendering.CompactRenderer
import koodies.tracing.rendering.Printer
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.Settings
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import io.opentelemetry.api.trace.Tracer as TracerAPI

private val linkedRenderersLock = ReentrantLock()
private val linkedRenderers = mutableMapOf<SpanId, Renderer>()

private fun Span.linkRenderer(renderer: Renderer): Unit = linkedRenderersLock.withLock {
    spanId.takeIf { it.valid }?.let { linkedRenderers[it] = renderer }
}

internal val Span.rendererLinked: Boolean
    get() = linkedRenderersLock.withLock {
        linkedRenderers.containsKey(spanId)
    }

private fun Span.unlinkRenderer(): Unit = linkedRenderersLock.withLock {
    spanId.takeIf { it.valid }?.let { linkedRenderers.remove(it) }
}

private val Span.linkedRenderer: Renderer?
    get() = linkedRenderersLock.withLock {
        spanId.takeIf { it.valid }?.let { linkedRenderers[it] }
    }

internal fun Span?.newChildSpan(
    name: CharSequence,
    tracer: TracerAPI,
    rendererProvider: (Renderer?) -> Renderer,
): Pair<Span, Renderer> {
    val spanBuilder = tracer.spanBuilder(name.ansiRemoved)
    this?.also { spanBuilder.setParent(Context.current().with(it)) }
    val span = spanBuilder.startSpan()

    check(span.spanId.valid) { "Span has not valid span ID: $span" }

    val renderer = linkedRenderersLock.withLock {
        this?.linkedRenderer.let(rendererProvider)
            .also { span.linkRenderer(it) }
            .also { it.start(span.traceId, span.spanId, name) }
    }

    return span to renderer
}

/**
 * A span that renders all invocations using [renderer]
 * after having delegated them to [span].
 */
public data class RenderingSpan(
    private val span: Span,
    private val renderer: Renderer,
) : CurrentSpan, Span {

    override fun event(name: CharSequence, attributes: Map<CharSequence, Any>): CurrentSpan {
        attributes.toAttributes().also {
            span.addEvent(name.ansiRemoved, it)
            renderer.event(name, it)
        }
        return this
    }

    override fun <T : Any?> setAttribute(key: AttributeKey<T>, value: T): Span = span.setAttribute(key, value)

    override fun addEvent(name: String, attributes: Attributes): Span {
        span.addEvent(name.ansiRemoved, attributes)
        renderer.event(name, attributes)
        return this
    }

    override fun addEvent(name: String, attributes: Attributes, timestamp: Long, unit: TimeUnit): Span {
        span.addEvent(name.ansiRemoved, attributes, timestamp, unit)
        renderer.event(name, attributes)
        return this
    }

    override fun setStatus(statusCode: StatusCode, description: String): Span = span.setStatus(statusCode, description)

    override fun exception(exception: Throwable, attributes: Map<CharSequence, Any>): CurrentSpan {
        attributes.toAttributes().also {
            span.recordException(exception, it)
            renderer.exception(exception, it)
        }
        return this
    }

    override fun recordException(exception: Throwable, additionalAttributes: Attributes): Span {
        span.recordException(exception, additionalAttributes)
        renderer.exception(exception, additionalAttributes)
        return this
    }

    override fun updateName(name: String): Span = span.updateName(name)

    override fun end() {
        span.setStatus(OK)
        span.end()
        renderer.end(Result.success(Unit))
    }

    override fun end(timestamp: Long, unit: TimeUnit) {
        span.setStatus(OK)
        span.end(timestamp, unit)
        renderer.end(Result.success(Unit))
    }

    override fun getSpanContext(): SpanContext = span.spanContext

    override fun isRecording(): Boolean = span.isRecording

    public fun <R> end(result: Result<R>) {
        result.fold({
            end()
        }, {
            it.message
                ?.also { message -> span.setStatus(ERROR, message.ansiRemoved) }
                ?: span.setStatus(ERROR)
            span.end()
            renderer.end(result)
        })
        unlinkRenderer()
    }
}

/** Ends this span with the given [value]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <reified R> RenderingSpan.end(value: R): Unit =
    end(Result.success(value))

/** Ends this span with the given [exception]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun RenderingSpan.end(exception: Throwable?): Unit =
    exception?.let { end(Result.failure<Unit>(it)) } ?: end(Result.success(Unit))

/**
 * Runs the given [block] with the [CurrentSpan]
 * as its receiver.
 */
@TracingDsl
public fun <R> tracing(
    recordException: Boolean = true,
    block: CurrentSpan.() -> R,
): R {
    val span = Span.current()
    val renderer = span.linkedRenderer ?: object : Renderer {
        override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence): Unit = Unit
        override fun event(name: CharSequence, attributes: Attributes): Unit = Unit
        override fun exception(exception: Throwable, attributes: Attributes): Unit = Unit
        override fun <R> end(result: Result<R>): Unit = Unit
        override fun customizedChild(customize: Settings.() -> Settings): Renderer = BlockRenderer(Settings().customize()) { printChild(it) }
        override fun injectedChild(provider: (Settings, Printer) -> Renderer): Renderer = provider(Settings()) { printChild(it) }
        override fun printChild(text: CharSequence) = println(text)
    }
    val scope = span.makeCurrent()
    val result = with(RenderingSpan(span, renderer)) {
        runCatching(block).onFailure {
            if (recordException) recordException(it)
        }
    }
    scope.close()
    return result.getOrThrow()
}

/**
 * Creates a new nested span inside of the currently active span,
 * and runs [block] with this newly creates span as its [CurrentSpan] in the receiver.
 *
 * All recorded events and exceptions are also printed to the console.
 * The exact behaviour can be customized using the optional [customize].
 *
 * The returned result of the given block is used to end the span with
 * either an [StatusCode.OK] or [StatusCode.ERROR] status.
 */
@TracingDsl
public fun <R> spanning(
    name: CharSequence,
    customize: Settings.() -> Settings = { this },
    tracer: TracerAPI = Tracer,
    block: CurrentSpan.() -> R,
): R = tracing(recordException = false) {
    val (span, renderer) = Span.current().newChildSpan(name, tracer) {
        it?.customizedChild { customize() } ?: CompactRenderer(Settings()) { println(it) }
    }
    val scope = span.makeCurrent()
    val result = with(RenderingSpan(span, renderer)) {
        runCatching(block)
            .also { scope.close() }
            .also { end(it) }
    }
    result.getOrThrow()
}

/**
 * Creates a new nested span inside of the currently active span,
 * and runs [block] with this newly creates span as its [CurrentSpan] in the receiver.
 *
 * All recorded events and exceptions are also rendered using the specified [renderer].
 *
 * The returned result of the given block is used to end the span with
 * either an [StatusCode.OK] or [StatusCode.ERROR] status.
 */
@TracingDsl
public fun <R> spanning(
    name: CharSequence,
    renderer: (Settings, Printer) -> Renderer,
    tracer: TracerAPI = Tracer,
    block: CurrentSpan.() -> R,
): R = tracing(recordException = false) {
    val (span, renderer_) = Span.current().newChildSpan(name, tracer) {
        it?.injectedChild(renderer) ?: CompactRenderer(Settings()) { println(it) }
    }
    val scope = span.makeCurrent()
    val result = with(RenderingSpan(span, renderer_)) {
        runCatching(block)
            .also { scope.close() }
            .also { end(it) }
    }
    result.getOrThrow()
}
