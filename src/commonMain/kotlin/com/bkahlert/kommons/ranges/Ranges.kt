package com.bkahlert.kommons.ranges

/**
 * Maps this [ClosedRange] with elements of type [T]
 * to a [ClosedRange] with elements of type [R] by applying
 * the specified [transform] to the [ClosedRange.start] and
 * [ClosedRange.endInclusive].
 */
public inline fun <reified T : Comparable<T>, reified R : Comparable<R>> ClosedRange<T>.map(
    transform: T.() -> R,
): ClosedRange<R> = start.transform()..endInclusive.transform()


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
