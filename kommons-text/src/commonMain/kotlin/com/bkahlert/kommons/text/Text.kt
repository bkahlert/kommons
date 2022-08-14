package com.bkahlert.kommons.text

import com.bkahlert.kommons.EMPTY
import com.bkahlert.kommons.locate
import com.bkahlert.kommons.map
import com.bkahlert.kommons.mapToRanges
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.text.Text.ChunkedText
import com.bkahlert.kommons.text.Text.TextComposite
import com.bkahlert.kommons.toList
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.math.roundToInt

/** An [Iterator] that iterates text boundaries. */
public typealias BreakIterator = Iterator<Int>

/** A text consisting of chunks of type [T]. */
public interface Text<out T> : Iterable<Text<T>> {

    /** The of this text. */
    public val length: Int

    /**
     * Returns the chunk at the specified [index] in this text.
     *
     * @throws [IndexOutOfBoundsException] if the [index] is out of bounds of this character sequence.
     */
    public operator fun get(index: Int): T

    /**
     * Returns a character sequence representing the text that is a subsequence of this text,
     * starting at the specified [startIndex] and ending right before the specified [endIndex].
     *
     * @param startIndex the start index (inclusive).
     * @param endIndex the end index (exclusive).
     */
    public fun subSequence(startIndex: Int = 0, endIndex: Int = length): CharSequence

    /**
     * Returns a new text that is a subsequence of this text,
     * starting at the specified [startIndex] and ending right before the specified [endIndex].
     */
    public fun subText(startIndex: Int = 0, endIndex: Int = length): Text<T>

    /** Returns an iterator that iterates over the sub texts of this text. */
    override fun iterator(): Iterator<Text<T>>

    /** Returns a list containing the chunks this text consists of. */
    public fun asList(): List<T>

    public companion object {

        /** Returns an empty text. */
        public fun <T> emptyText(): Text<T> = EmptyText

        /** Returns a text consisting of text chunks of type [T] using the specified [unit]. */
        public fun <T> CharSequence.asText(unit: TextUnit<T>): Text<T> = unit.textOf(this)

        /** Maps the [unit]-based [Text] of this character sequence using the specified [transform]. */
        public fun <T> CharSequence.mapText(
            unit: TextUnit<T>,
            transform: (Text<T>) -> Text<*>,
        ): CharSequence = mapText(unit, transform) { it.subSequence() }

        /** Maps the [unit]-based [Text] of this string using the specified [transform]. */
        public fun <T> String.mapText(
            unit: TextUnit<T>,
            transform: (Text<T>) -> Text<*>,
        ): String = mapText(unit, transform) { it.subSequence().toString() }

        private fun <T, R : CharSequence> R.mapText(
            unit: TextUnit<T>,
            transform: (Text<T>) -> Text<*>,
            join: (Text<*>) -> R,
        ): R {
            val text: Text<T> = asText(unit)
            return transform(text).takeUnless { it == text }?.let(join) ?: this
        }
    }

    private object EmptyText : Text<Nothing> {
        override val length: Int get() = 0
        override fun get(index: Int): Nothing = throw IndexOutOfBoundsException("index out of range: $index")
        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = String.EMPTY.also { checkBoundsIndexes(0, startIndex, endIndex) }
        override fun subText(startIndex: Int, endIndex: Int): Text<Nothing> = EmptyText.also { checkBoundsIndexes(0, startIndex, endIndex) }
        override fun iterator(): Iterator<Text<Nothing>> = emptyList<Text<Nothing>>().iterator()
        override fun asList(): List<Nothing> = emptyList()
        override fun toString(): String = String.EMPTY
    }

    /**
     * A text that is backed by the specified [text],
     * its chunks based the on the specified [indices],
     * and materialized using the specified [transform].
     */
    public class ChunkedText<T>(
        private val text: CharSequence,
        private val indices: List<IntRange>,
        private val transform: (CharSequence, IntRange) -> T,
    ) : Text<T> {
        public constructor(
            text: CharSequence,
            vararg indices: IntRange,
            transform: (CharSequence, IntRange) -> T,
        ) : this(text, indices.asList(), transform)

        public constructor(
            text: CharSequence,
            iterator: BreakIterator,
            transform: (CharSequence, IntRange) -> T,
        ) : this(text, iterator.mapToRanges().toList(), transform)

        override val length: Int get() = indices.size

        override fun get(index: Int): T {
            checkBoundsIndex(indices.indices, index)
            return transform(text, indices[index])
        }

        private val fullyIndexed get() = indices.fold(0) { last, range -> if (range.first == last) range.last + 1 else -1 } == text.length

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            checkBoundsIndexes(length, startIndex, endIndex)
            if (startIndex == endIndex) return String.EMPTY
            if (fullyIndexed && startIndex == 0 && endIndex == length) return text
            return buildString { this@ChunkedText.indices.subList(startIndex, endIndex).forEach { append(text, it.first, it.last + 1) } }
        }

        override fun subText(startIndex: Int, endIndex: Int): Text<T> {
            checkBoundsIndexes(length, startIndex, endIndex)
            if (startIndex == endIndex) return EmptyText
            if (fullyIndexed && startIndex == 0 && endIndex == length) return this
            return ChunkedText(text, indices.subList(startIndex, endIndex), transform)
        }

        override fun iterator(): Iterator<Text<T>> = indices.indices.iterator().map { subText(it, it + 1) }

        override fun asList(): List<T> = indices.map { transform(text, it) }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ChunkedText<*>

            if (text != other.text) return false
            if (indices != other.indices) return false

            return true
        }

        override fun hashCode(): Int {
            var result = text.hashCode()
            result = 31 * result + indices.hashCode()
            return result
        }

        override fun toString(): String = subSequence().toString()
    }

    /** A [Text] that acts as if the specified [texts] were concatenated. */
    public class TextComposite<out T>(
        private val texts: List<Text<T>>,
    ) : Text<T> {
        public constructor(vararg texts: Text<T>) : this(texts.asList())

        override val length: Int get() = texts.sumOf { it.length }

        override fun get(index: Int): T {
            checkBoundsIndex(0..length, index)
            val (delegateIndex, localIndex) = texts.locate(index, Text<T>::length)
            return texts[delegateIndex][localIndex]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            checkBoundsIndexes(length, startIndex, endIndex)
            if (startIndex == endIndex) return String.EMPTY
            if (startIndex == 0 && endIndex == length) CharSequenceComposite(texts.map { it.subSequence() })
            val (elementRange, virtualStartIndex, virtualEndIndex) = texts.locate(startIndex, endIndex, Text<T>::length)
            return elementRange.singleOrNull()
                ?.let { texts[it].subSequence(virtualStartIndex, virtualEndIndex) }
                ?: CharSequenceComposite(elementRange.map {
                    texts[it].let { text ->
                        text.subSequence(
                            if (elementRange.first == it) virtualStartIndex else 0,
                            if (elementRange.last == it) virtualEndIndex else text.length,
                        )
                    }
                })
        }

        override fun subText(startIndex: Int, endIndex: Int): Text<T> {
            checkBoundsIndexes(length, startIndex, endIndex)
            if (startIndex == endIndex) return EmptyText
            if (startIndex == 0 && endIndex == length) return this
            val (elementRange, virtualStartIndex, virtualEndIndex) = texts.locate(startIndex, endIndex, Text<T>::length)
            return elementRange.singleOrNull()
                ?.let { texts[it].subText(virtualStartIndex, virtualEndIndex) }
                ?: TextComposite(elementRange.map {
                    texts[it].let { text ->
                        text.subText(
                            if (elementRange.first == it) virtualStartIndex else 0,
                            if (elementRange.last == it) virtualEndIndex else text.length,
                        )
                    }
                })
        }

        override fun iterator(): Iterator<Text<T>> = iterator {
            texts.forEach { text -> text.iterator().forEach { yield(it) } }
        }

        override fun asList(): List<T> = texts.flatMap { it.asList() }

        /** Returns the [texts] concatenated string. */
        override fun toString(): String = texts.joinToString(String.EMPTY)

        /** Returns `true` if the specified [other] is a [TextComposite] and the return values of [toString] are equal. */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as TextComposite<*>

            return toString() == other.toString()
        }

        /** Returns the hash code of [toString]. */
        override fun hashCode(): Int = toString().hashCode()
    }
}

/** Returns `true` if the collection is empty, `false` otherwise. */
public fun Text<*>.isEmpty(): Boolean = length == 0.also { emptyList<Nothing>().isEmpty() }

/** Returns a new text consisting of the text chunks of this and the specified [other] text. */
public operator fun <T> Text<T>.plus(other: Text<T>): Text<T> = when {
    this.isEmpty() -> other
    other.isEmpty() -> this
    else -> TextComposite(this, other)
}

/** The type of text chunks. */
public interface TextUnit<T> {

    /** The unit name. */
    public val name: String

    /** Returns a new [Text] backed by the specified [text]. */
    public fun textOf(text: CharSequence): Text<T>

    /** Returns a new [TextLength] equal to the specified [length]. */
    public fun lengthOf(length: Int): TextLength<T>
}

/** A [TextUnit] of which the text chunks are created consecutively using a [BreakIterator]. */
public abstract class ChunkingTextUnit<T>(
    final override val name: String,
) : TextUnit<T> {
    /** Returns a [BreakIterator] that is used to chunk the specified [text]. */
    protected abstract fun chunk(text: CharSequence): BreakIterator

    /** Materializes the specified [text] at the specified [range] to an instance of [T]. */
    protected abstract fun transform(text: CharSequence, range: IntRange): T

    final override fun textOf(text: CharSequence): Text<T> = if (text.isEmpty()) Text.emptyText() else ChunkedText(text, chunk(text), ::transform)
    final override fun lengthOf(length: Int): TextLength<T> = TextLength(length, this)
    final override fun toString(): String = name
}

/** Represents the length of text in a given [TextUnit]. */
public class TextLength<T>(
    /** The length of text measured in [unit]. */
    public val value: Int,
    /** The unit of this text length. */
    public val unit: TextUnit<T>,
) : Comparable<TextLength<T>> {

    public override fun compareTo(other: TextLength<T>): Int = value.compareTo(other.value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TextLength<*>

        if (value != other.value) return false
        if (unit != other.unit) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun toString(): String = when (value) {
        1 -> "$value ${unit.name}"
        else -> "$value ${unit.name}s"
    }


    // arithmetic operators

    /** Returns the negative of this value. */
    public operator fun unaryMinus(): TextLength<T> = TextLength(-value, unit)

    /** Returns a text length whose value is the sum of this text length value and the [other] value. */
    public operator fun plus(other: Int): TextLength<T> = TextLength(value + other, unit)

    /** Returns a text length whose value is the sum of this and the [other] text length value. */
    public operator fun plus(other: TextLength<T>): TextLength<T> = this + other.value

    /** Returns a text length whose value is the difference between this text length value and the [other] value. */
    public operator fun minus(other: Int): TextLength<T> = this + (-other)

    /** Returns a text length whose value is the difference between this and the [other] text length. */
    public operator fun minus(other: TextLength<T>): TextLength<T> = this - other.value

    /** Returns a text length whose value is this text length value multiplied by the given [scale] number. */
    public operator fun times(scale: Int): TextLength<T> = TextLength(value * scale, unit)

    /**
     * Returns a text length whose value is this text length value multiplied by the given [scale] number.
     *
     * The operation may involve rounding when the result can't be represented exactly with a [Double] number.
     */
    public operator fun times(scale: Double): TextLength<T> {
        val intScale = scale.roundToInt()
        if (intScale.toDouble() == scale) {
            return times(intScale)
        }

        val result = value.toDouble() * scale
        return TextLength(result.toInt(), unit)
    }

    /**
     * Returns a text length whose value is this text length value divided by the given [scale] number.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * for example, when dividing text length by zero.
     */
    public operator fun div(scale: Int): TextLength<T> {
        if (scale == 0) throw IllegalArgumentException("Dividing text length by zero yields an undefined result.")
        return TextLength(value / scale, unit)
    }

    /**
     * Returns a text length whose value is this text length value divided by the given [scale] number.
     *
     * @throws IllegalArgumentException if the operation results in an undefined value for the given arguments,
     * for example, when dividing an infinite text length by infinity or text length by zero.
     */
    public operator fun div(scale: Double): TextLength<T> {
        val intScale = scale.roundToInt()
        if (intScale.toDouble() == scale && intScale != 0) {
            return div(intScale)
        }

        val result = value.toDouble() / scale
        return TextLength(result.toInt(), unit)
    }

    /** Returns a number that is the ratio of this and the [other] text length value. */
    public operator fun div(other: TextLength<T>): Double = value.toDouble() / other.value.toDouble()

    /** Returns true, if the text length value is less than zero. */
    public fun isNegative(): Boolean = value < 0

    /** Returns true, if the text length value is greater than zero. */
    public fun isPositive(): Boolean = value > 0

    /** Returns the absolute value of this value. The returned value is always non-negative. */
    public val absoluteValue: TextLength<T> get() = if (isNegative()) -this else this
}

/** Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the specified [n] is negative. */
public fun requireNotNegative(
    n: Int,
    lazyMessage: () -> Any = { "Requested text unit count $n is less than zero." }
): Int = n.also { require(n >= 0, lazyMessage) }


/**
 * Builds a new [Text] by populating a [MutableList]
 * using the given [builderAction] and
 * returning a [Text] encompassing the same elements.
 *
 * The list passed as a receiver to the [builderAction]
 * is valid only inside that function.
 */
public inline fun <T> buildText(builderAction: MutableList<Text<T>>.() -> Unit): Text<T> {
    contract { callsInPlace(builderAction, EXACTLY_ONCE) }
    return TextComposite(mutableListOf<Text<T>>().apply(builderAction))
}


/**
 * Returns a subsequence of this character sequence containing the first [n] units of the reified [TextUnit] from this character sequence,
 * or the entire character sequence if this character sequence is shorter.
 *
 * @throws IllegalArgumentException if [n] is negative.
 */
public fun <T> Text<T>.take(n: Int): Text<T> {
    requireNotNegative(n)
    return subText(endIndex = n.coerceAtMost(length))
}

/**
 * Returns a subsequence of this character sequence containing the last [n] units of the reified [TextUnit] from this character sequence,
 * or the entire character sequence if this character sequence is shorter.
 *
 * @throws IllegalArgumentException if [n] is negative.
 */
public fun <T> Text<T>.takeLast(n: Int): Text<T> {
    requireNotNegative(n)
    val length = length
    return subText(startIndex = length - n.coerceAtMost(length))
}


private fun targetLengthFor(length: Int, markerText: Text<*>): Int {
    requireNotNegative(length)
    require(length >= markerText.length) {
        "The specified length ($length) must be greater or equal than the length of the marker ${markerText.quoted} (${markerText.length})."
    }
    return length - markerText.length
}

/**
 * Returns this text truncated from the center to the specified [length]
 * including the [marker].
 */
public fun <T> Text<T>.truncate(length: Int, marker: Text<T>): Text<T> {
    requireNotNegative(length)
    if (this.length <= length) return this
    val targetLength = targetLengthFor(length, marker)
    val left = truncateEnd(-(-targetLength).floorDiv(2), marker = Text.emptyText())
    val right = truncateStart(targetLength.floorDiv(2), marker = Text.emptyText())
    return left + marker + right
}

/**
 * Returns this text truncated from the start to the specified [length]
 * including the [marker].
 */
public fun <T> Text<T>.truncateStart(length: Int, marker: Text<T>): Text<T> {
    requireNotNegative(length)
    if (this.length <= length) return this
    return marker + takeLast(targetLengthFor(length, marker))
}

/**
 * Returns this text truncated from the end to the specified [length]
 * including the [marker].
 */
public fun <T> Text<T>.truncateEnd(length: Int, marker: Text<T>): Text<T> {
    requireNotNegative(length)
    if (this.length <= length) return this
    return take(targetLengthFor(length, marker)) + marker
}
