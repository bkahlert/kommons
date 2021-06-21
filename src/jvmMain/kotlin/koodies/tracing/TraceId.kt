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
    internal val valid: Boolean get() = value.any { it != '0' }

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
    }
}

/**
 * The trace ID of this span.
 */
public inline val Span.traceId: TraceId
    get() = TraceId(spanContext.traceId)
