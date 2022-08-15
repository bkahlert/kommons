package com.bkahlert.kommons

/** Returns a [List] containing all elements. */
public fun <T> Iterator<T>.toList(): List<T> = asSequence().toList()

/**
 * Returns an iterator yielding the results of applying the given [transform] function
 * to each element in the original iterator.
 */
public fun <T, R> Iterator<T>.map(transform: (T) -> R): Iterator<R> =
    object : Iterator<R> {
        override fun hasNext(): Boolean = this@map.hasNext()
        override fun next(): R = transform(this@map.next())
    }

/**
 * Returns an iterator that yield ranges with each element ranging from the `predecessor+1..current-1`
 * and the first element starting with the specified [start].
 */
public fun Iterator<Int>.mapToRanges(start: Int = 0): Iterator<IntRange> {
    var prev = start
    return map { prev until it.also { prev = it } }
}
