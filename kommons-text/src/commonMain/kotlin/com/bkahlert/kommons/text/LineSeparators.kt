package com.bkahlert.kommons.text

import com.bkahlert.kommons.EMPTY
import com.bkahlert.kommons.text.LineSeparators.Common
import com.bkahlert.kommons.text.Unicode as UnicodeConstants

/**
 * Collection of [Common] line separators.
 *
 * @see <a href="https://www.unicode.org/reports/tr18/#RL1.6">Unicode® Technical Standard #18—Line Boundaries</a>
 * @see <a href="https://www.unicode.org/reports/tr18/">Unicode® Technical Standard #18—UNICODE REGULAR EXPRESSIONS</a>
 */
public object LineSeparators : AbstractList<String>() {

    /**
     * Line separator as used on Windows systems.
     *
     * Representations: `\r\n`,  `␍␊`, `⏎`
     */
    public const val CRLF: String = UnicodeConstants.CARRIAGE_RETURN.toString() + UnicodeConstants.LINE_FEED.toString()

    /**
     * Line separator as used on Unix systems and modern Mac systems.
     *
     * Representations: `\n`, `␊`, `⏎`
     *
     */
    public const val LF: String = UnicodeConstants.LINE_FEED.toString()

    /**
     * Line separator as used on old Mac systems.
     *
     * Representations: `\r`, `␍`, `⏎`
     */
    public const val CR: String = UnicodeConstants.CARRIAGE_RETURN.toString()

    /**
     * Next line separator
     *
     * Representations: `␤`, `⏎`
     */
    public const val NEL: String = UnicodeConstants.NEXT_LINE.toString()

    /**
     * Paragraph separator
     *
     * Representations: `ₛᷮ`, `⏎`
     */
    public const val PS: String = UnicodeConstants.PARAGRAPH_SEPARATOR.toString()

    /**
     * Line separator
     *
     * Representations: `ₛᷞ`, `⏎`
     */
    public const val LS: String = UnicodeConstants.LINE_SEPARATOR.toString()

    /**
     * Same line separator as used by Kotlin.
     */
    public val Default: String = StringBuilder().appendLine().toString()

    /** The common line separators [CRLF], [LF], and [CR]. */
    public val Common: Array<String> = arrayOf(CRLF, LF, CR)

    override val size: Int get() = Common.size
    override fun get(index: Int): String = Common[index]

    /** All [Unicode® Technical Standard #18—Line Boundaries](https://www.unicode.org/reports/tr18/#RL1.6). */
    public val Unicode: Array<String> = arrayOf(CRLF, LF, CR, NEL, PS, LS)

    /**
     * All [Unicode® Technical Standard #18—Line Boundaries](https://www.unicode.org/reports/tr18/#RL1.6)
     * that are not [Common].
     */
    public val Uncommon: Array<String> = Unicode.subtract(Common.toSet()).toTypedArray()


    /** A [Regex] that matches [Common] line separators. */
    public val CommonRegex: Regex by lazy { Regex.fromLiteralAlternates(*Common) }

    /** A [Regex] that matches [Unicode] line separators. */
    public val UnicodeRegex: Regex by lazy { Regex.fromLiteralAlternates(*Unicode) }

    /** A [Regex] that matches [Uncommon] line separators. */
    public val UncommonRegex: Regex by lazy { Regex.fromLiteralAlternates(*Uncommon) }


    /**
     * Returns the first line separator in this character sequence which is also contained in
     * the specified [lineSeparators] (default: [Common]), or `null` otherwise.
     */
    public fun CharSequence.getFirstLineSeparatorOrNull(vararg lineSeparators: String = Common): String? =
        splitToSequence(delimiters = lineSeparators, keepDelimiters = true).firstOrNull()
            ?.getTrailingLineSeparatorOrNull(lineSeparators = lineSeparators)

    /**
     * Returns the length of the first line separator in this character sequence which is also contained in
     * the specified [lineSeparators] (default: [Common]), or `0` otherwise.
     */
    public fun CharSequence.getFirstLineSeparatorLength(vararg lineSeparators: String = Common): Int =
        getFirstLineSeparatorOrNull(lineSeparators = lineSeparators)?.length ?: 0

    /**
     * Returns the line separator used the most in this character sequence which is also contained in
     * the specified [lineSeparators] (default: [Common]), or [default] if none is found.
     */
    public fun CharSequence.getMostFrequentLineSeparatorOrDefault(vararg lineSeparators: String = Common, default: String = Default): String {
        val histogram: MutableMap<String, Int?> = lineSeparators.associateWith { null }.toMutableMap()
        var offset: Int? = 0
        while (offset != null) {
            offset = findAnyOf(lineSeparators.toList(), offset)?.let { (pos, sep) ->
                histogram[sep] = (histogram[sep] ?: 0) + 1
                pos + sep.length
            }
        }
        return histogram
            .filterValues { it != null }
            .maxByOrNull { (_, count) -> count ?: 0 }
            ?.key ?: default
    }


    /** Replaces all specified [lineSeparators] (default: [Common]) with the specified [lineSeparator] (default: [Default]). */
    public fun CharSequence.unifyLineSeparators(lineSeparator: String = Default, lineSeparators: Array<String> = Common): String =
        splitToSequence(delimiters = lineSeparators, keepDelimiters = false).joinToString(lineSeparator)

    /** Replaces all specified [lineSeparators] (default: [Common]) with the [Default] line separator. */
    public fun CharSequence.unifyLineSeparators(lineSeparators: Array<String> = Common): String =
        unifyLineSeparators(lineSeparator = Default, lineSeparators = lineSeparators)


    /**
     * Splits this character sequence to a sequence of lines delimited by the specified [lineSeparators] (default: [Common]).
     *
     * If the lines returned do include terminating line separators is specified by [keepDelimiters].
     */
    public fun CharSequence?.lineSequence(vararg lineSeparators: String = Common, keepDelimiters: Boolean = false): Sequence<String> =
        this?.splitToSequence(delimiters = lineSeparators, keepDelimiters = keepDelimiters) ?: emptySequence()

    /**
     * Splits this character sequence to a list of lines delimited by the specified [lineSeparators] (default: [Common]).
     *
     * If the lines returned do include terminating line separators is specified by [keepDelimiters].
     */
    public fun CharSequence?.lines(vararg lineSeparators: String = Common, keepDelimiters: Boolean = false): List<String> =
        lineSequence(lineSeparators = lineSeparators, keepDelimiters = keepDelimiters).toList()

    /**
     * Splits this character sequence to a sequence of chunked lines delimited by the specified [lineSeparators] (default: [Common]),
     * each chunked line having a length of at most the specified [size],
     * and applies the specified [transform] to each of them.
     */
    public fun <R> CharSequence?.chunkedLineSequence(size: Int, vararg lineSeparators: String = Common, transform: (CharSequence) -> R): Sequence<R> {
        if (this == null) return emptySequence()
        val lines = lineSequence(lineSeparators = lineSeparators, keepDelimiters = false)
        return lines.flatMap { line: String ->
            val iterator = line.chunkedSequence(size, transform).iterator()
            if (iterator.hasNext()) iterator.asSequence() else sequenceOf(transform(String.EMPTY))
        }
    }

    /**
     * Splits this character sequence to a list of chunked lines delimited by the specified [lineSeparators] (default: [Common]),
     * each chunked line having a length of at most the specified [size],
     * and applies the specified [transform] to each of them.
     */
    public fun <R> CharSequence?.chunkedLines(size: Int, vararg lineSeparators: String = Common, transform: (CharSequence) -> R): List<R> =
        chunkedLineSequence(size = size, lineSeparators = lineSeparators, transform = transform).toList()

    /** Applies the specified [transform] to each line delimited by the specified [lineSeparators]. */
    public fun CharSequence.mapLines(vararg lineSeparators: String, transform: (String) -> CharSequence): String =
        buildString {
            this@mapLines.splitToSequence(delimiters = lineSeparators, keepDelimiters = true).forEach {
                when (val lineSeparator = it.getTrailingLineSeparatorOrNull(lineSeparators = lineSeparators)) {
                    null -> {
                        append(transform(it))
                    }

                    else -> {
                        append(transform(it.removeSuffix(lineSeparator)))
                        append(lineSeparator)
                    }
                }
            }
        }

    /** Applies the specified [transform] to each line delimited by a [Common] line separator. */
    public fun CharSequence.mapLines(transform: (String) -> CharSequence): String =
        mapLines(lineSeparators = Common, transform = transform)

    /** Whether this string consists of more than one line delimited by the specified [lineSeparators] (default: [Common]). */
    public fun CharSequence?.isMultiline(vararg lineSeparators: String = Common): Boolean =
        lineSequence(lineSeparators = lineSeparators).take(2).count() > 1

    /** Whether this string consists of one line delimited by the specified [lineSeparators] (default: [Common]). */
    public fun CharSequence?.isSingleLine(vararg lineSeparators: String = Common): Boolean =
        lineSequence(lineSeparators = lineSeparators).take(2).count() == 1


    /**
     * Returns the line separator this character sequence starts with and is contained in
     * the specified [lineSeparators] (default: [Common]), or `null` otherwise.
     */
    public fun CharSequence.getLeadingLineSeparatorOrNull(vararg lineSeparators: String = Common): String? =
        lineSeparators.firstOrNull { startsWith(it) }

    /**
     * Returns `true` if this character sequence starts with one of
     * the specified [lineSeparators] (default: [Common]), or false otherwise.
     */
    public fun CharSequence.startsWithLineSeparator(vararg lineSeparators: String = Common): Boolean =
        getLeadingLineSeparatorOrNull(lineSeparators = lineSeparators) != null

    /**
     * Returns this character sequence with its leading line separator removed if it's contained in
     * the specified [lineSeparators] (default: [Common]), or returns this character sequence otherwise.
     */
    public fun CharSequence.removeLeadingLineSeparator(vararg lineSeparators: String = Common): CharSequence =
        getLeadingLineSeparatorOrNull(lineSeparators = lineSeparators)?.let { removePrefix(it) } ?: this

    /**
     * Returns this string with its leading line separator removed if it's contained in
     * the specified [lineSeparators] (default: [Common]), or returns this string otherwise.
     */
    public fun String.removeLeadingLineSeparator(vararg lineSeparators: String = Common): String =
        getLeadingLineSeparatorOrNull(lineSeparators = lineSeparators)?.let { removePrefix(it) } ?: this

    /**
     * Returns this character sequence with the specified [lineSeparator] (default: any) prepended
     * if it's not already present and contained in
     * the specified [lineSeparators] (default: [Common]).
     */
    public fun CharSequence.withLeadingLineSeparator(lineSeparator: String? = null, vararg lineSeparators: String = Common): CharSequence =
        when (lineSeparator) {
            null -> {
                if (startsWithLineSeparator(lineSeparators = lineSeparators)) this
                else Default + this
            }

            else -> {
                if (lineSeparator == getLeadingLineSeparatorOrNull(lineSeparators = lineSeparators)) this
                else lineSeparator + removeLeadingLineSeparator(lineSeparators = lineSeparators)
            }
        }

    /**
     * Returns this string with the specified [lineSeparator] (default: any) prepended
     * if it's not already present and contained in
     * the specified [lineSeparators] (default: [Common]).
     */
    public fun String.withLeadingLineSeparator(lineSeparator: String? = null, vararg lineSeparators: String = Common): String =
        when (lineSeparator) {
            null -> {
                if (startsWithLineSeparator(lineSeparators = lineSeparators)) this
                else Default + this
            }

            else -> {
                if (lineSeparator == getLeadingLineSeparatorOrNull(lineSeparators = lineSeparators)) this
                else lineSeparator + removeLeadingLineSeparator(lineSeparators = lineSeparators)
            }
        }


    /**
     * Returns the line separator this character sequence ends with and is contained in
     * the specified [lineSeparators] (default: [Common]), or `null` otherwise.
     */
    public fun CharSequence.getTrailingLineSeparatorOrNull(vararg lineSeparators: String = Common): String? =
        lineSeparators.firstOrNull { endsWith(it) }

    /**
     * Returns `true` if this character sequence ends with one of
     * the specified [lineSeparators] (default: [Common]), or false otherwise.
     */
    public fun CharSequence.endsWithLineSeparator(vararg lineSeparators: String = Common): Boolean =
        getTrailingLineSeparatorOrNull(lineSeparators = lineSeparators) != null

    /**
     * Returns this character sequence with its trailing line separator removed if it's contained in
     * the specified [lineSeparators] (default: [Common]), or returns this character sequence otherwise.
     */
    public fun CharSequence.removeTrailingLineSeparator(vararg lineSeparators: String = Common): CharSequence =
        getTrailingLineSeparatorOrNull(lineSeparators = lineSeparators)?.let { removeSuffix(it) } ?: this

    /**
     * Returns this string with its trailing line separator removed if it's contained in
     * the specified [lineSeparators] (default: [Common]), or returns this string otherwise.
     */
    public fun String.removeTrailingLineSeparator(vararg lineSeparators: String = Common): String =
        getTrailingLineSeparatorOrNull(lineSeparators = lineSeparators)?.let { removeSuffix(it) } ?: this

    /**
     * Returns this character sequence with the specified [lineSeparator] (default: any) appended
     * if it's not already present and contained in
     * the specified [lineSeparators] (default: [Common]).
     */
    public fun CharSequence.withTrailingLineSeparator(lineSeparator: String? = null, vararg lineSeparators: String = Common): CharSequence =
        when (lineSeparator) {
            null -> {
                if (endsWithLineSeparator(lineSeparators = lineSeparators)) this
                else toString() + Default
            }

            else -> {
                if (lineSeparator == getTrailingLineSeparatorOrNull(lineSeparators = lineSeparators)) this
                else removeTrailingLineSeparator(lineSeparators = lineSeparators).toString() + lineSeparator
            }
        }

    /**
     * Returns this string with the specified [lineSeparator] (default: any) appended
     * if it's not already present and contained in
     * the specified [lineSeparators] (default: [Common]).
     */
    public fun String.withTrailingLineSeparator(lineSeparator: String? = null, vararg lineSeparators: String = Common): String =
        when (lineSeparator) {
            null -> {
                if (endsWithLineSeparator(lineSeparators = lineSeparators)) this
                else this + Default
            }

            else -> {
                if (lineSeparator == getTrailingLineSeparatorOrNull(lineSeparators = lineSeparators)) this
                else removeTrailingLineSeparator(lineSeparators = lineSeparators) + lineSeparator
            }
        }
}
