package koodies.tracing

import koodies.text.ANSI.ansiRemoved

@JvmInline
public value class TraceId(public val value: CharSequence) {
    public val valid: Boolean get() = value.any { it != '0' }
    override fun toString(): String = value.ansiRemoved

    public companion object
}

/**
 * The trace ID of this span.
 */
public inline val io.opentelemetry.api.trace.Span.traceId: TraceId
    get() = TraceId(spanContext.traceId)
