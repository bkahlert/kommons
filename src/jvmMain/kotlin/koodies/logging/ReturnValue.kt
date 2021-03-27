package koodies.logging

import koodies.exception.toCompactString
import koodies.text.LineSeparators.LF
import koodies.text.Semantics

/**
 * Implementors of this interface gain control on
 * how it is displayed by [RenderingLogger].
 */
public interface ReturnValue {
    /**
     * Whether this return value represents a successful state.
     */
    public val successful: Boolean?

    /**
     * Returns this instance's representation for the
     * purpose of being displayed by a [RenderingLogger].
     */
    public fun format(): CharSequence
}

/**
 * Mutable list of return values that is considered [successful] if does not contain any
 * failed [ReturnValue].
 */
public class ReturnValues<E>(vararg elements: E) : MutableList<E> by mutableListOf<E>(*elements), ReturnValue {
    private val unsuccessful: List<ReturnValue> get() = map { it.toReturnValue() }.filter { it.successful == false }
    override val successful: Boolean?
        get() = fold(true) { acc: Boolean?, el: E ->
            when (el.toReturnValue().successful) {
                true -> acc
                null -> if (acc == true) null else acc
                false -> false
            }
        }

    override fun format(): CharSequence = when (unsuccessful.size) {
        0 -> ""
        1 -> unsuccessful.single().format()
        else -> "Multiple problems encountered: " + unsuccessful.joinToString("") { "$LF    ${Semantics.Error} ${it.format()}" }
    }

    /**
     * Adds the elements of the given [returnValues] to this list.
     */
    public operator fun plus(returnValues: ReturnValues<out E>): ReturnValues<E> = apply { addAll(returnValues) }
}

/**
 * Default [ReturnValue] implementation for the given [value].
 *
 * If [value] is a [ReturnValue] itself this implementation delegates to it.
 *
 * Otherwise [value] is considered [successful] and is formatted using [toCompactString].
 */
public inline class AnyReturnValue(private val value: Any?) : ReturnValue {
    override val successful: Boolean?
        get() = if (value is ReturnValue) {
            value.successful
        } else {
            true
        }

    override fun format(): CharSequence =
        if (value is ReturnValue) {
            value.format()
        } else {
            value?.toCompactString()
        } ?: Semantics.Null
}

/**
 * Default [ReturnValue] implementation for the given [exception].
 *
 * The given [exception] is considered non-[successful] and is formatted using [toCompactString].
 */
public inline class ExceptionReturnValue(private val exception: Throwable) : ReturnValue {
    override val successful: Boolean get() = false
    override fun format(): CharSequence = exception.toCompactString()
}

/**
 * Converts any value to a [ReturnValue] used to compute its representation
 * based on whether it implements [ReturnValue] or not.
 *
 * If [ReturnValue] is implemented this implementation is used. Otherwise
 * [ExceptionReturnValue] is used for instances of [Throwable] and [AnyReturnValue]
 * for any other value.
 *
 * This extension function is only valid in the context of an existing [RenderingLogger].
 */
private fun Any?.toReturnValue(): ReturnValue =
    when (this) {
        is ReturnValue -> this
        is Result<*> -> fold({ it.toReturnValue() }, { it.toReturnValue() })
        is Throwable -> ExceptionReturnValue(this)
        else -> AnyReturnValue(this)
    }

/**
 * Converts any value to a [ReturnValue] used to compute its representation
 * based on whether it implements [ReturnValue] or not.
 *
 * If [ReturnValue] is implemented this implementation is used. Otherwise
 * [ExceptionReturnValue] is used for instances of [Throwable] and [AnyReturnValue]
 * for any other value.
 *
 * This extension function is only valid in the context of an existing [RenderingLogger].
 */
@Suppress("unused")
public val RenderingLogger.toReturnValue: Any?.() -> ReturnValue
    get() = { toReturnValue() }

