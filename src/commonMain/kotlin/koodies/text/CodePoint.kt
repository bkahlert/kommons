package koodies.text

import koodies.number.mod
import koodies.number.toHexadecimalString
import koodies.text.CodePoint.Companion.isDefined
import kotlin.Char.Companion.MAX_SURROGATE
import kotlin.Char.Companion.MAX_VALUE
import kotlin.Char.Companion.MIN_SURROGATE
import kotlin.Char.Companion.MIN_VALUE
import kotlin.random.Random

private val Whitespaces = listOf(
    CodePoint(0x0020u.toInt()), // SPACE: Depends on font, typically 1/4 em, often adjusted
    CodePoint(0x00A0u.toInt()), // NO-BREAK SPACE: As a space, but often not adjusted
    CodePoint(0x1680u.toInt()), // OGHAM SPACE MARK: Unspecified; usually not really a space but a dash
    CodePoint(0x180Eu.toInt()), // MONGOLIAN VOWEL SEPARATOR: 0
    CodePoint(0x2000u.toInt()), // EN QUAD: 1 en (= 1/2 em)
    CodePoint(0x2001u.toInt()), // EM QUAD: 1 em (nominally, the height of the font)
    CodePoint(0x2002u.toInt()), // EN SPACE (nut): 1 en (= 1/2 em)
    CodePoint(0x2003u.toInt()), // EM SPACE (mutton): 1 em
    CodePoint(0x2004u.toInt()), // THREE-PER-EM SPACE (thick space): 1/3 em
    CodePoint(0x2005u.toInt()), // FOUR-PER-EM SPACE (mid space): 1/4 em
    CodePoint(0x2006u.toInt()), // SIX-PER-EM SPACE: 1/6 em
    CodePoint(0x2007u.toInt()), // FIGURE SPACE	fo: “Tabular width”, the width of digits
    CodePoint(0x2008u.toInt()), // PUNCTUATION SPACE: The width of a period “.”
    CodePoint(0x2009u.toInt()), // THIN SPACE: 1/5 em (or sometimes 1/6 em)
    CodePoint(0x200Au.toInt()), // HAIR SPACE: Narrower than THIN SPACE
    CodePoint(0x200Bu.toInt()), // ZERO WIDTH SPACE: 0
    CodePoint(0x202Fu.toInt()), // NARROW NO-BREAK SPACE	fo: Narrower than NO-BREAK SPACE (or SPACE), “typically the width of a thin space or a mid space”
    CodePoint(0x205Fu.toInt()), // MEDIUM MATHEMATICAL SPACE: 4/18 em
    CodePoint(0x3000u.toInt()), // IDEOGRAPHIC SPACE: The width of ideographic (CJK) characters.
    CodePoint(0xFEFFu.toInt()), // ZERO WIDTH NO-BREAK SPACE: 0
)

/**
 * Representation of a [Unicode code point](http://www.unicode.org/glossary/#code_point)
 */
inline class CodePoint(val codePoint: Int) : Comparable<CodePoint> {
    constructor(charSequence: CharSequence) : this("$charSequence".also {
        require(it.isValidCodePoint()) { "$it does not represent a single Unicode code point" }
    }.singleCodePoint()!!)

    /**
     * Returns this code point as a hexadecimal string.,
     * e.g. `0A` for [NEW LINE](https://codepoints.net/U+000A)
     * or `200B` for [ZERO WIDTH SPACE](https://codepoints.net/U+200B).
     */
    fun toHexadecimalString(): String = codePoint.toHexadecimalString(pad = true)

    /**
     * Contains the character pointed to and represented by a [String].
     */
    val string: String
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
                byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte()) //  replacement character
            }
        }.decodeToString()

    override fun toString(): String = string

    /**
     * Contains the [Char] representing this code point **if** it can be represented by a single [Char].
     *
     * Otherwise [chars] or [string] must be used.
     */
    val char: Char? get() = codePoint.takeIf { it in MIN_VALUE.toInt()..MAX_VALUE.toInt() }?.toChar()

    /**
     * Determines if a character (Unicode code point) is defined in Unicode.
     *
     * @return `true` if this code point is defined
     * @see isDefined
     */
    val isDefined: Boolean get() = codePoint.isDefined()

    /**
     * Determines if this code point is a [LineSeparator].
     *
     * @return `true` if this code point is a [LineSeparator]
     * @see LineSeparators
     */
    val isLineSeparator: Boolean get() = string in LineSeparators

    /**
     * Determines if this code point is a
     * [Unicode surrogate code unit](http://www.unicode.org/glossary/#surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is a high surrogate
     * @see isHighSurrogate
     * @see isLowSurrogate
     */
    val isSurrogate: Boolean get() = char?.isSurrogate() == true

    /**
     * Determines if this code point is a
     * [Unicode high-surrogate code unit](http://www.unicode.org/glossary/#high_surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is a high surrogate
     * @see isSurrogate
     * @see isLowSurrogate
     */
    val isHighSurrogate: Boolean get() = char?.isHighSurrogate() == true

    /**
     * Determines if this code point is a
     * [Unicode low-surrogate code unit](http://www.unicode.org/glossary/#low_surrogate_code_unit)
     * (also known as *trailing-surrogate code unit*).
     *
     * @return `true` if this code point is a low surrogate
     * @see isSurrogate
     * @see isHighSurrogate
     */
    val isLowSurrogate: Boolean get() = char?.isLowSurrogate() == true

    /**
     * Returns `true` if this character is upper case.
     *
     * @see isLowerCase
     */
    val isUpperCase: Boolean get() = char?.isUpperCase() == true

    /**
     * Returns `true` if this character is lower case.
     *
     * @see isUpperCase
     */
    val isLowerCase: Boolean get() = char?.isLowerCase() == true

    /**
     * Determines if this code point is a
     * [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf).
     *
     * @return `true` if this code point is a space character
     * @see isWhitespace
     */
    val isWhitespace: Boolean get() = this in Whitespaces || char?.isWhitespace() == true

    /**
     * Determines if this code point is an alphanumeric ASCII character, that is,
     * is `A`-`Z`, `a`-`z` or `0`-`9`.
     *
     * @return `true` if this code point is between a high surrogate
     * @see isHighSurrogate
     */
    val isAsciiAlphanumeric: Boolean get() = (codePoint in 0x30..0x39) || (codePoint in 0x41..0x5a) || (codePoint in 0x61..0x7a)


    companion object {

        /**
         * The minimum value of a Unicode code point: `U+0000`.
         */
        private const val MIN_CODE_POINT: Int = 0x000000

        /**
         * The maximum value of a Unicode code point: `U+10FFFF`.
         */
        private const val MAX_CODE_POINT: Int = 0X10FFFF

        fun random(): CodePoint {
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
            this in MIN_SURROGATE.toInt()..MAX_SURROGATE.toInt()

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
        fun Int.isUsableCodePoint(): Boolean = isDefined() && !privateUse() && !surrogate()

        private fun String.singleCodePoint(): Int? {
            val bytes = encodeToByteArray()
            if (bytes.isEmpty() || bytes.size > 4) return null

            fun getUInt(index: Int) = bytes[index].toUByte().toInt()
            fun or(vararg values: Int) = values.fold(0) { acc, value -> acc or value }

            val firstByte = getUInt(0)
            return when {
                firstByte < 0x80 -> if (bytes.size != 1) null else
                    firstByte
                firstByte < 0xE0 -> if (bytes.size != 2) null else or(
                    (firstByte and 0x1F) shl 6,
                    getUInt(1) and 0x3F,
                )
                firstByte < 0xF0 -> if (bytes.size != 3) null else or(
                    (firstByte and 0x0F) shl 12,
                    (getUInt(1) and 0x3F) shl 6,
                    (getUInt(2) and 0x3F),
                )
                else -> if (bytes.size != 4) null else or(
                    (firstByte and 0x07) shl 18,
                    (getUInt(1) and 0x3F) shl 12,
                    (getUInt(2) and 0x3F) shl 6,
                    (getUInt(3) and 0x3F),
                )
            }
        }

        /**
         * `true` if these [Char] instances represent a *single* Unicode character.
         */
        fun String.isValidCodePoint(): Boolean = asCodePoint() != null

        /**
         * If this string represents exactly one valid Unicode code point, returns this Unicode code point.
         * In all other cases, returns `null`.
         */
        fun String.asCodePoint(): CodePoint? = singleCodePoint()?.takeIf { it.isUsableCodePoint() }?.let { CodePoint(it) }

        /**
         * Returns the Unicode code point with the same value.
         */
        fun Byte.asCodePoint(): CodePoint = CodePoint(toInt() and 0xFF)
    }


    operator fun rangeTo(to: CodePoint): CodePointRange = CodePointRange(this, to)

    @Suppress("KDocMissingDocumentation")
    class CodePointRange(override val start: CodePoint, override val endInclusive: CodePoint, step: Int = 1) :
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
    open class CodePointProgression internal constructor(start: CodePoint, endInclusive: CodePoint, val step: Int) : Iterable<CodePoint> {
        init {
            require(step != 0) { "Step must be non-zero." }
            require(step != Int.MIN_VALUE) { "Step must be greater than Int.MIN_VALUE to avoid overflow on negation." }
        }

        /**
         * The first element in the progression.
         */
        val first: CodePoint = start

        /**
         * The last element in the progression.
         */
        val last: CodePoint = getProgressionLastElement(start, endInclusive, step)

        override fun iterator(): CodePointIterator = CodePointProgressionIterator(first, last, step)

        /** Checks if the progression is empty. */
        open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

        override fun equals(other: Any?): Boolean =
            other is CodePointProgression && (isEmpty() && other.isEmpty() ||
                first == other.first && last == other.last && step == other.step)

        override fun hashCode(): Int =
            if (isEmpty()) -1 else (31 * (31 * first.codePoint + last.codePoint) + step)

        override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

        companion object {
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
    abstract class CodePointIterator : Iterator<CodePoint> {
        final override fun next() = nextCodePoint()
        abstract fun nextCodePoint(): CodePoint
    }

    override operator fun compareTo(other: CodePoint): Int = codePoint.compareTo(other.codePoint)
    operator fun plus(other: CodePoint): Int = codePoint + other.codePoint
    operator fun plus(other: Int): CodePoint = CodePoint(codePoint + other)
    operator fun minus(other: CodePoint): Int = codePoint - other.codePoint
    operator fun minus(other: Int): CodePoint = CodePoint(codePoint - other)
    @Suppress("AddOperatorModifier") fun mod(other: Int): Int = codePoint.mod(other)
    operator fun inc(): CodePoint = CodePoint(codePoint + 1)
    operator fun dec(): CodePoint = CodePoint(codePoint - 1)
}
