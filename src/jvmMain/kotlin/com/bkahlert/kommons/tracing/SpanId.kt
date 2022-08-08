package com.bkahlert.kommons.tracing

import com.bkahlert.kommons.text.ansiRemoved
import io.opentelemetry.api.trace.Span

/**
 * ID of a span.
 */
@JvmInline
public value class SpanId(private val value: CharSequence) : CharSequence {

    /**
     * Whether this ID is valid.
     */
    public val valid: Boolean get() = io.opentelemetry.api.trace.SpanId.isValid(value)

    override val length: Int get() = value.length
    override fun get(index: Int): Char = value[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = value.subSequence(startIndex, endIndex)

    override fun toString(): String = value.toString().ansiRemoved

    public companion object {

        /**
         * ID of the currently active [Span].
         */
        public inline val current: SpanId
            get() = Span.current().spanId

        /**
         * ID of an invalid [Span].
         */
        public inline val invalid: SpanId
            get() = SpanId(io.opentelemetry.api.trace.SpanId.getInvalid())
    }
}

/**
 * The span ID of this span.
 */
public inline val Span.spanId: SpanId
    get() = SpanId(spanContext.spanId)
