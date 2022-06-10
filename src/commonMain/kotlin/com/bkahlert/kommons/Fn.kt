package com.bkahlert.kommons

import com.bkahlert.kommons.debug.xray
import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.BigIntegerConstants
import com.bkahlert.kommons.math.toBigInteger
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.truncate

/**
 * Returns a new identity function that composes this optional identity function
 * with the given identity [functions] by chaining them.
 */
public inline fun <reified T> ((T) -> T)?.compose(vararg functions: ((T) -> T)): ((T) -> T) =
    functions.reversed().foldRight(compose { it }, { acc, x -> x + acc })

/**
 * Returns a new identity function that composes this optional identity function
 * with the given mandatory identity [function] by chaining them.
 */
public inline fun <reified T> ((T) -> T)?.compose(crossinline function: ((T) -> T)): ((T) -> T) =
    { function(this?.invoke(it) ?: it) }

/**
 * Returns a new identity function that composes this optional identity function
 * with the given mandatory identity [function] by chaining them.
 */
public inline operator fun <reified T> ((T) -> T)?.plus(crossinline function: ((T) -> T)): ((T) -> T) =
    { function(this?.invoke(it) ?: it) }

/**
 * Returns a new identity function that composes the given identity [functions]
 * by chaining them.
 */
public inline fun <reified T> compositionOf(vararg functions: (T) -> T): ((T) -> T) {
    if (functions.isEmpty()) return { t: T -> t }
    return functions.first().compose(*functions.drop(1).toTypedArray())
}

/**
 * Returns a new identity function that composes those functions of the given
 * array of boolean-function pairs, with a [Pair.first] `true`.
 */
public inline fun <reified T> compositionOf(vararg functions: Pair<Boolean, (T) -> T>): ((T) -> T) {
    if (functions.isEmpty()) return { t: T -> t }
    return functions.first { it.first }.second
        .compose(*functions.filter { it.first }.drop(1).map { it.second }.toTypedArray())
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
 * Wraps the specified function [block] with this value as its receiver by calling the specified functions [before] and [after]
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

/**
 * Runs the provided [block] this times with an increasing index passed
 * on each call and returns a list of the returned results.
 */
public operator fun <R> Int.times(block: (index: Int) -> R): List<R> {
    require(this >= 0) { "times must not be negative" }
    return when (this) {
        0 -> emptyList()
        else -> (0 until this).map(block)
    }
}

/**
 * Runs the provided [block] this times with an increasing index passed
 * on each call and returns a list of the returned results.
 */
public operator fun <R> BigInteger.times(block: (index: Int) -> R): List<R> {
    require(this >= BigIntegerConstants.ZERO) { "times must not be negative" }
    require(this <= Int.MAX_VALUE.toBigInteger()) { "times must not be greater than ${Int.MAX_VALUE}" }
    return toInt().times(block)
}

/**
 * Invokes this nullable identity lambda with [arg] and returns its result.
 * If this is `null`, [arg] is returned unchanged.
 */
public operator fun <R, T : (R) -> R> T?.invoke(arg: R): R = this?.invoke(arg) ?: arg

/**
 * Throws an [IllegalArgumentException] with the details if the value is obviously
 * spoiled with unusual characters such as escape sequences.
 */
public fun String.requireSaneInput() {
    require(!ansiContained) {
        "ANSI escape sequences detected: $xray"
    }
    if (length > 1) {
        mapOf(
            "double quotes" to '\"',
            "single quotes" to '\'',
        ).forEach { (name, char) ->
            require((first() == char) == (last() == char)) {
                val annotatedInput = truncate(30).let {
                    val error = "$char".formattedAs.input
                    if ((first() == char)) error + it.substring(1, lastIndex)
                    else it.substring(0, lastIndex - 1) + error
                }

                "Unmatched $name detected: $annotatedInput"
            }
        }
    }
}
