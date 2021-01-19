package koodies.text

import koodies.number.mod
import koodies.number.toHexString
import kotlin.random.Random
import kotlin.streams.asSequence

/**
 * Representation of a Unicode code point.
 *
 * Note: As not every Unicode code point forms a proper latter so does not every letter consist of a single code point.
 * If that is what you are looking for, use [Grapheme].
 */
inline class CodePoint(val codePoint: Int) : Comparable<CodePoint> {
    constructor(charSequence: CharSequence) : this("$charSequence"
        .also { require(it.isValidCodePoint()) { "$it does not represent a single Unicode code point" } }
        .codePointAt(0))

    constructor(chars: CharArray) : this(String(chars))

    /**
     * Contains the name of this code point
     */
    val unicodeName: String get() = Unicode[codePoint.toLong()]

    /**
     * The Unicode name, e.g. `LINE SEPARATOR`
     */
    val formattedName: String get() = "❲$unicodeName❳"

    /**
     * Contains this code point formatted in the form `U+XXXX`,
     * e.g. `U+0A` for [NEW LINE](https://codepoints.net/U+000A)
     * or `U+200B` for [ZERO WIDTH SPACE](https://codepoints.net/U+200B).
     */
    val `U+XXXX`: String get() = "U+${codePoint.toHexString(pad = true)}"

    /**
     * Contains this code point formatted in the form `uXXXX`,
     * e.g. `u0A` for [NEW LINE](https://codepoints.net/U+000A)
     * or `u200B` for [ZERO WIDTH SPACE](https://codepoints.net/U+200B).
     */
    val uXXXX: String get() = "U+${codePoint.toHexString(pad = true)}"

    /**
     * Contains this code point formatted in the form `\N{U+XXXX}`,
     * e.g. `\N{U+0A}` for [NEW LINE](https://codepoints.net/U+000A)
     * or `\N{U+200B}` for [ZERO WIDTH SPACE](https://codepoints.net/U+200B).
     */
    val `N{U+XXXX}`: String get() = "\\N{$`U+XXXX`}"

    /**
     * Contains the [Char] representing this code point **if** it can be represented by a single [Char].
     *
     * Otherwise [chars] or [string] must be used.
     */
    val char: Char? get() = if (charCount == 1) chars[0] else null

    /**
     * Contains the character pointed to and represented by a [CharArray].
     */
    val chars: CharArray get() = Character.toChars(codePoint)

    /**
     * Contains the number of [Char] values needed to represent this code point.
     */
    val charCount: Int get() = Character.charCount(codePoint)

    /**
     * Determines if this code point is a
     * [Unicode high-surrogate code unit](http://www.unicode.org/glossary/#high_surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is a high surrogate
     * @see isLowSurrogate
     */
    val isHighSurrogate: Boolean get() = char?.let { Character.isHighSurrogate(it) } ?: false

    /**
     * Determines if this code point is a
     * [Unicode high-surrogate code unit](http://www.unicode.org/glossary/#high_surrogate_code_unit)
     * (also known as *leading-surrogate code unit*).
     *
     * @return `true` if this code point is a low surrogate
     * @see isLowSurrogate
     */
    val isLowSurrogate: Boolean get() = char?.let { Character.isLowSurrogate(it) } ?: false

    /**
     * Determines if this code point is a
     * [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf).
     *
     * @return `true` if this code point is a space character
     * @see isWhitespace
     */
    val isWhitespace: Boolean get() = Character.isWhitespace(codePoint) || Unicode.whitespaces.contains(char)

    /**
     * Determines if this code point is alphanumeric, that is,
     * if it is a [Unicode Letter](https://www.unicode.org/glossary/#letter) or
     * a [Unicode Digit](http://www.unicode.org/glossary/#digits).
     *
     * @return `true` if this code point is a letter or digit
     * @see isLetterOrDigit
     */
    val isAlphanumeric: Boolean get() = Character.isLetterOrDigit(codePoint)

    /**
     * Determines if this code point is an alphanumeric ASCII character, that is,
     * is `A`-`Z`, `a`-`z` or `0`-`9`.
     *
     * @return `true` if this code point is between a high surrogate
     * @see isHighSurrogate
     */
    val isAsciiAlphanumeric: Boolean get() = (codePoint in 0x30..0x39) || (codePoint in 0x41..0x5a) || (codePoint in 0x61..0x7a)

    /**
     * Determines if a character (Unicode code point) is defined in Unicode.
     *
     * @return `true` if this code point is defined
     * @see isDefined
     */
    val isDefined: Boolean get() = Character.isDefined(codePoint)

    /**
     * Contains the character pointed to and represented by a [String].
     */
    val string: String get() = Character.toString(codePoint)

    override fun toString(): String = string

    operator fun rangeTo(to: CodePoint): CodePointRange = CodePointRange(this, to)

    companion object {

        fun random(): CodePoint {
            var possibleCodePoint = Random.nextInt(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
            while (!possibleCodePoint.isValidCodePoint()) possibleCodePoint =
                Random.nextInt(Character.MIN_CODE_POINT, Character.MAX_CODE_POINT)
            return CodePoint(possibleCodePoint)
        }

        fun Int.isValidCodePoint(): Boolean = Character.getType(this).toByte().let {
            it != Character.PRIVATE_USE && it != Character.SURROGATE && it != Character.UNASSIGNED
        }

        /**
         * `true` if these [Char] instances represent a *single* Unicode character.
         */
        fun CharSequence.isValidCodePoint(): Boolean = asCodePoint() != null

        /**
         * The single code point if this consists of exactly one valid code point.
         */
        fun CharSequence.asCodePoint(): CodePoint? =
            asCodePointSequence().take(2).toList().takeIf { it.size == 1 }?.first()?.takeIf { it.codePoint.isValidCodePoint() }

        fun Byte.asCodePoint(): CodePoint = CodePoint(toInt() and 0xFF)

        fun count(string: CharSequence): Long = string.codePoints().count()
        fun count(string: String): Long = string.codePoints().count()
    }

    @Suppress("KDocMissingDocumentation")
    class CodePointRange(override val start: CodePoint, override val endInclusive: CodePoint, step: Int = 1) :
        CodePointProgression(start, endInclusive, step), ClosedRange<CodePoint> {
        @Suppress("ConvertTwoComparisonsToRangeCheck") override fun contains(value: CodePoint): Boolean = start <= value && value <= endInclusive
        override fun isEmpty(): Boolean = if (step > 0) first > last else first < last
        override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
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
                else -> throw kotlin.IllegalArgumentException("Step is zero.")
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

operator fun String.minus(amount: Int): String =
    asCodePointSequence().map { it - amount }.joinToString("")

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
fun CharSequence.asCodePointSequence(): Sequence<CodePoint> =
    codePoints().mapToObj { CodePoint(it) }.asSequence()

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
fun String.asCodePointSequence(): Sequence<CodePoint> =
    (this as CharSequence).asCodePointSequence()

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each [CodePoint] of this string.
 */
fun <R> String.mapCodePoints(transform: (CodePoint) -> R): List<R> =
    asCodePointSequence().map(transform).toList()

