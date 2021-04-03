package koodies

import koodies.runtime.Program
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
public fun <U, R> runWrapping(before: () -> U, after: (U) -> Unit, block: (U) -> R): R {
    val u = before()
    val r = runCatching { block(u) }
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
public fun <T, U, R> T.runWrapping(before: T.() -> U, after: T.(U) -> Unit, block: T.(U) -> R): R {
    val u = before()
    val r = runCatching { block(u) }
    after(u)
    return r.getOrThrow()
}

// @formatter:off
/** Throws an [IllegalArgumentException] if `this` string [isEmpty]. */
public fun String.requireNotEmpty(): String = also{require(it.isNotEmpty())}
/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if `this` string [isEmpty]. */
public fun String.requireNotEmpty(lazyMessage: () -> Any): String = also{require(it.isNotEmpty(),lazyMessage)}
/** Throws an [IllegalArgumentException] if `this` string [isBlank]. */
public fun String.requireNotBlank(): String = also{require(isNotBlank())}
/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if `this` string [isBlank]. */
public fun String.requireNotBlank(lazyMessage: () -> Any): String = also{require(it.isNotBlank(),lazyMessage)}

/** Throws an [IllegalStateException] if `this` string [isEmpty]. */
public fun String.checkNotEmpty(): String = also{check(it.isNotEmpty())}
/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if `this` string [isEmpty]. */
public fun String.checkNotEmpty(lazyMessage: () -> Any): String = also{check(it.isNotEmpty(),lazyMessage)}
/** Throws an [IllegalStateException] if `this` string [isBlank]. */
public fun String.checkNotBlank(): String = also{check(it.isNotBlank())}
/** Throws an [IllegalStateException] with the result of calling [lazyMessage] if `this` string [isBlank]. */
public fun String.checkNotBlank(lazyMessage: () -> Any): String = also{check(it.isNotBlank(),lazyMessage)}
// @formatter:on

/**
 * Returns `this` object if this [Program] runs in debug mode or `null`, if it's not.
 */
public fun <T> T.takeIfDebugging(): T? = takeIf { Program.isDebugging }

/**
 * Returns `this` object if this [Program] does not run in debug mode or `null`, if it is.
 */
public fun <T> T.takeUnlessDebugging(): T? = takeIf { Program.isDebugging }
