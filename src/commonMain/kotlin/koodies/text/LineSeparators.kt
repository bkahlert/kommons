package koodies.text

import koodies.regex.or
import koodies.text.LineSeparators.autoDetect
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
     */
    public fun CharSequence?.lineSequence(keepDelimiters: Boolean = false): Sequence<String> =
        this?.splitToSequence(delimiters = ALL, keepDelimiters = keepDelimiters) ?: emptySequence()

    /**
     * Splits this character sequence to a list of lines delimited by any of the [LineSeparators].
     *
     * If the lines returned do include terminating line separators is specified by [keepDelimiters].
     */
    public fun CharSequence?.lines(keepDelimiters: Boolean = false): List<String> =
        lineSequence(keepDelimiters = keepDelimiters).toList()


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
    public fun unify(charSequence: CharSequence, lineSeparator: String = DEFAULT): String =
        fold(charSequence.toString()) { acc, sep -> acc.replace(sep, lineSeparator) }

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
    public fun CharSequence.withTrailingLineSeparator(lineSeparator: String = autoDetect(this), append: Boolean = true): String =
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
    @Deprecated("check if this string having a trailing line separator is correct and avoid the need to call this method")
    public fun CharSequence.runIgnoringTrailingLineSeparator(block: CharSequence.() -> CharSequence): String =
        trailingLineSeparator?.let { removeSuffix(it).run(block).toString() + it } ?: run(block).toString()

    /**
     * Maps each line of this character sequence using the specified [transform].
     */
    public fun CharSequence?.mapLines(transform: (CharSequence) -> CharSequence): String {
        if (this == null) return ""
        return lines().joinToString(DEFAULT, transform = transform)
    }

    /**
     * Returns this character sequence with all lines of text it consists of prefixed with the given [prefix].
     */
    public fun CharSequence?.prefixLinesWith(prefix: CharSequence): String =
        mapLines { "$prefix$it" }

    /**
     * Returns a sequence of lines of which none is longer than [maxLength].
     */
    public fun CharSequence?.linesOfLengthSequence(maxLength: Int): Sequence<CharSequence> {
        if (this == null) return emptySequence()
        val lines = lineSequence()
        return lines.flatMap { line: String ->
            val iterator = line.chunkedSequence(maxLength) { it }.iterator()
            if (iterator.hasNext()) iterator.asSequence() else sequenceOf("")
        }
    }

    /**
     * Returns a list of lines of which none is longer than [maxLineLength].
     */
    public fun CharSequence?.linesOfLength(maxLineLength: Int): List<CharSequence> =
        linesOfLengthSequence(maxLineLength).toList()

    /**
     * Returns a sequence of lines of which none occupies more than given [maxColumns].
     */
    public fun CharSequence?.linesOfColumnsSequence(maxColumns: Int): Sequence<CharSequence> {
        if (this == null) return emptySequence()
        val lines = lineSequence()
        return lines.flatMap { line: String ->
            val iterator = line.chunkedByColumnsSequence(maxColumns) { it }.iterator()
            if (iterator.hasNext()) iterator.asSequence() else sequenceOf("")
        }
    }

    /**
     * Returns a list of lines of which none occupies more than given [maxColumns].
     */
    public fun CharSequence?.linesOfColumns(maxColumns: Int): List<CharSequence> =
        linesOfColumnsSequence(maxColumns).toList()

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
