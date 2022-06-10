package com.bkahlert.kommons.ranges

import com.bkahlert.kommons.math.BigDecimal
import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.minus

/**
 * Maps this [ClosedRange] with elements of type [T]
 * to a [ClosedRange] with elements of type [R] by applying
 * the specified [transform] to the [ClosedRange.start] and
 * [ClosedRange.endInclusive].
 */
public inline fun <reified T : Comparable<T>, reified R : Comparable<R>> ClosedRange<T>.map(
    transform: T.() -> R,
): ClosedRange<R> = start.transform()..endInclusive.transform()

/** The difference between the [ClosedRange.start] and [ClosedRange.endInclusive]. */
public inline val ClosedRange<Int>.difference: Int get() = endInclusive - start

/** The difference between the [ClosedRange.start] and [ClosedRange.endInclusive]. */
public inline val ClosedRange<Long>.difference: Long get() = endInclusive - start

/** The difference between the [ClosedRange.start] and [ClosedRange.endInclusive]. */
public inline val ClosedRange<Double>.difference: Double get() = endInclusive - start

/** The difference between the [ClosedRange.start] and [ClosedRange.endInclusive]. */
public inline val ClosedRange<BigDecimal>.difference: BigDecimal get() = endInclusive - start

/** The difference between the [ClosedRange.start] and [ClosedRange.endInclusive]. */
public inline val ClosedRange<BigInteger>.difference: BigInteger get() = endInclusive - start


/**
 * Returns a [ClosedRange] with [ClosedRange.start] less than or equal to [ClosedRange.endInclusive]
 * by returning this range if that condition is already true,
 * or a copy of this range with [ClosedRange.start] and [ClosedRange.endInclusive] swapped.
 */
public inline fun <reified T : Comparable<T>> ClosedRange<T>.sort(): ClosedRange<T> = if (start > endInclusive) endInclusive..start else this


/**
 * Contains the absolute difference between the [ClosedRange.start]
 * and [ClosedRange.endInclusive] `+1`.
 */
public inline val <reified T> ClosedRange<T>.size: Int
    where T : Comparable<T>, T : Number
    get() {
        val x = start.toInt()
        val y = endInclusive.toInt()
        return if (x > y) x - y + 1 else y - x + 1
    }
