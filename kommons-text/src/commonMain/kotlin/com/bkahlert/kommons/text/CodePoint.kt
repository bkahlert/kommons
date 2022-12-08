package com.bkahlert.kommons.text

import com.bkahlert.kommons.asIterable
import com.bkahlert.kommons.map
import com.bkahlert.kommons.mapToRanges
import kotlin.jvm.JvmInline

/**
 * Representation of a [Unicode code point](https://unicode.org/glossary/#code_point)
 *
 * @see <a href="https://unicode.org/reports/tr18/">Unicode® Technical Standard #18—UNICODE REGULAR EXPRESSIONS</a>
 */
@JvmInline
public value class CodePoint(
    /**
     * Index of this code point in the [Unicode](http://www.unicode.org/) table.
     */
    public val value: Int,
) : Comparable<CodePoint>, CharSequence {
    public constructor(char: kotlin.Char) : this(char.code)
    public constructor(high: kotlin.Char, low: kotlin.Char) : this(makeCharFromSurrogatePair(high, low))
    public constructor(text: CharSequence, range: IntRange) : this(
        when (text.subSequence(range).length) {
            1 -> text[range.first].code
            2 -> makeCharFromSurrogatePair(text[range.first], text[range.first + 1])
            else -> @Suppress("RedundantCompanionReference")
            throw IllegalArgumentException("The requested range $range is not suitable to get a single ${Companion.name}.")
        }
    )

    init {
        if (value !in INDEX_RANGE) throw IndexOutOfBoundsException("index out of range $INDEX_RANGE: $value")
    }

    /** Returns the code point with the [value] increased by the specified [offset]. */
    public operator fun plus(offset: Int): CodePoint = CodePoint(value + offset)

    /** Returns the code point with the [value] decreased by the specified [offset]. */
    public operator fun minus(offset: Int): CodePoint = CodePoint(value - offset)

    /** Returns the code point with the [value] increased by `1`. */
    public operator fun inc(): CodePoint = CodePoint(value + 1)

    /** Returns the code point with the [value] decreased by `1`. */
    public operator fun dec(): CodePoint = CodePoint(value - 1)

    /** Returns a new [CodePointRange] starting with this code point and ending with the specified [endInclusive] code point. */
    public operator fun rangeTo(endInclusive: CodePoint): CodePointRange = CodePointRange(this, endInclusive)

    override fun compareTo(other: CodePoint): Int = value.compareTo(other.value)

    override val length: Int get() = string.length
    override fun get(index: Int): kotlin.Char = string[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = string.subSequence(startIndex, endIndex)
    override fun toString(): String = string

    /** The [kotlin.Char] representing this code point **if** it can be represented by a single [kotlin.Char]. */
    public val char: kotlin.Char? get() = value.takeIf { it in kotlin.Char.MIN_VALUE.code..kotlin.Char.MAX_VALUE.code }?.toChar()

    /** The 16-bit Unicode characters needed to encode. */
    public val chars: CharArray get() = string.toCharArray()

    /** The number of 16-bit Unicode characters needed to encode. */
    public val charCount: Int get() = chars.size

    /** Whether this code point is one of the 10 digits `0`-`9`. */
    public val is0to9: Boolean get() = value in 0x30..0x39

    /** Whether this code point is one of the 26 uppercase characters `A`-`Z`. */
    public val isAtoZ: Boolean get() = value in 0x41..0x5a

    /** Whether this code point is one of the 26 lowercase characters `a`-`z`. */
    @Suppress("SpellCheckingInspection")
    public val isatoz: Boolean get() = value in 0x61..0x7a

    /**
     * Whether this code point is one of
     * the 26 uppercase characters `A`-`Z` or
     * the 26 lowercase characters `a`-`z`.
     */
    @Suppress("SpellCheckingInspection")
    public val isAtoz: Boolean get() = isAtoZ || isatoz

    /**
     * Whether this code point is an alphanumeric ASCII character, that is,
     * is `A`-`Z`, `a`-`z` or `0`-`9`.
     */
    public val isAsciiAlphanumeric: Boolean get() = is0to9 || isAtoZ || isatoz

    /**
     * Whether this code point is alphanumeric, that is,
     * if it's a [Unicode Letter](https://www.unicode.org/glossary/#letter) or
     * a [Unicode Digit](http://www.unicode.org/glossary/#digits).
     */
    public val isAlphanumeric: Boolean get() = isLetter || isDigit

    /** Text unit for texts consisting of [CodePoint] chunks. */
    public companion object : ChunkingTextUnit<CodePoint>("code point") {
        /** The minimum index a code point can have. */
        public const val MIN_INDEX: Int = 0x0

        /** The maximum index a code point can have. */
        public const val MAX_INDEX: Int = 0x10FFFF

        /** The range of indices a code point can have. */
        public val INDEX_RANGE: IntRange = MIN_INDEX..MAX_INDEX

        override fun chunk(text: CharSequence): BreakIterator = CodePointBreakIterator(text)

        override fun transform(text: CharSequence, range: IntRange): CodePoint = CodePoint(text, range)

        /** Returns a new [TextLength] equal to this number of code points. */
        public inline val Int.codePoints: TextLength<CodePoint> get() = lengthOf(this)
    }
}

/** An [Iterator] that iterates [CodePoint] boundaries of the specified [text]. */
public class CodePointBreakIterator(
    private val text: CharSequence,
    private val throwOnInvalidSequence: Boolean = false,
) : BreakIterator by (iterator {
    var index = 0
    while (index < text.length) {
        val ch = text[index++]
        when {
            ch.isHighSurrogate() -> {
                val low = if (index < text.length) text[index] else null
                if (low?.isLowSurrogate() == true) {
                    yield(++index)
                } else {
                    if (throwOnInvalidSequence) throw CharacterCodingException(index - 1)
                    else yield(index)
                }
            }

            ch.isLowSurrogate() -> {
                if (throwOnInvalidSequence) throw CharacterCodingException(index - 1)
                else yield(index)
            }

            else -> yield(index)
        }
    }
})

/** An [Iterator] that iterates [CodePoint] instances. */
public class CodePointIterator(
    private val text: CharSequence,
    private val throwOnInvalidSequence: Boolean = false,
) : Iterator<CodePoint> by (CodePointBreakIterator(text, throwOnInvalidSequence).mapToRanges().map { range -> CodePoint(text, range) })

/** The character pointed to and represented by a [String]. */
internal expect val CodePoint.string: String

/** Returns the Unicode code point with the same value. */
public inline val kotlin.Char.codePoint: CodePoint get() = CodePoint(code)

/** Returns the Unicode code point with the same value. */
public fun Byte.asCodePoint(): CodePoint = CodePoint(toInt() and 0xFF)

/** Returns the Unicode code point with the same value or throws an [IllegalArgumentException] otherwise. */
public fun CharSequence.asCodePoint(): CodePoint = asCodePointOrNull() ?: throw IllegalArgumentException("invalid code point: $this")

/** Returns the Unicode code point with the same value, or `null` otherwise. */
public fun CharSequence.asCodePointOrNull(): CodePoint? = asCodePointSequence().singleOrNull()

/** Whether this code point is a letter. */
public expect val CodePoint.isLetter: Boolean

/** Whether this code point is a digit. */
public expect val CodePoint.isDigit: Boolean

/** Whether this code point is a [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf). */
public expect val CodePoint.isWhitespace: Boolean

internal expect fun CharacterCodingException(inputLength: Int): CharacterCodingException

private fun makeCharFromSurrogatePair(high: kotlin.Char, low: kotlin.Char): Int {
    check(high in kotlin.Char.MIN_HIGH_SURROGATE..kotlin.Char.MAX_HIGH_SURROGATE) { "high character is outside valid range: 0x${high.code.toString(16)}" }
    check(low in kotlin.Char.MIN_LOW_SURROGATE..kotlin.Char.MAX_LOW_SURROGATE) { "high character is outside valid range: 0x${low.code.toString(16)}" }
    val off = 0x10000 - (kotlin.Char.MIN_HIGH_SURROGATE.code shl 10) - kotlin.Char.MIN_LOW_SURROGATE.code
    return (high.code shl 10) + low.code + off
}

/** Returns a sequence yielding the [CodePoint] instances contained in the specified text range of this string. */
public fun CharSequence.asCodePointSequence(
    startIndex: Int = 0,
    endIndex: Int = length,
    throwOnInvalidSequence: Boolean = false,
): Sequence<CodePoint> {
    checkBoundsIndexes(length, startIndex, endIndex)
    return when {
        isEmpty() -> emptySequence()
        else -> CodePointIterator(subSequence(startIndex, endIndex), throwOnInvalidSequence).asSequence()
    }
}

/** Returns the [CodePoint] instances contained in the specified text range of this string. */
public fun CharSequence.toCodePointList(
    startIndex: Int = 0,
    endIndex: Int = length,
    throwOnInvalidSequence: Boolean = false,
): List<CodePoint> = asCodePointSequence(startIndex, endIndex, throwOnInvalidSequence).toList()

/** Returns the number of Unicode code points contained in the specified text range of this string. */
public fun CharSequence.codePointCount(
    startIndex: Int = 0,
    endIndex: Int = length,
    throwOnInvalidSequence: Boolean = false,
): Int = asCodePointSequence(startIndex, endIndex, throwOnInvalidSequence).count()

/** A closed range of code points. */
public class CodePointRange(
    override val start: CodePoint,
    override val endInclusive: CodePoint,
) : ClosedRange<CodePoint>, Iterable<CodePoint> {
    private val iterable = asIterable { it + 1 }
    override fun iterator(): Iterator<CodePoint> = iterable.iterator()
}
