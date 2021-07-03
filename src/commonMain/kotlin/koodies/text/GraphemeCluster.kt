package koodies.text

import koodies.collections.PeekingIterator
import koodies.collections.peekingIterator
import koodies.text.GraphemeCluster.Companion.readGraphemeCluster
import kotlin.jvm.JvmInline

/**
 * Representation of a [Unicode grapheme cluster](https://unicode.org/glossary/#grapheme_cluster)
 *
 * Instances can be created using [asGraphemeClusterSequence].
 *
 * @see <a href="https://unicode.org/reports/tr29/">Unicode® Technical Standard #18—UNICODE TEXT SEGMENTATION</a>
 */
@JvmInline
public value class GraphemeCluster private constructor(public val codePoints: List<CodePoint>) {
    private constructor(grapheme: CodePoint) : this(listOf(grapheme))

    override fun toString(): String {
        return codePoints.joinToString("") { grapheme -> grapheme.toString() }
    }

    public companion object {

        internal val lineSeparatorCodePoints = LineSeparators.filter { it.length == 1 }.mapNotNull { it.asCodePoint() }

        /**
         * Returns a description of the grapheme and the given [offset].
         * The [Pair.first] component describes the width of the found grapheme.
         * The [Pair.second] component is the number of code points this grapheme consists of.
         */
        @Suppress("NOTHING_TO_INLINE")
        internal inline fun PeekingIterator<CodePoint>.readGraphemeCluster(): GraphemeCluster {
            val firstCodePoint: CodePoint = next()
            if (firstCodePoint.codePoint < 32 || firstCodePoint.codePoint in 0x7f..0x9f || firstCodePoint in lineSeparatorCodePoints) {
                return if (firstCodePoint.char == '\r' && peekOrNull()?.char == '\n') GraphemeCluster(listOf(firstCodePoint, next()))
                else GraphemeCluster(firstCodePoint)
            }

            val graphemeCluster = StringBuilder(firstCodePoint.toString())
            val graphemes = mutableListOf(firstCodePoint)
            val maxWidth = TextWidth.calculateWidth(graphemeCluster) + TextWidth.COLUMN_SLACK
            var isEmoji = false

            while (hasNext()) {
                val codePoint = peek()
                graphemeCluster.append(codePoint.toString())
                if (isEmoji) {
                    if (codePoint.isWhitespace) isEmoji = false
                } else {
                    if (codePoint == Unicode.ZERO_WIDTH_JOINER.codePoint) isEmoji = true
                }

                if (isEmoji || TextWidth.calculateWidth(graphemeCluster) <= maxWidth) {
                    graphemes.add(next())
                } else {
                    return GraphemeCluster(graphemes)
                }
            }
            return GraphemeCluster(graphemes)
        }
    }
}

/**
 * If this character sequence represents a single grapheme cluster, returns it.
 * In all other cases, returns `null`.
 */
public fun CharSequence.asGraphemeCluster(): GraphemeCluster? = asGraphemeClusterSequence().singleOrNull()

/**
 * Returns a lazily propagated sequence containing the [GraphemeCluster] instances this string consists of.
 *
 * Each grapheme instance is heuristically detected by measuring the width of the first
 * and its following characters for as long as the width does not change.
 */
public fun CharSequence.asGraphemeClusterSequence(): Sequence<GraphemeCluster> {
    if (isEmpty()) return emptySequence()
    val codePoints: PeekingIterator<CodePoint> = asCodePointSequence().peekingIterator()
    return generateSequence {
        if (codePoints.hasNext()) codePoints.readGraphemeCluster()
        else null
    }
}

/**
 * Returns a list containing the [GraphemeCluster] instances this string consists of.
 */
public fun CharSequence.toGraphemeClusterList(): List<GraphemeCluster> =
    asGraphemeClusterSequence().toList()

/**
 * Returns a sequence containing the [GraphemeCluster] instances this string consists of.
 */
public val CharSequence.graphemeClusterCount: Int
    get() = asGraphemeClusterSequence().count()

/**
 * Returns a list containing the results of applying the given [transform] function
 * to each [GraphemeCluster] of this string.
 */
public fun <R> String.mapGraphemeClusters(transform: (GraphemeCluster) -> R): List<R> =
    asGraphemeClusterSequence().map(transform).toList()
