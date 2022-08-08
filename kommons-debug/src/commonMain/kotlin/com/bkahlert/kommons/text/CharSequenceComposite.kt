package com.bkahlert.kommons.text

import com.bkahlert.kommons.locate

/** A [CharSequence] that acts as if the specified [texts] were concatenated. */
public class CharSequenceComposite(
    private val texts: List<CharSequence>,
) : CharSequence {
    public constructor(vararg texts: CharSequence) : this(texts.asList())

    override val length: Int get() = texts.sumOf { it.length }

    override fun get(index: Int): kotlin.Char {
        checkBoundsIndex(indices, index)
        val (delegateIndex, localIndex) = texts.locate(index, CharSequence::length)
        return texts[delegateIndex][localIndex]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        checkBoundsIndexes(length, startIndex, endIndex)
        if (startIndex == endIndex) return String.EMPTY
        if (startIndex == 0 && endIndex == length) return this
        val (elementRange, virtualStartIndex, virtualEndIndex) = texts.locate(startIndex, endIndex, CharSequence::length)
        return elementRange.singleOrNull()
            ?.let { CharSequenceDelegate(texts[it], startIndex = virtualStartIndex, endIndex = virtualEndIndex) }
            ?: CharSequenceComposite(elementRange.map {
                CharSequenceDelegate(
                    texts[it],
                    startIndex = if (elementRange.first == it) virtualStartIndex else 0,
                    endIndex = if (elementRange.last == it) virtualEndIndex else -1,
                )
            })
    }

    /** Returns the [texts] concatenated to a string. */
    override fun toString(): String = texts.joinToString(String.EMPTY)

    /** Returns `true` if the specified [other] is a [CharSequenceComposite] and the return values of [toString] are equal. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CharSequenceComposite

        return toString() == other.toString()
    }

    /** Returns the hash code of [toString]. */
    override fun hashCode(): Int = toString().hashCode()
}
