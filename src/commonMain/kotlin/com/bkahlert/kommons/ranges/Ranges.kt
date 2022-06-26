package com.bkahlert.kommons.ranges


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
