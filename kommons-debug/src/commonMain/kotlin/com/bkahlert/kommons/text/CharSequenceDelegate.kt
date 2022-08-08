package com.bkahlert.kommons.text

/**
 * A [CharSequence] that delegates to the specified [text]
 * optionally sub-sequenced with the specified [range].
 */
public class CharSequenceDelegate(
    private val text: CharSequence,
    private val startIndex: Int = 0,
    private val endIndex: Int = -1,
) : CharSequence {
    public constructor(
        text: CharSequence,
        range: IntRange,
    ) : this(text, range.first, range.last + 1)

    private val Int.abs get() = if (this < 0) text.length + this + 1 else this

    override val length: Int get() = endIndex.abs - startIndex

    override fun get(index: Int): kotlin.Char {
        checkBoundsIndex(indices, index)
        return text[startIndex + index]
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        checkBoundsIndexes(length, startIndex, endIndex)
        if (startIndex == endIndex) return String.EMPTY
        if (startIndex == 0 && endIndex == text.length) return this
        return CharSequenceDelegate(text, this.startIndex + startIndex, this.startIndex + endIndex)
    }

    /** Returns the [text] sub-sequenced with the [range]. */
    override fun toString(): String {

        return text.subSequence(checkBoundsIndexes(text.length, startIndex, endIndex.abs)).toString()
    }

    /** Returns `true` if the specified [other] is a [CharSequenceDelegate] and the return values of [toString] are equal. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CharSequenceDelegate

        return toString() == other.toString()
    }

    /** Returns the hash code of [toString]. */
    override fun hashCode(): Int = toString().hashCode()
}
