package koodies.tracing

import koodies.text.ANSI.ansiRemoved

@JvmInline
public value class TraceId(public val value: CharSequence) {
    public val valid: Boolean get() = value.any { it != '0' }
    override fun toString(): String = value.ansiRemoved
}
