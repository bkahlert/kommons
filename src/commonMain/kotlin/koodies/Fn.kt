package koodies

import koodies.Either.Left
import koodies.Either.Right
import koodies.debug.xray
import koodies.runtime.isDebugging
import koodies.text.ANSI.containsEscapeSequences
import koodies.text.Semantics.formattedAs
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * Runs the specified [block] if `this` is `null` and provide
 * and expected instance of [T].
 *
 * Example:
 * ```kotlin
 * val value: String? = …
 *  …
 * val result: String = value otherwise { "fallback" }
 * ```
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

/**
 * Runs the provided [block] `this` times with an increasing index passed
 * on each call and returns a list of the returned results.
 */
public operator fun <R> Int.times(block:(index:Int)->R): List<R> {
    require(this >= 0) { "times must not be negative"}
    return when(this) {
        0 -> emptyList()
        else -> (0 until this).map(block)
    }
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
 * Throws an [IllegalArgumentException] with the details if the value is obviously
 * spoiled with unusual characters such as escape sequences.
 */
public fun String.requireSaneInput() {
    require(!containsEscapeSequences) {
        "ANSI escape sequences detected: $xray"
    }
    if (length > 1) {
        mapOf(
            "double quotes" to '\"',
            "single quotes" to '\'',
        ).forEach { (name, char) ->
            require((first() == char) == (last() == char)) {
                val annotatedInput = truncate(30, strategy = MIDDLE).let {
                    val error = "$char".formattedAs.input
                    if ((first() == char)) error + it.substring(1, lastIndex)
                    else it.substring(0, lastIndex - 1) + error
                }

                "Unmatched $name detected: $annotatedInput"
            }
        }
    }
}

/**
 * Returns `this` object if this [ProgramInstance] runs in debug mode or `null`, if it's not.
 */
public fun <T> T.takeIfDebugging(): T? = takeIf { isDebugging }

/**
 * Returns `this` object if this [ProgramInstance] does not run in debug mode or `null`, if it is.
 */
public fun <T> T.takeUnlessDebugging(): T? = takeIf { isDebugging }

/**
 * Represents a container containing either an instance of type [A] ([Left]]
 * or [B] ([Right]).
 */
// TODO transform to sealed interface when Kotlin 1.5 is out
public sealed class Either<A, B> {
    public class Left<A, B>(public val left: A) : Either<A, B>()
    public class Right<A, B>(public val right: B) : Either<A, B>()
}

/**
 * Returns either the encapsulated instance [A] or
 * the encapsulated instance [B] transformed to an instance of [A]
 * using the given [transform].
 */
public inline fun <reified A, reified B> Either<A, B>.or(transform: (B) -> A): A =
    when (this) {
        is Left -> left
        is Right -> transform(right)
    }

/**
 * Returns either the encapsulated instance [A] mapped using [transform] or
 * the encapsulated instance [B].
 */
public inline fun <reified A, reified B, reified R> Either<A, B>.map(transform: A.() -> R): Either<R, B> =
    when (this) {
        is Left -> Left(left.transform())
        is Right ->
            @Suppress("UNCHECKED_CAST")
            this.also { check(it === right) } as Either<R, B>
    }
//
///**
// * Returns either the encapsulated instance [A] mapped using [transform] or
// * the encapsulated instance [B].
// */
//public inline fun <reified A, reified B> Either<List<A>, B>.singleOr(transform: List<A>.(B) -> A): A =
//    when (this) {
//        is Left -> left.singleOrNull() ?: left.transform()
//        is Right -> emptyList<A>().transform(right)
//    }
