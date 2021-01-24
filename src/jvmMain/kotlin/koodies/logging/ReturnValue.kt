package koodies.logging

import koodies.exception.toCompactString

/**
 * Implementors of this interface gain control on
 * how it is displayed by [RenderingLogger].
 */
interface ReturnValue {
    /**
     * Whether this return value represents a successful state.
     */
    val successful: Boolean

    /**
     * Returns this instance's representation for the
     * purpose of being displayed by a [RenderingLogger].
     */
    fun format(): CharSequence
}

inline class DelegatingReturnValue(private val delegate: ReturnValue) : ReturnValue {
    constructor(result: Result<*>) : this(result.fold({ it.toReturnValue() }, { it.toReturnValue() }))

    override val successful: Boolean get() = delegate.successful
    override fun format(): CharSequence = delegate.format()
}

inline class AnyReturnValue(private val value: Any?) : ReturnValue {
    override val successful: Boolean get() = value?.let { it as? ReturnValue }?.successful ?: true
    override fun format(): CharSequence = value?.let { it as? ReturnValue }?.format() ?: value?.toCompactString() ?: "␀"
}

inline class ExceptionReturnValue(private val exception: Throwable) : ReturnValue {
    override val successful: Boolean get() = false
    override fun format(): CharSequence = exception.toCompactString()
}

fun Any?.toReturnValue(): ReturnValue =
    when (this) {
        is ReturnValue -> this
        is Result<*> -> DelegatingReturnValue(this)
        is Throwable -> ExceptionReturnValue(this)
        else -> AnyReturnValue(this)
    }
