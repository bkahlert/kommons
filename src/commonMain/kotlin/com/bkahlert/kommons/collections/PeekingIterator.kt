package com.bkahlert.kommons.collections

/**
 * An iterator than supports the [peek] and [peekOrNull] operation.
 */
public class PeekingIterator<T>(
    private val iterator: Iterator<T>,
) : Iterator<T> {
    private val readAhead = mutableListOf<T>()

    /**
     * Reads as many elements as necessary to resolve the provided [index] and
     * returns the element at that position. If less elements are available, `null` returned.
     */
    public fun peekOrNull(index: Int = 0): T? {
        if (!readAhead(index)) return null
        return readAhead[index]
    }

    /**
     * Reads as many elements as necessary to resolve the provided [index] and
     * returns the element at that position. If less elements are available, [IndexOutOfBoundsException] is thrown.
     */
    public fun peek(index: Int = 0): T {
        if (!readAhead(index)) throw IndexOutOfBoundsException()
        return readAhead[index]
    }

    /**
     * Reads as many elements as necessary to peek the element
     * with the specified [index] and returns whether enough elements could be read.
     */
    private fun readAhead(index: Int): Boolean {
        for (i in 0..index - readAhead.size) {
            if (iterator.hasNext()) readAhead.add(iterator.next())
            else return false
        }
        return true
    }

    override fun next(): T =
        if (readAhead.isNotEmpty()) {
            readAhead.removeFirst()
        } else {
            iterator.next()
        }

    override fun hasNext(): Boolean = readAhead.isNotEmpty() || iterator.hasNext()
}

/**
 * Returns an [PeekingIterator] that returns the values from `this` sequence.
 */
public fun <T> Sequence<T>.peekingIterator(): PeekingIterator<T> = PeekingIterator(iterator())

/**
 * Returns an [PeekingIterator] that returns the values from `this` iterable.
 */
public fun <T> Iterable<T>.peekingIterator(): PeekingIterator<T> = PeekingIterator(iterator())
