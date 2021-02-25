package koodies.functional

import kotlin.DeprecationLevel.ERROR
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * If [condition] is `true`, calls the specified function [block] with `this` value as its argument and returns its result.
 *
 * @see [let]
 */
@Deprecated("delete", level = ERROR)
inline fun <T : R, R> T.letIf(condition: Boolean, block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition) block(this) else this
}

/**
 * If [predicate] evaluates to `true`, calls the specified function [block] with `this` value as its argument and returns its result.
 *
 * @see [let]
 */
@Deprecated("delete", level = ERROR)
inline fun <T : R, R> T.letIf(predicate: (T) -> Boolean, block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (predicate(this)) block(this) else this
}

/**
 * If [predicate] evaluates to `true`, returns the specified [value].
 *
 * @see [let]
 */
@Deprecated("delete", level = ERROR)
inline fun <T : R, R> T.letIf(predicate: (T) -> Boolean, value: R): R =
    if (predicate(this)) value else this

/**
 * If [predicate] equals this, returns the specified [value].
 *
 * @see [let]
 */
@Deprecated("delete", level = ERROR)
fun <T : R, R> T.letIf(predicate: T, value: R): R =
    if (this == predicate) value else this
