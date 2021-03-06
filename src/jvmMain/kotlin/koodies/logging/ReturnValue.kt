package koodies.logging

import koodies.exception.toCompactString

/**
 * Implementors of this interface gain control on
 * how it is displayed by [RenderingLogger].
 */
public interface ReturnValue {
    /**
     * Whether this return value represents a successful state.
     */
    public val successful: Boolean

    /**
     * Returns this instance's representation for the
     * purpose of being displayed by a [RenderingLogger].
     */
    public fun format(): CharSequence
}

public inline class DelegatingReturnValue(private val delegate: ReturnValue) : ReturnValue {
    public constructor(result: Result<*>) : this(result.fold({ it.toReturnValue() }, { it.toReturnValue() }))

    override val successful: Boolean get() = delegate.successful
    override fun format(): CharSequence = delegate.format()
}

public inline class AnyReturnValue(private val value: Any?) : ReturnValue {
    override val successful: Boolean get() = value?.let { it as? ReturnValue }?.successful ?: true
    override fun format(): CharSequence = value?.let { it as? ReturnValue }?.format() ?: value?.toCompactString() ?: "␀"
}

public inline class ExceptionReturnValue(private val exception: Throwable) : ReturnValue {
    override val successful: Boolean get() = false
    override fun format(): CharSequence = exception.toCompactString()
}

public fun Any?.toReturnValue(): ReturnValue =
    when (this) {
        is ReturnValue -> this
        is Result<*> -> DelegatingReturnValue(this)
        is Throwable -> ExceptionReturnValue(this)
        else -> AnyReturnValue(this)
    }
