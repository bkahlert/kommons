package koodies.text

import koodies.collections.Dictionary
import koodies.collections.dictOf

public object Whitespaces : Collection<String> {

    /** SPACE: Depends on font, typically 1/4 em, often adjusted */
    public const val SPACE: String = "\u0020"

    /** NO-BREAK SPACE: As a space, but often not adjusted */
    public const val NO_BREAK_SPACE: String = "\u00A0"

    /** OGHAM SPACE MARK: Unspecified; usually not really a space but a dash */
    public const val OGHAM_SPACE_MARK: String = "\u1680"

    /** MONGOLIAN VOWEL SEPARATOR: 0 */
    public const val MONGOLIAN_VOWEL_SEPARATOR: String = "\u180E"

    /** EN QUAD: 1 en (= 1/2 em) */
    public const val EN_QUAD: String = "\u2000"

    /** EM QUAD: 1 em (nominally, the height of the font) */
    public const val EM_QUAD: String = "\u2001"

    /** EN SPACE (nut): 1 en (= 1/2 em) */
    public const val EN_SPACE: String = "\u2002"

    /** EM SPACE (mutton): 1 em */
    public const val EM_SPACE: String = "\u2003"

    /** THREE-PER-EM SPACE (thick space): 1/3 em */
    public const val THREE_PER_EM_SPACE: String = "\u2004"

    /** FOUR-PER-EM SPACE (mid space): 1/4 em */
    public const val FOUR_PER_EM_SPACE: String = "\u2005"

    /** SIX-PER-EM SPACE: 1/6 em */
    public const val SIX_PER_EM_SPACE: String = "\u2006"

    /** FIGURE SPACE	fo: “Tabular width”, the width of digits */
    public const val FIGURE_SPACE_FO: String = "\u2007"

    /** PUNCTUATION SPACE: The width of a period “.” */
    public const val PUNCTUATION_SPACE: String = "\u2008"

    /** THIN SPACE: 1/5 em (or sometimes 1/6 em) */
    public const val THIN_SPACE: String = "\u2009"

    /** HAIR SPACE: Narrower than THIN SPACE */
    public const val HAIR_SPACE: String = "\u200A"

    /** ZERO WIDTH SPACE: 0 */
    public const val ZERO_WIDTH_SPACE: String = "\u200B"

    /** NARROW NO-BREAK SPACE	fo: Narrower than NO-BREAK SPACE (or SPACE), “typically the width of a thin space or a mid space” */
    public const val NARROW_NO_BREAK_SPACE_FO: String = "\u202F"

    /** MEDIUM MATHEMATICAL SPACE: 4/18 em */
    public const val MEDIUM_MATHEMATICAL_SPACE: String = "\u205F"

    /** IDEOGRAPHIC SPACE: The width of ideographic (CJK) characters. */
    public const val IDEOGRAPHIC_SPACE: String = "\u3000"

    /** ZERO WIDTH NO-BREAK SPACE: 0 */
    public const val ZERO_WIDTH_NO_BREAK_SPACE: String = "\uFEFF"

    public val Dict: Dictionary<String, String> by lazy {
        dictOf(
            SPACE to "SPACE",
            NO_BREAK_SPACE to "NO BREAK SPACE",
            OGHAM_SPACE_MARK to "OGHAM SPACE MARK",
//            MONGOLIAN_VOWEL_SEPARATOR to "MONGOLIAN VOWEL SEPARATOR", // not considered blank by Kotlin on JVM
            EN_QUAD to "EN QUAD",
            EM_QUAD to "EM QUAD",
            EN_SPACE to "EN SPACE",
            EM_SPACE to "EM SPACE",
            THREE_PER_EM_SPACE to "THREE PER EM SPACE",
            FOUR_PER_EM_SPACE to "FOUR PER EM SPACE",
            SIX_PER_EM_SPACE to "SIX PER EM SPACE",
            FIGURE_SPACE_FO to "FIGURE SPACE FO",
            PUNCTUATION_SPACE to "PUNCTUATION SPACE",
            THIN_SPACE to "THIN SPACE",
            HAIR_SPACE to "HAIR SPACE",
//            ZERO_WIDTH_SPACE to "ZERO WIDTH SPACE", // not considered blank by Kotlin on JVM
            NARROW_NO_BREAK_SPACE_FO to "NARROW NO BREAK SPACE FO",
            MEDIUM_MATHEMATICAL_SPACE to "MEDIUM MATHEMATICAL SPACE",
            IDEOGRAPHIC_SPACE to "IDEOGRAPHIC SPACE",
//            ZERO_WIDTH_NO_BREAK_SPACE to "ZERO WIDTH NO BREAK SPACE", // not considered blank by Kotlin on JVM
            default = { "UNKNOWN" })
    }

    private val ALL by lazy {
        Dict.keys.toTypedArray()
    }

    public val asCodePoints: List<CodePoint> by lazy {
        ALL.map { it.asCodePoint() ?: error("Failed to convert whitespace ${it.toUByte()}") }
    }

    public val asChars: List<Char> by lazy { ALL.map { it[0] } }

    override val size: Int by lazy { ALL.size }

    override fun contains(element: String): Boolean = ALL.contains(element)

    override fun containsAll(elements: Collection<String>): Boolean = ALL.toList().containsAll(elements)

    override fun isEmpty(): Boolean = ALL.isEmpty()

    override fun iterator(): Iterator<String> = ALL.iterator()

    /**
     * Replaces all lines separators by [SPACE].
     */
    public fun unify(charSequence: CharSequence): String =
        fold(charSequence.toString()) { acc, sep -> acc.replace(sep, SPACE) }

    /**
     * If this [CharSequence] ends with one or more of the [Whitespaces] this property includes them.
     */
    public val CharSequence.trailingWhitespaces: String get() :String = takeLastWhile { it in asChars }.toString()

    /**
     * If this [CharSequence] ends with one or more of the [Whitespaces] this property is `true`.
     */
    public val CharSequence.hasTrailingWhitespaces: Boolean get() = trailingWhitespaces.isNotEmpty()

    /**
     * If this [String] ends with one or more of the [Whitespaces] this property contains this [String] without it.
     */
    public val String.withoutTrailingWhitespaces: String
        get() = (this as CharSequence).trailingWhitespaces.takeUnlessEmpty()?.let { removeSuffix(it) } ?: this
}
