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
    /** Represents a container containing the left value. */
    public data class Left<out A, out B>(
        /** The actual left value. */
        public val value: A
    ) : Either<A, B>

    /** Represents a container containing the right value. */
    public data class Right<out A, out B>(
        /** The actual right value. */
        public val value: B
    ) : Either<A, B>
}

/**
 * Returns the encapsulated [Left.value] value if this instance represents [Left] or throws a [NoSuchElementException] otherwise.
 *
 * This function is a shorthand for `getLeftOrElse { throw it }` (see [getLeftOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <A, B> Either<A, B>.getLeftOrThrow(): A = when (this) {
    is Left -> value
    else -> throw NoSuchElementException()
}

/**
 * Returns the encapsulated [Right.value] value if this instance represents [Right] or throws a [NoSuchElementException] otherwise.
 *
 * This function is a shorthand for `getRightOrElse { throw it }` (see [getRightOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <A, B> Either<A, B>.getRightOrThrow(): B = when (this) {
    is Right -> value
    else -> throw NoSuchElementException()
}

/**
 * Returns the encapsulated [Left.value] value if this instance represents [Left] or the
 * result of [onRight] function for the encapsulated [Right.value] value otherwise.
 *
 * This function is a shorthand for `fold(onLeft = { it }, onRight = onRight)` (see [fold]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A : R, B> Either<A, B>.getLeftOrElse(onRight: (B) -> R): R {
    contract {
        callsInPlace(onRight, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> value
        is Right -> onRight(value)
    }
}

/**
 * Returns the encapsulated [Right.value] value if this instance represents [Right] or the
 * result of [onLeft] function for the encapsulated [Left.value] value otherwise.
 *
 * This function is a shorthand for `fold(onLeft = onLeft, onRight = { it })` (see [fold]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A, B : R> Either<A, B>.getRightOrElse(onLeft: (A) -> R): R {
    contract {
        callsInPlace(onLeft, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> onLeft(value)
        is Right -> value
    }
}

/**
 * Returns the encapsulated [Left.value] value if this instance represents [Left] or the
 * [defaultValue] if it's [Right].
 *
 * This function is a shorthand for `getLeftOrElse { defaultValue }` (see [getLeftOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A : R> Either<A, *>.getLeftOrDefault(defaultValue: R): R = when (this) {
    is Left -> value
    is Right -> defaultValue
}

/**
 * Returns the encapsulated [Right.value] value if this instance represents [Right] or the
 * [defaultValue] if it's [Left].
 *
 * This function is a shorthand for `getRightOrElse { defaultValue }` (see [getRightOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, B : R> Either<*, B>.getRightOrDefault(defaultValue: R): R = when (this) {
    is Left -> defaultValue
    is Right -> value
}

/**
 * Returns the encapsulated [Left.value] value if this instance represents [Left] or
 * `null` if it's [Right].
 *
 * This function is a shorthand for `getLeftOrElse { null }` (see [getLeftOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <R, A : R> Either<A, *>.getLeftOrNull(): R? = when (this) {
    is Left -> value
    is Right -> null
}

/**
 * Returns the encapsulated [Right.value] value if this instance represents [Right] or
 * `null` if it's [Left].
 *
 * This function is a shorthand for `getRightOrElse { null }` (see [getRightOrElse]).
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun <R, B : R> Either<*, B>.getRightOrNull(): R? = when (this) {
    is Left -> null
    is Right -> value
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
        is Left -> onLeft(value)
        is Right -> onRight(value)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [Left] or the
 * original encapsulated [Right] value if it's [Right].
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A, B> Either<A, B>.mapLeft(transform: (A) -> R): Either<R, B> {
    contract {
        callsInPlace(transform, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> Left(transform(value))
        is Right -> @Suppress("UNCHECKED_CAST") (this as Either<R, B>)
    }
}

/**
 * Returns the encapsulated result of the given [transform] function applied to the encapsulated value
 * if this instance represents [Right] or the
 * original encapsulated [Left] value if it's [Left].
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <R, A, B> Either<A, B>.mapRight(transform: (B) -> R): Either<A, R> {
    contract {
        callsInPlace(transform, AT_MOST_ONCE)
    }
    return when (this) {
        is Left -> @Suppress("UNCHECKED_CAST") (this as Either<A, R>)
        is Right -> Right(transform(value))
    }
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [Left].
 * Returns the original [Either] unchanged.
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <A, B> Either<A, B>.onLeft(action: (A) -> Unit): Either<A, B> {
    contract {
        callsInPlace(action, AT_MOST_ONCE)
    }
    if (this is Left) action(value)
    return this
}

/**
 * Performs the given [action] on the encapsulated value if this instance represents [Right].
 * Returns the original [Either] unchanged.
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun <A, B> Either<A, B>.onRight(action: (B) -> Unit): Either<A, B> {
    contract {
        callsInPlace(action, AT_MOST_ONCE)
    }
    if (this is Right) action(value)
    return this
}

/** Converts this [Either] to a [Result]. */
@Suppress("NOTHING_TO_INLINE", "DirectUseOfResultType")
public inline fun <T> Either<T, Throwable>.toResult(): Result<T> =
    mapLeft { Result.success(it) } getLeftOrElse { Result.failure(it) }

/** Converts this [Result] to an [Either]. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> Result<T>.toEither(): Either<T, Throwable> =
    fold({ Left(it) }, { Right(it) })

/**
 * Returns this result's value if it represents [Result.success] and
 * the caught [Throwable] if it represents [Result.failure].
 */
public fun <T> Result<T>.getOrException(): Pair<T?, Throwable?> =
    fold({ it to null }, { null to it })
