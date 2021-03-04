package koodies.text

/**
 * Collection of line separators themselves and various corresponding methods
 * like [lines] or [lineSequence].
 */
public object LineSeparators : Collection<String> {

    /**
     * Line break as used on Windows systems.
     *
     * Representations: `\r\n`,  ␍␊
     */
    public const val CRLF: String = Unicode.carriageReturn.toString() + Unicode.lineFeed.toString()

    /**
     * Line break as used on Unix systems and modern Mac systems.
     *
     * Representations: `\n`, ␊, ⏎
     *
     */
    public const val LF: String = Unicode.lineFeed.toString()

    /**
     * Line break as used on old Mac systems.
     *
     * Representations: `\r`,  ␍
     */
    public const val CR: String = Unicode.carriageReturn.toString()

    /**
     * Line separator
     */
    public const val LS: String = Unicode.lineSeparator.toString()

    /**
     * Paragraph separator
     */
    public const val PS: String = Unicode.paragraphSeparator.toString()

    /**
     * Next line separator
     */
    public const val NEL: String = Unicode.nextLine.toString()

    /**
     * [Regex] that matches all line separators.
     */
    public val SEPARATOR_PATTERN: Regex by lazy { "$CRLF|[${ALL.filter { it != CRLF }.joinToString("")}]".toRegex() }

    /**
     * [Regex] that matches only string that contain no line separators, e.g. the last line of a multi-line text.
     */
    public val LAST_LINE_PATTERN: Regex by lazy { ".+$".toRegex() }

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
     * If this [CharSequence] ends with one of the [LineSeparators] this property includes it.
     */
    public val CharSequence.trailingLineSeparator: String? get() :String? = ALL.firstOrNull { this.endsWith(it) }

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
     * If this [CharSequence] [isMultiline] this property contains the first line's line separator.
     */
    public val CharSequence.firstLineSeparator: String?
        get() = lineSequence(keepDelimiters = true).firstOrNull()?.trailingLineSeparator

    /**
     * If this [CharSequence] [isMultiline] this property contains the first line's line separator length.
     */
    public val CharSequence.firstLineSeparatorLength: Int get() = firstLineSeparator?.length ?: 0
}
