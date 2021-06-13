package koodies.tracing

@JvmInline
public value class SpanId(public val value: String) {
    public val valid: Boolean get() = value.any { it != '0' }
    override fun toString(): String = value
}
