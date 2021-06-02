package koodies.text

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
public value class GraphemeCluster private constructor(public val graphemes: List<CodePoint>) {
    private constructor(grapheme: CodePoint) : this(listOf(grapheme))

    override fun toString(): String {
        return graphemes.joinToString("") { grapheme -> grapheme.toString() }
    }

    public companion object {

        /**
         * Returns a description of the grapheme and the given [offset].
         * The [Pair.first] component describes the width of the found grapheme.
         * The [Pair.second] component is the number of code points this grapheme consists of.
         */
        @Suppress("NOTHING_TO_INLINE")
        internal inline fun Iterator<CodePoint>.readGraphemeCluster(firstCodePoint: CodePoint): Pair<GraphemeCluster, CodePoint?> {
            if (firstCodePoint.codePoint < 32 || (firstCodePoint.codePoint in 0x7f..0x9f)) {
                return GraphemeCluster(firstCodePoint) to if (hasNext()) next() else null
            }

            val graphemeCluster = StringBuilder(firstCodePoint.toString())
            val graphemes = mutableListOf(firstCodePoint)
            var remainder: CodePoint? = null
            val width = TextWidth.calculateWidth(graphemeCluster)
            while (hasNext()) {
                val codePoint = next()
                graphemeCluster.append(codePoint.toString())
                (codePoint == Unicode.zeroWidthJoiner.codePoint || TextWidth.calculateWidth(graphemeCluster) == width).also { sameWidth ->
                    if (sameWidth) {
                        graphemes.add(codePoint)
                    } else {
                        remainder = codePoint
                        return GraphemeCluster(graphemes) to remainder
                    }
                }
            }
            return GraphemeCluster(graphemes) to remainder
        }
    }
}

/**
 * Returns a lazily propagated sequence containing the [GraphemeCluster] instances this string consists of.
 *
 * Each grapheme instance is heuristically detected by measuring the width of the first
 * and its following characters for as long as the width does not change.
 */
public fun CharSequence.asGraphemeClusterSequence(): Sequence<GraphemeCluster> {
    if (isEmpty()) return emptySequence()
    val codePoints: Iterator<CodePoint> = asCodePointSequence().iterator()
    var remainder: CodePoint? = codePoints.next()
    return generateSequence {
        remainder?.let {
            val (graphemeCluster, currentRemainder) = codePoints.readGraphemeCluster(it)
            remainder = currentRemainder
            graphemeCluster
        }
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
