package koodies.tracing

import koodies.text.ANSI.ansiRemoved

@JvmInline
public value class SpanId(public val value: CharSequence) {
    public val valid: Boolean get() = value.any { it != '0' }
    override fun toString(): String = value.ansiRemoved

    public companion object {
        /**
         * ID of the currently active [Span].
         */
        public inline val current: SpanId
            get() = io.opentelemetry.api.trace.Span.current().spanId
    }
}

/**
 * The span ID of this span.
 */
public inline val io.opentelemetry.api.trace.Span.spanId: SpanId
    get() = SpanId(spanContext.spanId)
