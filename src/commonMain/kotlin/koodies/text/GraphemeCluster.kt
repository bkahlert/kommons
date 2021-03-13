package koodies.text

/**
 * Sequence of one Unicode [CodePoint] or more
 * - that should be treated as a single unit
 * - and the end-user generally thinks of as a character.
 *
 * @see <a href="https://www.unicode.org/reports/tr29/#Grapheme_Cluster_Boundaries">Unicode® Standard Annex #29—UNICODE TEXT SEGMENTATION</a>
 */
public inline class GraphemeCluster(public val codePoints: List<CodePoint>) {

    public constructor(charSequence: CharSequence) : this(
        charSequence.asCodePointSequence().toList()
            .also { require(charSequence.isGraphemeCluster) { "$it does not represent a single grapheme" } })

    public val isWhitespace: Boolean get() = codePoints.size == 1 && codePoints[0].isWhitespace
    public val isAlphanumeric: Boolean get() = codePoints.size == 1 && codePoints[0].isAlphanumeric

    /**
     * Contains the character pointed to and represented by a [String].
     */
    public val asString: String get() = codePoints.joinToString("") { it.string }

    override fun toString(): String = asString
}

/**
 * `true` if these [Char] instances represent a *single* grapheme.
 */
public val CharSequence.isGraphemeCluster: Boolean
    get() = Regex("\\X").matches(this)

public fun CharSequence.getGrapheme(index: Int): String =
    asGraphemeClusterSequence().drop(index).firstOrNull()?.asString ?: throw IndexOutOfBoundsException()

private val graphemeClusterRegex: Regex = Regex(".+?\\b{g}")

/**
 * Returns a sequence containing the [GraphemeCluster] instances this string consists of.
 */
public fun CharSequence.asGraphemeClusterSequence(): Sequence<GraphemeCluster> =
    graphemeClusterRegex.findAll(this).flatMap { it.groupValues }.map { GraphemeCluster(it) }

/**
 * Returns a list containing the [GraphemeCluster] instances this string consists of.
 */
public fun CharSequence.toGraphemeClusterList(): List<GraphemeCluster> =
    asGraphemeClusterSequence().toList()

/**
 * Returns a sequence containing the [GraphemeCluster] instances this string consists of.
 */
public val CharSequence.graphemeClusterCount: Int
    get() = graphemeClusterRegex.findAll(this).flatMap { it.groupValues }.count()

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each [GraphemeCluster] of this string.
 */
public fun <R> String.mapGraphemeClusters(transform: (GraphemeCluster) -> R): List<R> =
    asGraphemeClusterSequence().map(transform).toList()
