package koodies.functional

import kotlin.DeprecationLevel.ERROR
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * If [condition] is `true`, calls the specified function [block] with `this` value as its receiver and returns `this` value.
 *
 * @see [apply]
 */
@Deprecated("delete", level = ERROR)
inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (condition) block()
    return this
}

/**
 * If [value] is not `null`, calls the specified function [block] with `this` value as its receiver, [value] as its argument and returns `this` value.
 *
 * @see [apply]
 */
@Deprecated("delete", level = ERROR)
inline fun <T, V : Any> T.applyIfNotNull(value: V?, block: T.(V) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    value?.let {
        block(it)
    }
    return this
}

/**
 * If [predicate] evaluates to `true`, calls the specified function [block] with `this` value as its receiver and returns `this` value.
 *
 * @see [apply]
 */
@Deprecated("delete", level = ERROR)
inline fun <T> T.applyIf(predicate: (T) -> Boolean, block: T.() -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (predicate(this)) block()
    return this
}
