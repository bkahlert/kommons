package com.bkahlert.kommons

import com.bkahlert.kommons.Either.Left
import com.bkahlert.kommons.Either.Right
import kotlin.contracts.InvocationKind.AT_MOST_ONCE
import kotlin.contracts.contract

/**
 * Represents a container containing either an instance of type [A] ([Left])
 * or [B] ([Right]).
 */
public sealed interface Either<out A, out B> {
    public data class Left<out A, out B>(public val left: A) : Either<A, B>

    public data class Right<out A, out B>(public val right: B) : Either<A, B>
}

/**
 * Returns the encapsulated [Left.left] value if this instance represents [Left] or throws a [NoSuchElementException] otherwise.
 *
 * This function is a shorthand for `leftOrElse { throw it }` (see [leftOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <A, B> Either<A, B>.leftOrThrow(): A = when (this) {
    is Left -> left
    else -> throw NoSuchElementException()
}

/**
 * Returns the encapsulated [Right.right] value if this instance represents [Right] or throws a [NoSuchElementException] otherwise.
 *
 * This function is a shorthand for `rightOrElse { throw it }` (see [rightOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <A, B> Either<A, B>.rightOrThrow(): B = when (this) {
    is Right -> right
    else -> throw NoSuchElementException()
}

/**
 * Returns the encapsulated [Left.left] value if this instance represents [Left] or the
 * result of [onRight] function for the encapsulated [Right.right] value otherwise.
 *
 * This function is a shorthand for `fold(onLeft = { it }, onRight = onRight)` (see [fold]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A : R, B> Either<A, B>.leftOrElse(onRight: (B) -> R): R {
    contract {
        callsInPlace(onRight, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> left
        is Right -> onRight(right)
    }
}

/**
 * Returns the encapsulated [Right.right] value if this instance represents [Right] or the
 * result of [onLeft] function for the encapsulated [Left.left] value otherwise.
 *
 * This function is a shorthand for `fold(onLeft = onLeft, onRight = { it })` (see [fold]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A, B : R> Either<A, B>.rightOrElse(onLeft: (A) -> R): R {
    contract {
        callsInPlace(onLeft, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> onLeft(left)
        is Right -> right
    }
}

/**
 * Returns the encapsulated [Left.left] value if this instance represents [Left] or the
 * [defaultValue] if it is [Right].
 *
 * This function is a shorthand for `leftOrElse { defaultValue }` (see [leftOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A : R> Either<A, *>.leftOrDefault(defaultValue: R): R = when (this) {
    is Left -> left
    is Right -> defaultValue
}

/**
 * Returns the encapsulated [Right.right] value if this instance represents [Right] or the
 * [defaultValue] if it is [Left].
 *
 * This function is a shorthand for `rightOrElse { defaultValue }` (see [rightOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, B : R> Either<*, B>.rightOrDefault(defaultValue: R): R = when (this) {
    is Left -> defaultValue
    is Right -> right
}

/**
 * Returns the encapsulated [Left.left] value if this instance represents [Left] or
 * `null` if it is [Right].
 *
 * This function is a shorthand for `leftOrElse { null }` (see [leftOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <R, A : R> Either<A, *>.leftOrNull(): R? = when (this) {
    is Left -> left
    is Right -> null
}

/**
 * Returns the encapsulated [Right.right] value if this instance represents [Right] or
 * `null`  if it is [Left].
 *
 * This function is a shorthand for `rightOrElse { null }` (see [rightOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <R, B : R> Either<*, B>.rightOrNull(): R? = when (this) {
    is Left -> null
    is Right -> right
}

/**
 * Returns the result of [onLeft] for the encapsulated instance [A] or
 * the result of [onRight] for the encapsulated instance [B].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <A, B, R> Either<A, B>.fold(
    onLeft: (A) -> R,
    onRight: (B) -> R,
): R {
    contract {
        callsInPlace(onLeft, AT_MOST_ONCE)
        callsInPlace(onRight, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> onLeft(left)
        is Right -> onRight(right)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [Left] or the
 * original encapsulated [Right] value if it is [Right].
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A, B> Either<A, B>.mapLeft(transform: (A) -> R): Either<R, B> {
    contract {
        callsInPlace(transform, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> Left(transform(left))
        is Right -> @Suppress("UNCHECKED_CAST") (this as Either<R, B>)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [Right] or the
 * original encapsulated [Left] value if it is [Left].
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A, B> Either<A, B>.mapRight(transform: (B) -> R): Either<A, R> {
    contract {
        callsInPlace(transform, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> @Suppress("UNCHECKED_CAST") (this as Either<A, R>)
        is Right -> Right(transform(right))
    }
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [Left].
 * Returns the original `Either` unchanged.
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <A, B> Either<A, B>.onLeft(action: (A) -> Unit): Either<A, B> {
    contract {
        callsInPlace(action, AT_MOST_ONCE)
    }
    if (this is Left) action(left)
    return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [Right].
 * Returns the original `Either` unchanged.
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <A, B> Either<A, B>.onRight(action: (B) -> Unit): Either<A, B> {
    contract {
        callsInPlace(action, AT_MOST_ONCE)
    }
    if (this is Right) action(right)
    return this
}

/** Alias for [Left.left] */
public inline val <A, B> Left<A, B>.value: A get() = left

/** Alias for [Right.right] */
public inline val <A, B> Right<A, B>.value: B get() = right

/**
 * Converts this [Either] to a [Result].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> Either<T, Throwable>.asResult(): Result<T> =
    mapLeft { Result.success(it) } leftOrElse { Result.failure(it) }

/**
 * Converts this [Result] to an [Either].
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> Result<T>.asEither(): Either<T, Throwable> =
    fold({ Left(it) }, { Right(it) })
