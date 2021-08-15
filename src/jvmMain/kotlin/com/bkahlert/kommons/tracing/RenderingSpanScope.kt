package com.bkahlert.kommons.tracing

import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.tracing.Key.KeyValue
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout
import com.bkahlert.kommons.tracing.rendering.CompactRenderer
import com.bkahlert.kommons.tracing.rendering.Printer
import com.bkahlert.kommons.tracing.rendering.RenderableAttributes
import com.bkahlert.kommons.tracing.rendering.Renderer
import com.bkahlert.kommons.tracing.rendering.RendererProvider
import com.bkahlert.kommons.tracing.rendering.RenderingAttributes
import com.bkahlert.kommons.tracing.rendering.ReturnValue
import com.bkahlert.kommons.tracing.rendering.Settings
import com.bkahlert.kommons.tracing.rendering.Style
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// TODO use Context to link renderer
//public class ContextUser {
//
//    private static final ContextKey<MyState> KEY = ContextKey.named("MyState");
//
//    public Context startWork() {
//        return Context.withValues(KEY, new MyState());
//    }
//
//    public void continueWork(Context context) {
//        MyState state = context.get(KEY);
//        // Keys are compared by reference only.
//        assert state != Context.current().get(ContextKey.named("MyState"));
//        ...
//    }
//}

private val linkedRenderersLock = ReentrantLock()
private val linkedRenderers = mutableMapOf<SpanId, Renderer>()

private fun Span.linkRenderer(renderer: Renderer): Unit = linkedRenderersLock.withLock { spanId.takeIf { it.valid }?.let { linkedRenderers[it] = renderer } }

/** Whether this span has a [Renderer] linked to it. */
internal val Span.rendererLinked: Boolean get() = spanId.rendererLinked

/** Whether this span has a [Renderer] linked to it. */
internal val SpanId.rendererLinked: Boolean get() = linkedRenderersLock.withLock { linkedRenderers.containsKey(this) }

/** The [Renderer] linked to this span. If no renderer is linked, the [RootRenderer] is returned. */
internal val Span.linkedRenderer: Renderer get() = spanId.linkedRenderer

/** The [Renderer] linked to this span. If no renderer is linked, the [RootRenderer] is returned. */
internal val SpanId.linkedRenderer: Renderer get() = linkedRenderersLock.withLock { takeIf { it.valid }?.let { linkedRenderers[it] } ?: RootRenderer }

private fun Span.unlinkRenderer(): Unit = linkedRenderersLock.withLock { spanId.takeIf { it.valid }?.let { linkedRenderers.remove(it) } }

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
 * A span that renders all invocations using [renderer]
 * after having delegated them to [span].
 */
internal data class RenderingSpanScope(
    private val span: Span,
    private val renderer: Renderer,
) : SpanScope {

    override fun makeCurrent(): Scope = span.makeCurrent()

    override fun event(event: Event): SpanScope {
        span.addEvent(event.name.ansiRemoved, event.attributes.toAttributes())
        val attributes = RenderableAttributes.of(event.attributes)
        renderer.event(event.name, attributes)
        return this
    }

    override fun <T : Any> attribute(key: AttributeKey<T>, value: T): SpanScope = apply {
        span.setAttribute(key, value)
    }

    override fun status(statusCode: StatusCode, description: String): SpanScope = apply {
        span.setStatus(statusCode, description)
    }

    override fun exception(exception: Throwable, vararg attributes: KeyValue<*, *>): SpanScope = apply {
        span.recordException(exception, attributes.toList().toAttributes())
        renderer.exception(exception, RenderableAttributes.of(*attributes))
    }

    fun end() {
        span.setStatus(OK)
        span.end()
        renderer.end(Result.success(Unit))
        span.unlinkRenderer()
    }

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
        span.unlinkRenderer()
    }

    companion object {

        /**
         * Creates a new top-level [RenderingSpanScope].
         */
        internal fun of(
            name: CharSequence,
            vararg attributes: KeyValue<*, *>,
            rendererProvider: (Renderer) -> Renderer,
        ): RenderingSpanScope = of(
            name = name,
            parentSpan = Span.getInvalid(),
            attributes = attributes,
            rendererProvider = rendererProvider
        )

        /**
         * Creates a new child [RenderingSpanScope] for the given [parentSpan].
         */
        internal fun of(
            name: CharSequence,
            parentSpan: Span,
            vararg attributes: KeyValue<*, *>,
            rendererProvider: (Renderer) -> Renderer,
        ): RenderingSpanScope {
            val renderer = rendererProvider(parentSpan.linkedRenderer)

            val span = KommonsTracer
                .spanBuilder(name.ansiRemoved)
                .setParent(Context.current().with(parentSpan))
                .setAllAttributes(attributes.toList().toAttributes())
                .setAttribute(RenderingAttributes.RENDERER, (RenderingAttributes.RENDERER to renderer).value.ansiRemoved)
                .startSpan()

            if (span.spanId.valid) {
                span.linkRenderer(renderer)
            }

            renderer.start(span.traceId, span.spanId, name)

            return RenderingSpanScope(span, renderer)
        }
    }
}

/**
 * Runs the given [block] with the current [SpanScope]
 * as its receiver.
 *
 * All recorded events and exceptions are also printed to the console.
 * The exact behaviour can be customized using the optional [renderer].
 */
@TracingDsl
public fun <R> spanScope(
    recordException: Boolean = true,
    renderer: RendererProvider = { it.create(this) },
    block: SpanScope.() -> R,
): R {
    val span = Span.current()
    val actual = renderer(Settings()) { span.linkedRenderer }
    val scope = span.makeCurrent()
    val result = with(RenderingSpanScope(span, actual)) {
        runCatching(block).onFailure {
            if (recordException) exception(it)
        }
    }
    scope.close()
    return result.getOrThrow()
}

/**
 * Runs the given [block] with a new child [SpanScope] of the current [SpanScope]
 * as its receiver.
 *
 * All recorded events and exceptions are also printed to the console.
 * The exact behaviour can be customized using the optional [renderer].
 *
 * The returned result of the given block is used to end the span with
 * either an [StatusCode.OK] or [StatusCode.ERROR] status.
 */
@TracingDsl
public fun <R> runSpanning(
    name: CharSequence,
    vararg attributes: KeyValue<*, *>,
    renderer: RendererProvider = { it.create(this) },

    nameFormatter: FilteringFormatter<CharSequence>? = null,
    contentFormatter: FilteringFormatter<CharSequence>? = null,
    decorationFormatter: Formatter<CharSequence>? = null,
    returnValueTransform: ((ReturnValue) -> ReturnValue?)? = null,
    layout: ColumnsLayout? = null,
    style: ((ColumnsLayout, Int) -> Style)? = null,
    printer: Printer? = null,

    block: SpanScope.() -> R,
): R = spanScope(recordException = false) {
    val renderingChildSpan = RenderingSpanScope.of(name, Span.current(), *attributes) {
        it.childRenderer { default ->
            renderer(copy(
                nameFormatter = nameFormatter ?: this.nameFormatter,
                contentFormatter = contentFormatter ?: this.contentFormatter,
                decorationFormatter = decorationFormatter ?: this.decorationFormatter,
                returnValueTransform = returnValueTransform ?: this.returnValueTransform,
                layout = layout ?: this.layout,
                style = style ?: this.style,
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
