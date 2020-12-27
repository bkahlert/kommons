package koodies.functional

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * If [condition] is `true`, calls the specified function [block] and returns its result.
 *
 * @see [run]
 */
inline fun <R> runIf(condition: Boolean, block: () -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition) block() else null
}

/**
 *  If [predicate] evaluates to `true`, calls the specified function [block] and returns its result.
 *
 * @see [run]
 */
inline fun <R> runIf(predicate: () -> Boolean, block: () -> R): R? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (predicate()) block() else null
}

/**
 * If [condition] is `true`, calls the specified function [block] with `this` value as its receiver and returns its result.
 *
 * @see [run]
 */
inline fun <T : R, R> T.runIf(condition: Boolean, block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition) block(this) else this
}

/**
 * If [predicate] evaluates to `true`, calls the specified function [block] with `this` value as its receiver and returns its result.
 *
 * @see [run]
 */
inline fun <T : R, R> T.runIf(predicate: (T) -> Boolean, block: T.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return if (predicate(this)) block(this) else this
}
