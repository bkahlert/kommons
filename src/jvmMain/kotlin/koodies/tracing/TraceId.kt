package koodies.tracing

import io.opentelemetry.api.trace.Span
import koodies.text.ANSI.ansiRemoved

/**
 * ID of a trace.
 */
@JvmInline
public value class TraceId(private val value: CharSequence) : CharSequence {

    /**
     * Whether this ID is valid.
     */
    public val valid: Boolean get() = io.opentelemetry.api.trace.TraceId.isValid(value)

    override val length: Int get() = value.length
    override fun get(index: Int): Char = value[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = value.subSequence(startIndex, endIndex)

    override fun toString(): String = value.ansiRemoved

    public companion object {
        /**
         * ID of the currently active [Span].
         */
        public inline val current: TraceId
            get() = Span.current().traceId

        /**
         * ID of an invalid [Span].
         */
        public inline val invalid: TraceId
            get() = TraceId(io.opentelemetry.api.trace.TraceId.getInvalid())
    }
}

/**
 * The trace ID of this span.
 */
public inline val Span.traceId: TraceId
    get() = TraceId(spanContext.traceId)
