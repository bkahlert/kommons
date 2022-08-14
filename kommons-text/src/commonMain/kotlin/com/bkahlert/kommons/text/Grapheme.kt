package com.bkahlert.kommons.text

import com.bkahlert.kommons.map
import com.bkahlert.kommons.mapToRanges
import kotlin.jvm.JvmInline

/**
 * Representation of a [Unicode grapheme cluster](https://unicode.org/glossary/#grapheme_cluster)
 *
 * @see <a href="https://unicode.org/reports/tr29/">Unicode® Technical Standard #18—UNICODE TEXT SEGMENTATION</a>
 * @see <a href="https://unicode.org/reports/tr29/#Grapheme_Cluster_Boundary_Rules">Grapheme Cluster Boundary Rules</a>
 */
@JvmInline
public value class Grapheme private constructor(
    /** The string this grapheme consists of. */
    public val value: CharSequence,
) : CharSequence by value {
    public constructor(text: CharSequence, startIndex: Int = 0, endIndex: Int = -1) : this(CharSequenceDelegate(text, startIndex, endIndex))
    public constructor(text: CharSequence, range: IntRange) : this(CharSequenceDelegate(text, range))

    /** The [CodePoint] instances this grapheme consists of. */
    public val codePoints: List<CodePoint> get() = value.toCodePointList()
    override fun toString(): String = value.toString()

    /** Text unit for texts consisting of [Grapheme] chunks. */
    public companion object : ChunkingTextUnit<Grapheme>("grapheme") {

        override fun chunk(text: CharSequence): BreakIterator = GraphemeBreakIterator(text)

        override fun transform(text: CharSequence, range: IntRange): Grapheme = Grapheme(text, range)

        /** Returns a new [TextLength] equal to this number of graphemes. */
        public inline val Int.graphemes: TextLength<Grapheme> get() = lengthOf(this)
    }
}

/** An [Iterator] that iterates [Grapheme] boundaries. */
public expect class GraphemeBreakIterator(text: CharSequence) : BreakIterator

/** An [Iterator] that iterates [Grapheme] instances. */
public class GraphemeIterator(private val text: CharSequence) : Iterator<Grapheme> by (GraphemeBreakIterator(text).mapToRanges().map { Grapheme(text, it) })

/** Returns the [Grapheme] with the same value, or throws an [IllegalArgumentException] otherwise. */
public fun CharSequence.asGrapheme(): Grapheme = asGraphemeOrNull() ?: throw IllegalArgumentException("invalid grapheme: $this")

/** Returns the [Grapheme] with the same value, or `null` otherwise. */
public fun CharSequence.asGraphemeOrNull(): Grapheme? = asGraphemeSequence().singleOrNull()

/** Returns a sequence yielding the [Grapheme] instances contained in the specified text range of this string. */
public fun CharSequence.asGraphemeSequence(
    startIndex: Int = 0,
    endIndex: Int = length,
): Sequence<Grapheme> {
    checkBoundsIndexes(length, startIndex, endIndex)
    return when {
        isEmpty() -> emptySequence()
        else -> GraphemeIterator(subSequence(startIndex, endIndex)).asSequence()
    }
}

/** Returns the [Grapheme] instances contained in the specified text range of this string. */
public fun CharSequence.toGraphemeList(
    startIndex: Int = 0,
    endIndex: Int = length,
): List<Grapheme> = asGraphemeSequence(startIndex, endIndex).toList()

/** Returns the number of [Grapheme] instances contained in the specified text range of this string. */
public fun CharSequence.graphemeCount(
    startIndex: Int = 0,
    endIndex: Int = length,
): Int = asGraphemeSequence(startIndex, endIndex).count()
