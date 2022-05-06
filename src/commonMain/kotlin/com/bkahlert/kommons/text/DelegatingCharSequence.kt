package com.bkahlert.kommons.text

import com.bkahlert.kommons.ranges.size

/**
 * A [CharSequence] that delegates to the specified [delegate]
 * optionally sub-sequenced with the specified [range].
 */
public class DelegatingCharSequence(
    private val delegate: CharSequence,
    private val range: IntRange? = null,
) : CharSequence {
    override val length: Int
        get() = range?.size ?: delegate.length

    private inline val start: Int get() = range?.start ?: 0

    override fun get(index: Int): Char {
        if (!indices.contains(index)) {
            throw IndexOutOfBoundsException("index out of range: $index")
        }
        return delegate[start + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        if (endIndex < startIndex || with(0..length) { !contains(startIndex) || !contains(endIndex) }) {
            throw IndexOutOfBoundsException("begin $startIndex, end $endIndex, length $length")
        }
        return DelegatingCharSequence(delegate, start.let { (it + startIndex).until(it + endIndex) })
    }

    override fun toString(): String =
        range?.let { delegate.substring(it) } ?: delegate.toString()
}
