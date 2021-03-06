package koodies.text

import koodies.text.Unicode.Emojis.Emoji
import koodies.time.Now
import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.test.runtime.java.api.perf.graphemesBaseListener
import org.antlr.v4.test.runtime.java.api.perf.graphemesLexer
import org.antlr.v4.test.runtime.java.api.perf.graphemesParser
import org.antlr.v4.test.runtime.java.api.perf.graphemesParser.GraphemesContext
import java.util.ArrayList
import java.util.Objects

/**
 * Representation of a letter as perceived by a user.
 *
 * Graphemes are actually the smallest distinctive unit in a writing system
 * and consist of 1 or more instances of [CodePoint].
 */
public inline class Grapheme(public val codePoints: List<CodePoint>) {

    public constructor(charSequence: CharSequence) : this(
        charSequence.asCodePointSequence().toList()
            .also { require(count(charSequence) == 1) { "$it does not represent a single grapheme" } })

    public val isWhitespace: Boolean get() = codePoints.size == 1 && codePoints[0].isWhitespace
    public val isAlphanumeric: Boolean get() = codePoints.size == 1 && codePoints[0].isAlphanumeric

    /**
     * Contains the character pointed to and represented by a [String].
     */
    public val asString: String get() = codePoints.joinToString("") { it.string }

    override fun toString(): String = asString

    public companion object {
        /**
         * `true` if these [Char] instances represent a *single* grapheme.
         */
        public fun isGrapheme(chars: CharSequence): Boolean = chars.let {
            it.asGraphemeSequence().drop(1).firstOrNull() == null
        }

        public fun count(emoji: Emoji): Int = count("$emoji")
        public fun count(string: CharSequence): Int = count("$string")
        public fun count(string: String): Int = parseToResults(string).size

        public fun <T : CharSequence> T.getGrapheme(index: Int): String =
            asGraphemeSequence().drop(index).firstOrNull()?.asString ?: throw StringIndexOutOfBoundsException()

        public fun <T : CharSequence> T.getGraphemeCount(): Int = count(this)

        public fun CharSequence.asGraphemeSequence(): Sequence<Grapheme> = parseToSequence(toString())

        public fun toGraphemeList(string: String): List<Grapheme> =
            parseToResults(string).map { Grapheme(string.subSequence(it.stringOffset, it.stringOffset + it.stringLength)) }

        public fun parseToSequence(string: String): Sequence<Grapheme> = object : Sequence<Grapheme> {
            override fun iterator(): Iterator<Grapheme> = object : AbstractIterator<Grapheme>() {
                var offset: Int = 0
                override fun computeNext() =
                    if (offset < string.length) {
                        val remaining = string.substring(offset)
                        val result = parseFirstResult(remaining)
                        offset += result.stringLength
                        val grapheme = Grapheme(remaining.subSequence(0, result.stringLength))
                        setNext(grapheme)
                    } else {
                        done()
                    }

            }
        }

        private fun parseFirstResult(string: String): Result {
            val listener = GraphemesParsingListener(string, throwOnFirstExit = true)
            val result: Result = kotlin.runCatching {
                val lexer = object : graphemesLexer(CharStreams.fromString(string)) {
                    override fun getErrorListeners(): MutableList<out ANTLRErrorListener<in Int>> = mutableListOf()
                }
                val tokens = CommonTokenStream(lexer)
                val parser = graphemesParser(tokens)
                val tree: GraphemesContext = parser.graphemes()
                ParseTreeWalker.DEFAULT.walk(listener, tree)
                listener.results.first() // only for type-safety; never happens
            }.recover {
                check(it is RuntimeException) { "kk" }
                listener.results.first()
            }.getOrThrow()
            return result
        }

        private fun parseToResults(string: String): List<Result> {
            val listener = GraphemesParsingListener(string)
            kotlin.runCatching {
                val lexer = object : graphemesLexer(CharStreams.fromString(string)) {
                    override fun getErrorListeners(): MutableList<out ANTLRErrorListener<in Int>> = mutableListOf()
                }
                lexer.errorListeners.removeAll(lexer.errorListeners)
                val tokens = CommonTokenStream(lexer)
                val parser = graphemesParser(tokens)
                val tree: GraphemesContext = parser.graphemes()
                ParseTreeWalker.DEFAULT.walk(listener, tree)
            }
            return listener.results
        }
    }

    private class Result(val type: Type, val stringOffset: Int, val stringLength: Int) {
        override fun hashCode(): Int =
            Objects.hash(type, stringOffset, stringLength)

        override fun equals(other: Any?): Boolean {
            if (other !is Result) {
                return false
            }
            val that = other
            return type == that.type && stringOffset == that.stringOffset && stringLength == that.stringLength
        }

        override fun toString(): String = String.format("%s type=%s stringOffset=%d stringLength=%d", super.toString(), type, stringLength, stringOffset)

        enum class Type {
            EMOJI, NON_EMOJI
        }
    }

    /**
     * Handles converting code points to Java String UTF-16 offsets.
     */
    private class CodePointCounter(private val input: String) {
        var inputIndex = 0
        var codePointIndex = 0
        fun advanceToIndex(newCodePointIndex: Int): Int {
            while (codePointIndex < newCodePointIndex) {
                val codePoint = Character.codePointAt(input, inputIndex)
                inputIndex += Character.charCount(codePoint)
                codePointIndex++
            }
            return inputIndex
        }
    }

    private class GraphemesParsingListener(string: String, val throwOnFirstExit: Boolean = false) : graphemesBaseListener() {
        val results: MutableList<Result> = ArrayList()

        private val codePointCounter: CodePointCounter = CodePointCounter(string)
        private var clusterType: Result.Type? = null
        private var clusterStringStartIndex = 0

        override fun enterGrapheme_cluster(ctx: graphemesParser.Grapheme_clusterContext) {
            clusterType = Result.Type.NON_EMOJI
            clusterStringStartIndex = codePointCounter.advanceToIndex(ctx.getStart().startIndex)
        }

        override fun enterEmoji_sequence(ctx: graphemesParser.Emoji_sequenceContext) {
            clusterType = Result.Type.EMOJI
        }

        override fun exitGrapheme_cluster(ctx: graphemesParser.Grapheme_clusterContext) {
            val clusterStringStopIndex = codePointCounter.advanceToIndex(ctx.getStop().stopIndex + 1)
            val clusterStringLength = clusterStringStopIndex - clusterStringStartIndex
            results.add(Result(clusterType ?: throw IllegalStateException(), clusterStringStartIndex, clusterStringLength))
            if (throwOnFirstExit) throw RuntimeException()
        }
    }
}

public val Now.grapheme: Grapheme get() = Grapheme(emoji)
