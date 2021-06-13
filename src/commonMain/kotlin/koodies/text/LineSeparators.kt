package koodies.text

import koodies.regex.or
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
     * Same line separator as used by Kotlin.
     */
    public val DEFAULT: String = StringBuilder().appendLine().toString()

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

    private val ALL by lazy { arrayOf(CRLF, LF, CR, NEL, PS, LS) }

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
     * If this character sequence consists of more than one line this property is `true`.
     */
    public val CharSequence?.isMultiline: Boolean get() = lines().size > 1

    /**
     * Splits this character sequence to a sequence of lines delimited by any of the [LineSeparators].
     *
     * If the lines returned do include terminating line separators is specified by [keepDelimiters].
     *
     * If the last last is empty, it will be ignored unless [ignoreTrailingSeparator] is provided.
     */
    public fun CharSequence?.lineSequence(
        ignoreTrailingSeparator: Boolean = false,
        keepDelimiters: Boolean = false,
    ): Sequence<String> =
        this?.splitToSequence(
            delimiters = ALL,
            keepDelimiters = keepDelimiters,
            ignoreTrailingSeparator = ignoreTrailingSeparator
        ) ?: emptySequence()

    /**
     * Splits this character sequence to a list of lines delimited by any of the [LineSeparators].
     *
     * If the lines returned do include terminating line separators is specified by [keepDelimiters].
     *
     * If the last last is empty, it will be ignored unless [ignoreTrailingSeparator] is provided.
     */
    public fun CharSequence?.lines(
        ignoreTrailingSeparator: Boolean = false,
        keepDelimiters: Boolean = false,
    ): List<String> =
        this?.lineSequence(
            ignoreTrailingSeparator = ignoreTrailingSeparator,
            keepDelimiters = keepDelimiters
        )?.toList() ?: emptyList()


    /**
     * Auto-detects the line separator used in the given character sequence.
     */
    public fun autoDetect(charSequence: CharSequence): String {
        val histogram: MutableMap<String, Int?> = associateWith { null }.toMutableMap()
        var offset: Int? = 0
        while (offset != null) {
            offset = charSequence.findAnyOf(this, offset)?.let { (pos, sep) ->
                histogram[sep] = (histogram[sep] ?: 0) + 1
                pos + sep.length
            }
        }
        return histogram
            .filterValues { it != null }
            .maxByOrNull { (_, count) -> count ?: 0 }
            ?.key ?: DEFAULT
    }

    /**
     * Replaces all lines separators by [DEFAULT].
     */
    public fun unify(charSequence: CharSequence): String =
        fold(charSequence.toString()) { acc, sep -> acc.replace(sep, DEFAULT) }

    /**
     * If this character sequence starts with one of the [LineSeparators] this property includes it.
     */
    public val CharSequence.leadingLineSeparator: String? get() :String? = ALL.firstOrNull { startsWith(it) }

    /**
     * If this character sequence starts with one of the [LineSeparators] this property is `true`.
     */
    public val CharSequence.hasLeadingLineSeparator: Boolean get() = leadingLineSeparator != null

    /**
     * If this character sequence starts with one of the [LineSeparators] this property contains this string without it.
     */
    public val CharSequence.removeLeadingLineSeparator: String
        get() = leadingLineSeparator?.let { removePrefix(it).toString() } ?: toString()

    /**
     * If this character sequence ends with one of the [LineSeparators] this property includes it.
     */
    public val CharSequence.trailingLineSeparator: String? get() :String? = ALL.firstOrNull { endsWith(it) }

    /**
     * If this character sequence ends with one of the [LineSeparators] this property is `true`.
     */
    public val CharSequence.hasTrailingLineSeparator: Boolean get() = trailingLineSeparator != null

    /**
     * If this character sequence ends with one of the [LineSeparators] this property contains this string without it.
     */
    public val CharSequence.removeTrailingLineSeparator: String
        get() = trailingLineSeparator?.let { removeSuffix(it).toString() } ?: toString()

    /**
     * If this string does not end with one of the [LineSeparators] this string appended
     * with the given [lineSeparator] (default: [autoDetect]) is returned.
     *
     * If [append] is set to false, `this` string is returned unchanged, which is handy
     * if the needed behaviour is dynamic.
     */
    public fun CharSequence.withTrailingLineSeparator(append: Boolean = true, lineSeparator: String = autoDetect(this)): String =
        if (append && !hasTrailingLineSeparator) toString() + lineSeparator else toString()

    /**
     * If this character sequence [isMultiline] this property contains the first line's line separator.
     */
    public val CharSequence.firstLineSeparator: String?
        get() = lineSequence(keepDelimiters = true).firstOrNull()?.trailingLineSeparator

    /**
     * If this character sequence [isMultiline] this property contains the first line's line separator length.
     */
    public val CharSequence.firstLineSeparatorLength: Int get() = firstLineSeparator?.length ?: 0

    /**
     * Applies the specified [block] on this character sequence without an eventually existing [trailingLineSeparator].
     */
    public fun CharSequence.runIgnoringTrailingLineSeparator(block: (CharSequence) -> CharSequence): String =
        trailingLineSeparator?.let { removeSuffix(it).let(block).toString() + trailingLineSeparator } ?: toString()

    /**
     * Maps each line of this character sequence using [transform].
     *
     * If this character sequence consists of but a single line this line is mapped.
     *
     * If this character sequence has a trailing line that trailing line is left unchanged.
     */
    public fun CharSequence?.mapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> CharSequence): String {
        if (this == null) return ""
        val mappedLines = lines().map(transform)
        val trailingLineSeparator = hasTrailingLineSeparator && ignoreTrailingSeparator
        return mappedLines
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
    public fun String?.mapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> CharSequence): String =
        (this as? CharSequence).mapLines(ignoreTrailingSeparator, transform)

    /**
     * Flat maps each line of this character sequence using [transform].
     *
     * If this character sequence consists of but a single line this line is mapped.
     *
     * If this character sequence has a trailing line that trailing line is left unchanged.
     */
    public fun <T : CharSequence?> T.flatMapLines(ignoreTrailingSeparator: Boolean = true, transform: (CharSequence) -> Iterable<T>): String {
        if (this == null) return ""
        return (hasTrailingLineSeparator && ignoreTrailingSeparator).let { trailingLineSeparator ->
            lines().map { line -> transform(line).joinToString(LF) }
                .let { if (trailingLineSeparator) it.dropLast(1) else it }
                .joinToString(LF)
                .let { if (trailingLineSeparator) it + LF else it }
        }
    }

    /**
     * Returns this character sequence with all lines of text it consists of prefixed with the given [prefix].
     */
    public fun CharSequence?.prefixLinesWith(prefix: CharSequence, ignoreTrailingSeparator: Boolean = true): String =
        mapLines(ignoreTrailingSeparator) { "$prefix$it" }

    /**
     * Returns a sequence of lines of which none is longer than [maxLength].
     */
    public fun CharSequence?.linesOfLengthSequence(maxLength: Int, ignoreTrailingSeparator: Boolean = false): Sequence<CharSequence> {
        if (this == null) return emptySequence()
        val lines = lineSequence(ignoreTrailingSeparator = ignoreTrailingSeparator)
        return lines.flatMap { line: String ->
            val sequence = line.chunkedSequence(maxLength) { it }
            if (ignoreTrailingSeparator) sequence
            else sequence.iterator().run { if (!hasNext()) sequenceOf("") else asSequence() }
        }
    }

    /**
     * Returns a list of lines of which none is longer than [maxLineLength].
     */
    public fun CharSequence?.linesOfLength(maxLineLength: Int, ignoreTrailingSeparator: Boolean = false): List<CharSequence> =
        linesOfLengthSequence(maxLineLength, ignoreTrailingSeparator).toList()

    /**
     * Returns a sequence of lines of which none occupies more than given [maxColumns].
     */
    public fun CharSequence?.linesOfColumnsSequence(maxColumns: Int, ignoreTrailingSeparator: Boolean = false): Sequence<CharSequence> {
        if (this == null) return emptySequence()
        val lines = lineSequence(ignoreTrailingSeparator = ignoreTrailingSeparator)
        return lines.flatMap { line: String ->
            val sequence = line.chunkedByColumnsSequence(maxColumns) { it }
            if (ignoreTrailingSeparator) sequence
            else sequence.iterator().run { if (!hasNext()) sequenceOf("") else asSequence() }
        }
    }

    /**
     * Returns a list of lines of which none occupies more than given [maxColumns].
     */
    public fun CharSequence?.linesOfColumns(maxColumns: Int, ignoreTrailingSeparator: Boolean = false): List<CharSequence> =
        linesOfColumnsSequence(maxColumns, ignoreTrailingSeparator).toList()

    /**
     * Returns a string consisting of lines of which each occupies exactly the given number of [columns].
     *
     * The last line is filled with whitespaces if necessary.
     */
    public fun CharSequence?.wrapLines(columns: Int): CharSequence =
        this?.linesOfColumnsSequence(columns)?.joinToString(LF) {
            val missingColumns = columns - it.columns
            it.toString() + " ".repeat(missingColumns)
        } ?: ""
}
