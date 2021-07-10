package koodies.tracing

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import koodies.text.ANSI.FilteringFormatter
import koodies.text.ANSI.Formatter
import koodies.text.ANSI.ansiRemoved
import koodies.tracing.Key.KeyValue
import koodies.tracing.rendering.BlockRenderer
import koodies.tracing.rendering.BlockStyle
import koodies.tracing.rendering.BlockStyles.None
import koodies.tracing.rendering.ColumnsLayout
import koodies.tracing.rendering.CompactRenderer
import koodies.tracing.rendering.Printer
import koodies.tracing.rendering.RenderableAttributes
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.RendererProvider
import koodies.tracing.rendering.ReturnValue
import koodies.tracing.rendering.Settings
import koodies.tracing.rendering.Style
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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

private val Span.linkedRenderer: Renderer
    get() = linkedRenderersLock.withLock {
        spanId.takeIf { it.valid }?.let { linkedRenderers[it] } ?: RootRenderer
    }

/** Renderer that does not render does not render itself, but can only be used to create child renderer. */
public object RootRenderer : Renderer {
    override fun start(traceId: TraceId, spanId: SpanId, name: CharSequence): Unit = Unit
    override fun event(name: CharSequence, attributes: RenderableAttributes): Unit = Unit
    override fun exception(exception: Throwable, attributes: RenderableAttributes): Unit = Unit
    override fun <R> end(result: Result<R>): Unit = Unit
    override fun childRenderer(renderer: RendererProvider): Renderer = renderer(Settings(printer = ::printChild)) { CompactRenderer(it) }
    override fun printChild(text: CharSequence): Unit = println(text)
}

/**
 * Creates a new rendering child span
 */
internal fun Span.renderingChildSpan(
    name: CharSequence,
    tracer: io.opentelemetry.api.trace.Tracer,
    vararg attributes: KeyValue<*, *>,
    rendererProvider: (Renderer) -> Renderer,
): RenderingSpan {
    val span = tracer
        .spanBuilder(name.ansiRemoved)
        .setParent(Context.current().with(this))
        .setAllAttributes(attributes.toList().toAttributes())
        .startSpan()

    val renderer = if (span.spanId.valid) {
        linkedRenderersLock.withLock {
            linkedRenderer.let(rendererProvider)
                .also { span.linkRenderer(it) }
                .also { it.start(span.traceId, span.spanId, name) }
        }
    } else {
        run { BlockRenderer(Settings(blockStyle = None)) }
    }

    return RenderingSpan(span, renderer)
}

/**
 * A span that renders all invocations using [renderer]
 * after having delegated them to [span].
 */
internal data class RenderingSpan(
    private val span: Span,
    private val renderer: Renderer,
) : CurrentSpan, Span {

    // needs to delegate to span to the span instance becomes current (not this wrapper)
    override fun makeCurrent(): Scope = span.makeCurrent()

    override fun event(event: Event): CurrentSpan {
        span.addEvent(event.name.ansiRemoved, event.attributes.toAttributes())
        val attributes = RenderableAttributes.of(event.attributes)
        renderer.event(event.name, attributes)
        return this
    }

    override fun <T : Any> setAttribute(key: AttributeKey<T>, value: T): Span = span.setAttribute(key, value)

    override fun addEvent(name: String, attributes: Attributes): Span {
        span.addEvent(name.ansiRemoved, attributes)
        renderer.event(name, RenderableAttributes.of(attributes))
        return this
    }

    override fun addEvent(name: String, attributes: Attributes, timestamp: Long, unit: TimeUnit): Span {
        span.addEvent(name.ansiRemoved, attributes, timestamp, unit)
        renderer.event(name, RenderableAttributes.of(attributes))
        return this
    }

    override fun setStatus(statusCode: StatusCode, description: String): Span = span.setStatus(statusCode, description)

    override fun exception(exception: Throwable, vararg attributes: KeyValue<*, *>): CurrentSpan {
        span.recordException(exception, attributes.toList().toAttributes())
        renderer.exception(exception, RenderableAttributes.of(*attributes))
        return this
    }

    override fun recordException(exception: Throwable, additionalAttributes: Attributes): Span {
        span.recordException(exception, additionalAttributes)
        renderer.exception(exception, RenderableAttributes.of(additionalAttributes))
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

    fun <R> end(result: Result<R>) {
        result.fold({
            span.setStatus(OK)
            span.end()
            renderer.end(result)
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
internal inline fun <reified R> RenderingSpan.end(value: R): Unit =
    end(Result.success(value))

/** Ends this span with the given [exception]. */
@Suppress("NOTHING_TO_INLINE")
internal inline fun RenderingSpan.end(exception: Throwable?): Unit =
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
    val renderer = span.linkedRenderer
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
 * Runs the given [block] with the [CurrentSpan]
 * as its receiver.
 */
@TracingDsl
public fun <R> tracing(
    recordException: Boolean = true,
    renderer: RendererProvider,
    block: CurrentSpan.() -> R,
): R {
    val span = Span.current()
    val actual = renderer(Settings()) { span.linkedRenderer }
    val scope = span.makeCurrent()
    val result = with(RenderingSpan(span, actual)) {
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
 * The exact behaviour can be customized using the optional [renderer].
 *
 * The returned result of the given block is used to end the span with
 * either an [StatusCode.OK] or [StatusCode.ERROR] status.
 */
@TracingDsl
public fun <R> spanning(
    name: CharSequence,
    vararg attributes: KeyValue<*, *>,
    renderer: RendererProvider = { it(this) },
    tracer: io.opentelemetry.api.trace.Tracer = Tracer,

    nameFormatter: FilteringFormatter? = null,
    contentFormatter: FilteringFormatter? = null,
    decorationFormatter: Formatter? = null,
    returnValueTransform: ((ReturnValue) -> ReturnValue?)? = null,
    layout: ColumnsLayout? = null,
    blockStyle: ((ColumnsLayout) -> BlockStyle)? = null,
    oneLineStyle: Style? = null,
    printer: Printer? = null,

    block: CurrentSpan.() -> R,
): R = tracing(recordException = false) {
    val renderingChildSpan = Span.current().renderingChildSpan(name, tracer, *attributes) {
        it.childRenderer { default ->
            renderer(copy(
                nameFormatter = nameFormatter ?: this.nameFormatter,
                contentFormatter = contentFormatter ?: this.contentFormatter,
                decorationFormatter = decorationFormatter ?: this.decorationFormatter,
                returnValueTransform = returnValueTransform ?: this.returnValueTransform,
                layout = layout ?: this.layout,
                blockStyle = blockStyle ?: this.blockStyle,
                oneLineStyle = oneLineStyle ?: this.oneLineStyle,
                printer = printer ?: this.printer,
            ), default)
        }
    }
    val scope = renderingChildSpan.makeCurrent()
    val result = with(renderingChildSpan) {
        runCatching(block)
            .also { scope.close() }
            .also { end(it) }
    }
    result.getOrThrow()
}
