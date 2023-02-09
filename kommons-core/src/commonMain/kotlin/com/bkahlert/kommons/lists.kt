package com.bkahlert.kommons

/** Shortcut for `subList(range.first, range.last+1)` */
@Suppress("NOTHING_TO_INLINE")
public inline fun <T> List<T>.subList(range: IntRange): List<T> = subList(range.first, range.end)

/**
 * Returns a new list wrapping this list with the following differences:
 * 1) Negative indices are supported and start from the end of this list.
 *    For example `this[-1]` returns the last element, `this[-2]` returns the second, ...
 * 2) Modulus operation is applied, that is,
 *    `listOf("a","b","c").withNegativeIndices(4)` returns `a`. `this[-4]` would return `c`.
 */
public fun <T> List<T>.withNegativeIndices(): List<T> =
    object : List<T> by this {
        override fun get(index: Int): T = this@withNegativeIndices[index.mod(size)]
    }


/**
 * Returns a list that contains the predecessor for each element the specified [predicate] returns `true`.
 */
public fun <T> List<T>.predecessor(predicate: (T) -> Boolean): List<T> {
    if (isEmpty()) return this
    return mapIndexedNotNull { index, element ->
        if (predicate(element)) {
            if (index > 0) get(index - 1) else get(size - 1)
        } else {
            null
        }
    }
}

/**
 * Returns a list that contains one successor element for each element the specified [predicate] returns `true`.
 */
public fun <T> List<T>.successor(predicate: (T) -> Boolean): List<T> {
    if (isEmpty()) return this
    val limit = size - 1
    return mapIndexedNotNull { index, element ->
        if (predicate(element)) {
            if (index < limit) get(index + 1) else get(0)
        } else {
            null
        }
    }
}
