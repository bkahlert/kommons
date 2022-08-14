package com.bkahlert.kommons

/** Represents a quartet of values. */
public data class Quadruple<out A, out B, out C, out D>(
    /** The first value. */
    public val first: A,
    /** The second value. */
    public val second: B,
    /** The third value. */
    public val third: C,
    /** The fourth value. */
    public val fourth: D,
)

/** Represents a quintet of values. */
public data class Quintuple<out A, out B, out C, out D, out E>(
    /** The first value. */
    public val first: A,
    /** The second value. */
    public val second: B,
    /** The third value. */
    public val third: C,
    /** The fourth value. */
    public val fourth: D,
    /** The fifth value. */
    public val fifth: E,
)

/** Creates a tuple of type [Triple] from this [Pair] and [that]. */
public infix fun <A, B, C> Pair<A, B>.too(that: C): Triple<A, B, C> =
    Triple(first, second, that)

/** Creates a tuple of type [Quadruple] from `this` [Triple] and [that]. */
public infix fun <A, B, C, D> Triple<A, B, C>.too(that: D): Quadruple<A, B, C, D> =
    Quadruple(first, second, third, that)

/** Creates a tuple of type [Quintuple] from `this` [Quadruple] and [that]. */
public infix fun <A, B, C, D, E> Quadruple<A, B, C, D>.too(that: E): Quintuple<A, B, C, D, E> =
    Quintuple(first, second, third, fourth, that)

/**
 * Returns a pair containing the results of applying the given [transform] function
 * to each element in the original pair.
 */
public fun <T, A : T, B : T, R> Pair<A, B>.map(transform: (T) -> R): Pair<R, R> =
    Pair(transform(first), transform(second))

/**
 * Returns a triple containing the results of applying the given [transform] function
 * to each element in the original triple.
 */
public fun <T, A : T, B : T, C : T, R> Triple<A, B, C>.map(transform: (T) -> R): Triple<R, R, R> =
    Triple(transform(first), transform(second), transform(third))

/**
 * Returns a quadruple containing the results of applying the given [transform] function
 * to each element in the original quadruple.
 */
public fun <T, A : T, B : T, C : T, D : T, R> Quadruple<A, B, C, D>.map(transform: (T) -> R): Quadruple<R, R, R, R> =
    Quadruple(transform(first), transform(second), transform(third), transform(fourth))

/**
 * Returns a quintuple containing the results of applying the given [transform] function
 * to each element in the original quintuple.
 */
public fun <T, A : T, B : T, C : T, D : T, E : T, R> Quintuple<A, B, C, D, E>.map(transform: (T) -> R): Quintuple<R, R, R, R, R> =
    Quintuple(transform(first), transform(second), transform(third), transform(fourth), transform(fifth))
