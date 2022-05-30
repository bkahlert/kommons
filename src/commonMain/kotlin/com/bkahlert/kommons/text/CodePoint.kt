package com.bkahlert.kommons.text

import com.bkahlert.kommons.math.mod
import com.bkahlert.kommons.math.toHexadecimalString
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.CodePoint.Companion.isUsableCodePoint
import kotlin.Char.Companion.MAX_SURROGATE
import kotlin.Char.Companion.MAX_VALUE
import kotlin.Char.Companion.MIN_SURROGATE
import kotlin.Char.Companion.MIN_VALUE
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.text.padStart as kotlinPadStart

private fun String.escape() = "\\u$this"

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
    public val codePoint: Int,
) : Comparable<CodePoint> {
    public constructor(char: Char) : this(char.code)
    public constructor(charSequence: CharSequence) : this("$charSequence".also {
        require(it.isValidCodePoint()) { "$it does not represent a single Unicode code point" }
    }.singleCodePoint()!!)

    /**
     * Returns this code point as a hexadecimal string.,
     * e.g. `0A` for [NEW LINE](https://codepoints.net/U+000A)
     * or `200B` for [ZERO WIDTH SPACE](https://codepoints.net/U+200B).
     */
    public val hexCode: String get() = codePoint.toHexadecimalString(pad = true).uppercase()

    /**
     * Returns this code point as string that can be used to match exactly this code point using a regular expression.
     */
    public fun toLiteralRegex(): Regex = hexCode.kotlinPadStart(4, '0').escape().toRegex()

    /**
     * Contains the character pointed to and represented by a [String].
     */
    public val string: String
        get() = when {
            codePoint <= 0x7F -> {
                byteArrayOf(codePoint.toByte())
            }
            codePoint <= 0x07FF -> {
                byteArrayOf(
                    (((codePoint shr 6) and 0x1F) or 0xC0).toByte(),
                    (((codePoint shr 0) and 0x3F) or 0x80).toByte(),
                )
            }
            codePoint <= 0xFFFF -> {
                byteArrayOf(
                    (((codePoint shr 12) and 0x0F) or 0xE0).toByte(),
                    (((codePoint shr 6) and 0x3F) or 0x80).toByte(),
                    (((codePoint shr 0) and 0x3F) or 0x80).toByte(),
                )
            }
            codePoint <= 0x10FFFF -> {
                byteArrayOf(
                    (((codePoint shr 18) and 0x07) or 0xF0).toByte(),
                    (((codePoint shr 12) and 0x3F) or 0x80).toByte(),
                    (((codePoint shr 6) and 0x3F) or 0x80).toByte(),
                    (((codePoint shr 0) and 0x3F) or 0x80).toByte(),
                )
            }
            else -> {
                byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte())
            }
        }.decodeToString()

    /**
     * String representation of this code point.
     */
    override fun toString(): String = string

    /**
     * Contains the [Char] representing this code point **if** it can be represented by a single [Char].
     *
     * Otherwise [string] must be used.
     */
    public val char: Char? get() = codePoint.takeIf { it in MIN_VALUE.code..MAX_VALUE.code }?.toChar()

    /**
     * Determines if a character (Unicode code point) is defined in Unicode.
     *
     * @return `true` if this code point is defined
     * @see isDefined
     */
    public val isDefined: Boolean get() = codePoint.isDefined()

    /**
     * Determines if this code point is a line separator.
     *
     * @return `true` if this code point is a line separator
     * @see LineSeparators
     */
    public val isLineSeparator: Boolean get() = string in LineSeparators

    /**
     * Determines if this code point is a
     * [Unicode surrogate code unit](http://www.unicode.org/glossary/#surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is a high surrogate
     * @see isHighSurrogate
     * @see isLowSurrogate
     */
    public val isSurrogate: Boolean get() = char?.isSurrogate() == true

    /**
     * Determines if this code point is a
     * [Unicode high-surrogate code unit](http://www.unicode.org/glossary/#high_surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is a high surrogate
     * @see isSurrogate
     * @see isLowSurrogate
     */
    public val isHighSurrogate: Boolean get() = char?.isHighSurrogate() == true

    /**
     * Determines if this code point is a
     * [Unicode low-surrogate code unit](http://www.unicode.org/glossary/#low_surrogate_code_unit)
     * (also known as *trailing-surrogate code unit*).
     *
     * @return `true` if this code point is a low surrogate
     * @see isSurrogate
     * @see isHighSurrogate
     */
    public val isLowSurrogate: Boolean get() = char?.isLowSurrogate() == true

    /**
     * Returns `true` if this character is upper case.
     *
     * @see isLowerCase
     */
    public val isUpperCase: Boolean get() = char?.isUpperCase() == true

    /**
     * Returns `true` if this character is lower case.
     *
     * @see isUpperCase
     */
    public val isLowerCase: Boolean get() = char?.isLowerCase() == true

    /**
     * Determines if this code point is a
     * [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf).
     *
     * @return `true` if this code point is a space character
     * @see isWhitespace
     */
    public val isWhitespace: Boolean get() = this in Whitespaces.asCodePoints || char?.isWhitespace() == true

    /**
     * Determines if this code point is one of the mistakenly
     * considered whitespaces listed in [Whitespaces.ZeroWidthWhitespaces].
     *
     * @return `true` if this code point is listed in [Whitespaces.ZeroWidthWhitespaces].
     */
    public val isZeroWidthWhitespace: Boolean get() = string in Whitespaces.ZeroWidthWhitespaces

    /**
     * Determines if this code point is one of the 10 digits `0`-`9`.
     */
    public val is0to9: Boolean get() = codePoint in 0x30..0x39

    /**
     * Determines if this code point is one of the 26 upper case characters `A`-`Z`.
     */
    public val isAtoZ: Boolean get() = codePoint in 0x41..0x5a

    /**
     * Determines if this code point is one of the 26 lower case characters `a`-`z`.
     */
    @Suppress("SpellCheckingInspection")
    public val isatoz: Boolean
        get() = codePoint in 0x61..0x7a

    /**
     * Determines if this code point is one of
     * the 26 upper case characters `A`-`Z` or
     * the 26 lower case characters `a`-`z`.
     */
    @Suppress("SpellCheckingInspection")
    public val isAtoz: Boolean
        get() = isAtoZ || isatoz

    /**
     * Determines if this code point is an alphanumeric ASCII character, that is,
     * is `A`-`Z`, `a`-`z` or `0`-`9`.
     *
     * @return `true` if this code point is between a high surrogate
     * @see isHighSurrogate
     */
    public val isAsciiAlphanumeric: Boolean get() = is0to9 || isAtoZ || isatoz

    /**
     * Determines if this code point is alphanumeric, that is,
     * if it is a [Unicode Letter](https://www.unicode.org/glossary/#letter) or
     * a [Unicode Digit](http://www.unicode.org/glossary/#digits).
     *
     * @return `true` if this code point is a letter or digit
     * @see <a href="https://www.unicode.org/reports/tr18/#property_syntax">Unicode® Technical Standard #18—UNICODE REGULAR EXPRESSIONS</a>
     */
    public val isAlphanumeric: Boolean get() = Regex("[\\p{L} \\p{Nd}]").matches(string)

    public companion object {

        /**
         * The minimum value of a Unicode code point: `U+0000`.
         */
        private const val MIN_CODE_POINT: Int = 0x000000

        /**
         * The maximum value of a Unicode code point: `U+10FFFF`.
         */
        private const val MAX_CODE_POINT: Int = 0X10FFFF

        /**
         * Returns a random [CodePoint].
         */
        public fun random(): CodePoint {
            var possibleCodePoint = Random.nextInt(MIN_CODE_POINT, MAX_CODE_POINT)
            while (!possibleCodePoint.isUsableCodePoint()) possibleCodePoint =
                Random.nextInt(MIN_CODE_POINT, MAX_CODE_POINT)
            return CodePoint(possibleCodePoint)
        }

        private val Int.plane get() = this ushr 16

        private fun Int.privateUse(): Boolean {
            val privatePlanes = listOf(15, 16)
            return plane in privatePlanes
        }

        private fun Int.surrogate(): Boolean =
            this in MIN_SURROGATE.code..MAX_SURROGATE.code

        private fun Int.isDefined(): Boolean {
            val assignedPlanes = listOf(0, 1, 2, 14, 15)
            return plane in assignedPlanes
        }

        /**
         * Returns whether this Unicode code point
         * - belongs to an defined plane
         * - not belongs to a private plane
         * - not represents a surrogate
         */
        public fun Int.isUsableCodePoint(): Boolean = isDefined() && !privateUse() && !surrogate()
    }

    /**
     * Returns a [CodePointRange] between this and [to].
     */
    public operator fun rangeTo(to: CodePoint): CodePointRange = CodePointRange(this, to)

    @Suppress("KDocMissingDocumentation")
    public class CodePointRange(override val start: CodePoint, override val endInclusive: CodePoint, step: Int = 1) :
        CodePointProgression(start, endInclusive, step), ClosedRange<CodePoint> {
        @Suppress("ConvertTwoComparisonsToRangeCheck") override fun contains(value: CodePoint): Boolean = start <= value && value <= endInclusive
        override fun isEmpty(): Boolean = if (step > 0) first > last else first < last
        override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            if (!super.equals(other)) return false

            other as CodePointRange

            if (start != other.start) return false
            if (endInclusive != other.endInclusive) return false
            if (step != other.step) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + start.hashCode()
            result = 31 * result + endInclusive.hashCode()
            result = 31 * result + step
            return result
        }
    }

    /**
     * A progression of values of type [CodePoint].
     */
    public open class CodePointProgression internal constructor(start: CodePoint, endInclusive: CodePoint, public val step: Int) : Iterable<CodePoint> {
        init {
            require(step != 0) { "Step must be non-zero." }
            require(step != Int.MIN_VALUE) { "Step must be greater than Int.MIN_VALUE to avoid overflow on negation." }
        }

        /**
         * The first element in the progression.
         */
        public val first: CodePoint = start

        /**
         * The last element in the progression.
         */
        public val last: CodePoint = getProgressionLastElement(start, endInclusive, step)

        override fun iterator(): CodePointIterator = CodePointProgressionIterator(first, last, step)

        /** Checks if the progression is empty. */
        public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

        override fun equals(other: Any?): Boolean {
            if (other !is CodePointProgression) return false
            if (isEmpty() && other.isEmpty()) return true
            return first == other.first && last == other.last && step == other.step
        }

        override fun hashCode(): Int =
            if (isEmpty()) -1 else (31 * (31 * first.codePoint + last.codePoint) + step)

        override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

        private companion object {
            /**
             * Calculates the final element of a bounded arithmetic progression, i.e. the last element of the progression which is in the range
             * from [start] to [end] in case of a positive [step], or from [end] to [start] in case of a negative
             * [step].
             *
             * No validation on passed parameters is performed. The given parameters should satisfy the condition:
             *
             * - either `step > 0` and `start <= end`,
             * - or `step < 0` and `start >= end`.
             *
             * @param start first element of the progression
             * @param end ending bound for the progression
             * @param step increment, or difference of successive elements in the progression
             * @return the final element of the progression
             * @suppress
             */
            private fun getProgressionLastElement(start: CodePoint, end: CodePoint, step: Int): CodePoint = when {
                step > 0 -> if (start.codePoint >= end.codePoint) end else end - differenceModulo(end, start, step)
                step < 0 -> if (start <= end) end else end + differenceModulo(start, end, -step)
                else -> throw IllegalArgumentException("Step is zero.")
            }

            // (a - b) mod c
            private fun differenceModulo(a: CodePoint, b: CodePoint, c: Int): Int = (a.mod(c) - b.mod(c)).mod(c)
        }
    }

    /**
     * An iterator over a progression of values of type [CodePoint].
     * @property step the number by which the value is incremented on each step.
     */
    internal class CodePointProgressionIterator(first: CodePoint, last: CodePoint, private val step: Int) : CodePointIterator() {
        private val finalElement = last
        private var hasNext: Boolean = if (step > 0) first <= last else first >= last
        private var next = if (hasNext) first else finalElement
        override fun hasNext(): Boolean = hasNext

        override fun nextCodePoint(): CodePoint {
            val value = next
            if (value == finalElement) {
                if (!hasNext) throw NoSuchElementException()
                hasNext = false
            } else {
                next += step
            }
            return value
        }
    }

    /**
     * An iterator over a sequence of values of type [CodePoint].
     */
    public abstract class CodePointIterator : Iterator<CodePoint> {
        final override fun next(): CodePoint = nextCodePoint()
        public abstract fun nextCodePoint(): CodePoint
    }

    override operator fun compareTo(other: CodePoint): Int = codePoint.compareTo(other.codePoint)
    public operator fun plus(other: CodePoint): Int = codePoint + other.codePoint
    public operator fun plus(other: Int): CodePoint = CodePoint(codePoint + other)
    public operator fun minus(other: CodePoint): Int = codePoint - other.codePoint
    public operator fun minus(other: Int): CodePoint = CodePoint(codePoint - other)
    @Suppress("AddOperatorModifier") public fun mod(other: Int): Int = codePoint.mod(other)
    public operator fun inc(): CodePoint = CodePoint(codePoint + 1)
    public operator fun dec(): CodePoint = CodePoint(codePoint - 1)
}

/**
 * Attempts to read a code point from this byte array at the given [offset].
 *
 * The byte array must be a UTF-8 encoded string.
 *
 * Returns a [Pair] with [Pair.first] being the number of bytes
 * the code point contained in [Pair.second] consists of.
 */
private fun ByteArray.readCodePoint(offset: Int): Pair<Int, Int>? {
    if (size <= offset) return null

    fun read(index: Int): Int = this[offset + index].toUByte().toInt()
    fun or(vararg values: Int): Int = values.fold(0) { acc, value -> acc or value }

    val firstByte: Int = read(0)
    return when {
        firstByte < 0x80 ->
            1 to firstByte
        firstByte < 0xE0 -> if (size <= offset + 1) null
        else 2 to or((firstByte and 0x1F) shl 6, read(1) and 0x3F)
        firstByte < 0xF0 -> if (size <= offset + 2) null
        else 3 to or((firstByte and 0x0F) shl 12, (read(1) and 0x3F) shl 6, (read(2) and 0x3F))
        else -> if (size <= offset + 3) null else
            4 to or((firstByte and 0x07) shl 18, (read(1) and 0x3F) shl 12, (read(2) and 0x3F) shl 6, (read(3) and 0x3F))
    }
}

/**
 * Attempts to read a code point from this character sequence at the given [offset].
 *
 * The characters are expected to be a UTF-18 encoded.
 *
 * Returns a [Pair] with [Pair.first] being the number of characters
 * the code point contained in [Pair.second] consists of.
 */
private fun CharSequence.readCodePoint(offset: Int): Pair<Int, Int>? {
    if (length <= offset) return null

    val firstChar: Char = this[offset]
    if (firstChar.isHighSurrogate()) {
        if (length <= offset + 1) {
            return 1 to firstChar.code
        }
        val secondChar: Char = this[offset + 1]
        if (secondChar.isLowSurrogate()) {
            val left = (firstChar.code - 0xD800) * 0x400
            val right = secondChar.code - 0xDC00
            return 2 to (left + right + 0x10000)
        }
    }

    return 1 to firstChar.code
}

/**
 * Attempts to read the single code point from this UTF-8 encoded string.
 *
 * Returns a [Pair] with [Pair.first] being the number of bytes
 * the code point contained in [Pair.second] consists of.
 */
private fun String.singleCodePoint(): Int? {
    val bytes = encodeToByteArray()
    return bytes.readCodePoint(0)?.let { (length, codePoint) ->
        when (length) {
            bytes.size -> codePoint
            else -> null
        }
    }
}

/**
 * `true` if these [Char] instances represent a *single* Unicode character.
 */
public fun String.isValidCodePoint(): Boolean = asCodePoint() != null

/**
 * If this string represents exactly one valid Unicode code point, returns it.
 * In all other cases, returns `null`.
 */
public fun String.asCodePoint(): CodePoint? = singleCodePoint()?.takeIf { it.isUsableCodePoint() }?.let { CodePoint(it) }

/**
 * Returns the Unicode code point with the same value.
 */
public fun Byte.asCodePoint(): CodePoint = CodePoint(toInt() and 0xFF)

/**
 * Returns the Unicode code point with the same value.
 */
public inline val Char.codePoint: CodePoint get() = CodePoint(code)

/**
 * Returns a lazily propagated sequence containing the [CodePoint] instances this string consists of.
 */
public fun ByteArray.asCodePointSequence(): Sequence<CodePoint> {
    var offset = 0
    return generateSequence {
        readCodePoint(offset)?.let { (length, codePoint) ->
            offset += length
            CodePoint(codePoint)
        }
    }
}

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
public fun CharSequence.asCodePointSequence(): Sequence<CodePoint> {
    var offset = 0
    return generateSequence {
        readCodePoint(offset)?.let { (length, codePoint) ->
            offset += length
            CodePoint(codePoint)
        }
    }
}

/**
 * Returns a list containing the [CodePoint] instances this string consists of.
 */
public fun CharSequence.toCodePointList(): List<CodePoint> =
    asCodePointSequence().toList()

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
public val CharSequence.codePointCount: Int
    get() {
        val bytes = toString().encodeToByteArray()
        var offset = 0
        val lengthsSequence = generateSequence { bytes.readCodePoint(offset)?.let { (length, _) -> length.also { offset += it } } }
        return lengthsSequence.count()
    }

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each [CodePoint] of this string.
 */
public fun <R> String.mapCodePoints(transform: (CodePoint) -> R): List<R> =
    asCodePointSequence().map(transform).toList()

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
public fun CharSequence.mapCharacters(transform: (String) -> CharSequence): String {
    val bytes = toString().encodeToByteArray()
    var offset = 0
    return generateSequence {
        bytes.readCodePoint(offset)?.let { (length, codePoint) ->
            offset += length
            transform(CodePoint(codePoint).string)
        }
    }.joinLinesToString("")
}

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
public fun CharSequence.formatCharacters(transform: ANSI.Text.() -> CharSequence): String {
    val bytes = toString().encodeToByteArray()
    var offset = 0
    return generateSequence {
        bytes.readCodePoint(offset)?.let { (length, codePoint) ->
            offset += length
            CodePoint(codePoint).string.ansi.transform()
        }
    }.joinLinesToString("")
}

/**
 * Returns a [Regex] matching exactly this character sequence.
 *
 * Each char is matched using its hexadecimal encoding.
 */
public fun CharSequence.toLiteralRegex(): Regex {
    val bytes = toString().encodeToByteArray()
    var offset = 0
    return generateSequence {
        bytes.readCodePoint(offset)?.let { (length, codePoint) ->
            offset += length
            CodePoint(codePoint).toLiteralRegex()
        }
    }.joinLinesToString("").toRegex()
}

/**
 * Contains the number of [Char] values needed to represent this code point.
 */
public expect val CodePoint.charCount: Int
