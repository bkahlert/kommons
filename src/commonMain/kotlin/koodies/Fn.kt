package koodies

import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * Runs the specified [block] if `this` is `null` and provide
 * and expected instance of [T].
 *
 * @see takeIf
 */
public inline infix fun <T> T?.otherwise(block: () -> T): T {
    contract {
        callsInPlace(block, EXACTLY_ONCE)
    }
    return this ?: block()
}

/**
 * Wraps the specified function [block] by calling the specified functions [before] and [after]
 * the actual invocation.
 *
 * Then return value of [before] is passed as an argument to [after] which
 * is handy if a value needs to be changed and restored.
 *
 * Returns the invocations result on success and throws the encountered exception on failure.
 * In both cases [after] will be called.
 */
public fun <U, R> runWrapping(before: () -> U, after: (U) -> Unit, block: () -> R): R {
    val u = before()
    val r = runCatching(block)
    after(u)
    return r.getOrThrow()
}

/**
 * Wraps the specified function [block] with `this` value as its receiver by calling the specified functions [before] and [after]
 * the actual invocation.
 *
 * Then return value of [before] is passed as an argument to [after] which
 * is handy if a value needs to be changed and restored.
 *
 * Returns the invocations result on success and throws the encountered exception on failure.
 * In both cases [after] will be called.
 */
public fun <T, U, R> T.runWrapping(before: T.() -> U, after: T.(U) -> Unit, block: T.() -> R): R {
    val u = before()
    val r = runCatching(block)
    after(u)
    return r.getOrThrow()
}
