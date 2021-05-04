package koodies.text

import koodies.regex.or
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.lineSequence
import koodies.text.LineSeparators.lines

/**
 * Collection of line separators themselves and various corresponding methods
 * like [lines] or [lineSequence]
 *
 * @see <a href="https://www.unicode.org/reports/tr18/#RL1.6">Unicode® Technical Standard #18—Line Boundaries</a>
 * @see <a href="https://www.unicode.org/reports/tr18/">Unicode® Technical Standard #18—UNICODE REGULAR EXPRESSIONS</a>
 */
public object LineSeparators : Collection<String> {

    /**
     * Line break as used on Windows systems.
     *
     * Representations: `\r\n`,  `␍␊`, `⏎`
     */
    public const val CRLF: String = Unicode.carriageReturn.toString() + Unicode.lineFeed.toString()

    /**
     * Line break as used on Unix systems and modern Mac systems.
     *
     * Representations: `\n`, `␊`, `⏎`
     *
     */
    public const val LF: String = Unicode.lineFeed.toString()

    /**
     * Line break as used on old Mac systems.
     *
     * Representations: `\r`, `␍`, `⏎`
     */
    public const val CR: String = Unicode.carriageReturn.toString()

    /**
     * Next line separator
     *
     * Representations: `␤`, `⏎`
     */
    public const val NEL: String = Unicode.nextLine.toString()

    /**
     * Paragraph separator
     *
     * Representations: `ₛᷮ`, `⏎`
     */
    public const val PS: String = Unicode.paragraphSeparator.toString()

    /**
     * Line separator
     *
     * Representations: `ₛᷞ`, `⏎`
     */
    public const val LS: String = Unicode.lineSeparator.toString()

    /**
     * [Regex] that matches all line separators.
     */
    public val REGEX: Regex by lazy {
        CRLF.toLiteralRegex() or "[${ALL.filter { it != CRLF }.map { it.toLiteralRegex() }.joinToString("")}]".toRegex()
    }

    /**
     * [Regex] that matches only strings that contain no line separators, e.g. the last line of a multi-line text.
     */
    public val LAST_LINE_REGEX: Regex by lazy { ".+$".toRegex() }

    private val ALL by lazy { arrayOf(CRLF, LF, CR, LS, PS, NEL) }

    override val size: Int by lazy { ALL.size }

    override fun contains(element: String): Boolean = ALL.contains(element)

    override fun containsAll(elements: Collection<String>): Boolean = ALL.toList().containsAll(elements)

    override fun isEmpty(): Boolean = ALL.isEmpty()

    override fun iterator(): Iterator<String> = ALL.iterator()

    /**
     * The maximum length a line separator handled can have.
     */
    public val MAX_LENGTH: Int by lazy { ALL.maxOf { it.length } }

    /**
     * If this [CharSequence] consists of more than one line this property is `true`.
     */
    public val CharSequence.isMultiline: Boolean get() = lines().size > 1


    /**
     * Splits this char sequence to a sequence of lines delimited by any of the [LineSeparators].
     *
     * If the lines returned do include terminating line separators is specified by [keepDelimiters].
     *
     * If the last last is empty, it will be ignored unless [ignoreTrailingSeparator] is provided.
     */
    public fun CharSequence.lineSequence(
        ignoreTrailingSeparator: Boolean = false,
        keepDelimiters: Boolean = false,
    ): Sequence<String> =
        splitToSequence(
            delimiters = ALL,
            keepDelimiters = keepDelimiters,
            ignoreTrailingSeparator = ignoreTrailingSeparator
        )

    /**
     * Splits this char sequence to a list of lines delimited by any of the [LineSeparators].
     *
     * If the lines returned do include terminating line separators is specified by [keepDelimiters].
     *
     * If the last last is empty, it will be ignored unless [ignoreTrailingSeparator] is provided.
     */
    public fun CharSequence.lines(
        ignoreTrailingSeparator: Boolean = false,
        keepDelimiters: Boolean = false,
    ): List<String> =
        lineSequence(
            ignoreTrailingSeparator = ignoreTrailingSeparator,
            keepDelimiters = keepDelimiters
        ).toList()

    /**
     * Replaces all lines separators by [LF].
     */
    public fun unify(charSequence: CharSequence): String =
        fold(charSequence.toString()) { acc, sep -> acc.replace(sep, LF) }

    /**
     * If this [CharSequence] starts with one of the [LineSeparators] this property includes it.
     */
    public val CharSequence.leadingLineSeparator: String? get() :String? = ALL.firstOrNull { startsWith(it) }

    /**
     * If this [CharSequence] starts with one of the [LineSeparators] this property is `true`.
     */
    public val CharSequence.hasLeadingLineSeparator: Boolean get() = leadingLineSeparator != null

    /**
     * If this [String] starts with one of the [LineSeparators] this property contains this [String] without it.
     */
    public val String.withoutLeadingLineSeparator: String
        get() = (this as CharSequence).leadingLineSeparator?.let { lineBreak -> removePrefix(lineBreak) } ?: this

    /**
     * If this [CharSequence] ends with one of the [LineSeparators] this property includes it.
     */
    public val CharSequence.trailingLineSeparator: String? get() :String? = ALL.firstOrNull { endsWith(it) }

    /**
     * If this [CharSequence] ends with one of the [LineSeparators] this property is `true`.
     */
    public val CharSequence.hasTrailingLineSeparator: Boolean get() = trailingLineSeparator != null

    /**
     * If this [String] ends with one of the [LineSeparators] this property contains this [String] without it.
     */
    public val String.withoutTrailingLineSeparator: String
        get() = (this as CharSequence).trailingLineSeparator?.let { lineBreak -> removeSuffix(lineBreak) } ?: this

    /**
     * If this [String] does not end with one of the [LineSeparators] this string appended
     * with the given [lineSeparator] (default `\n`) is returned.
     *
     * Returns this [String] if either this string already ends with a line separator
     * or if [append] is set to `false`. This inconvenience toggle is handy
     * if the needed behaviour is dynamic.
     */
    public fun String.withTrailingLineSeparator(append: Boolean = true, lineSeparator: String = LF): String =
        if (append && !hasTrailingLineSeparator) this + lineSeparator else this

    /**
     * If this [CharSequence] [isMultiline] this property contains the first line's line separator.
     */
    public val CharSequence.firstLineSeparator: String?
        get() = lineSequence(keepDelimiters = true).firstOrNull()?.trailingLineSeparator

    /**
     * If this [CharSequence] [isMultiline] this property contains the first line's line separator length.
     */
    public val CharSequence.firstLineSeparatorLength: Int get() = firstLineSeparator?.length ?: 0

    /**
     * Maps each line of this char sequence using [transform].
     *
     * If this char sequence consists of but a single line this line is mapped.
     *
     * If this char sequence has a trailing line that trailing line is left unchanged.
     */
    public fun CharSequence.mapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> CharSequence): String =
        (hasTrailingLineSeparator && ignoreTrailingSeparator).let { trailingLineSeparator ->
            lines().map(transform)
                .let { if (trailingLineSeparator) it.dropLast(1) else it }
                .joinToString(LF)
                .let { if (trailingLineSeparator) it + LF else it }
        }

    /**
     * Maps each line of this string using [transform].
     *
     * If this string consists of but a single line this line is mapped.
     *
     * If this string has a trailing line that trailing line is left unchanged.
     */
    public fun String.mapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> CharSequence): String =
        (this as CharSequence).mapLines(ignoreTrailingSeparator, transform)

    /**
     * Flat maps each line of this char sequence using [transform].
     *
     * If this char sequence consists of but a single line this line is mapped.
     *
     * If this char sequence has a trailing line that trailing line is left unchanged.
     */
    public fun <T : CharSequence> T.flatMapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> Iterable<T>): String =
        (hasTrailingLineSeparator && ignoreTrailingSeparator).let { trailingLineSeparator ->
            lines().map { line -> transform(line).joinToString(LF) }
                .let { if (trailingLineSeparator) it.dropLast(1) else it }
                .joinToString(LF)
                .let { if (trailingLineSeparator) it + LF else it }
        }

    /**
     * Returns the [String] of what all lines of text are prefixed with the given [prefix].
     */
    public fun CharSequence.prefixLinesWith(prefix: CharSequence, ignoreTrailingSeparator: Boolean = true): String =
        mapLines(ignoreTrailingSeparator) { "$prefix$it" }

    /**
     * Breaks this char sequence to a sequence of strings of [maxLength].
     *
     * @param maxLength The maximum length of each returned line.
     */
    public fun CharSequence.breakLines(maxLength: Int, ignoreTrailingSeparator: Boolean = true): String {
        return flatMapLines(ignoreTrailingSeparator) { line ->
            line.chunked(maxLength)
        }
    }


    /**
     * Returns a sequence of lines of which none is longer than [maxLineLength].
     */
    public fun CharSequence.linesOfLengthSequence(maxLineLength: Int, ignoreTrailingSeparator: Boolean = false): Sequence<CharSequence> {
        val ansiString = this is AnsiString
        val lines = lineSequence(ignoreTrailingSeparator = ignoreTrailingSeparator)
        return lines.flatMap { line: String ->
            if (ansiString) {
                val seq: Sequence<AnsiString> = line.asAnsiString().chunkedSequence(maxLineLength)
                if (ignoreTrailingSeparator) seq
                else seq.iterator().run { if (!hasNext()) sequenceOf(AnsiString.EMPTY) else asSequence() }
            } else {
                val seq = line.chunkedSequence(maxLineLength)
                if (ignoreTrailingSeparator) seq
                else seq.iterator().run { if (!hasNext()) sequenceOf("") else asSequence() }
            }
        }
    }

    /**
     * Returns a list of lines of which none is longer than [maxLineLength].
     */
    public fun CharSequence.linesOfLength(maxLineLength: Int, ignoreTrailingSeparator: Boolean = false): List<CharSequence> =
        linesOfLengthSequence(maxLineLength, ignoreTrailingSeparator).toList()


    /**
     * Returns a string consisting of lines of which none is longer than [maxLineLength].
     */
    public fun CharSequence.wrapLines(maxLineLength: Int): CharSequence =
        linesOfLength(maxLineLength).joinToString(LF) { "$it" }
}
