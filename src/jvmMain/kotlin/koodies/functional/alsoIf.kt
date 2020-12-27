package koodies.functional

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


/**
 * If [condition] is `true`, calls the specified function [block] with `this` value as its argument and returns `this` value.
 *
 * @see [also]
 */
inline fun <T> T.alsoIf(condition: Boolean, block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (condition) block(this)
    return this
}

/**
 * If [predicate] evaluates to `true`, calls the specified function [block] with `this` value as its argument and returns `this` value.
 *
 * @see [also]
 */
inline fun <T> T.alsoIf(predicate: (T) -> Boolean, block: (T) -> Unit): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    if (predicate(this)) block(this)
    return this
}
