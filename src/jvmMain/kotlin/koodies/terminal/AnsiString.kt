package koodies.terminal

import koodies.collections.synchronizedMapOf
import koodies.terminal.AnsiCode.Companion.closingControlSequence
import koodies.terminal.AnsiCode.Companion.controlSequence
import koodies.terminal.AnsiCode.Companion.parseAnsiCodesAsSequence
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiCode.Companion.unclosedCodes
import koodies.text.repeat
import kotlin.text.contains as containsRegex

/**
 * A char sequence which is [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) aware
 * and which does not break any sequence.
 *
 * The behaviour is as follows:
 * - escape sequences have length 0, that is, an [AnsiString] has the same length as its [String] counterpart
 * - [get] returns the unformatted char at the specified index
 * - [subSequence] returns the same char sequence as an unformatted [String] would do—but with the formatting ANSI escape sequences intact.
 * the sub sequence. Also escape sequences are ignored from [length].
 */
public open class AnsiString private constructor(public val string: String) : CharSequence {
    public constructor(charSequence: CharSequence) : this("$charSequence")

    private val tokens: Array<Pair<CharSequence, Int>> by lazy { string.tokenize() }

    public companion object {
        public val EMPTY: AnsiString = AnsiString("")

        private val ansiStringCache = synchronizedMapOf<Int, AnsiString>()
        public fun <T : CharSequence> T.asAnsiString(): AnsiString = when {
            this is AnsiString -> this
            this.isEmpty() -> EMPTY
            else -> ansiStringCache.computeIfAbsent(hashCode()) { AnsiString(this) }
        }

        private val tokenizationCache = synchronizedMapOf<Int, Array<Pair<CharSequence, Int>>>()
        public fun String.tokenize(): Array<Pair<CharSequence, Int>> = tokenizationCache.computeIfAbsent(hashCode()) {
            val tokens = mutableListOf<Pair<CharSequence, Int>>()
            val codes = mutableListOf<Int>()
            var consumed = 0
            while (consumed < length) {
                val match = AnsiCode.ansiCodeRegex.find(this, consumed)
                val range = match?.range
                if (range?.first == consumed) {
                    val ansiCodeString = this.subSequence(consumed, match.range.last + 1).also {
                        val currentCodes = AnsiCode.parseAnsiCode(match).toList()
                        codes.addAll(currentCodes)
                        consumed += it.length
                    }
                    tokens.add(ansiCodeString to 0)
                } else {
                    val first: Int? = range?.first
                    val ansiAhead = if (first != null) {
                        first < length
                    } else false
                    val substring1 = this.subSequence(consumed, if (ansiAhead) first!! else length)
                    val ansiCodeFreeString = substring1.also {
                        consumed += it.length
                    }
                    tokens.add(ansiCodeFreeString to ansiCodeFreeString.length)
                }
            }
            tokens.toTypedArray()
        }

        public val Array<Pair<CharSequence, Int>>.length: Int get():Int = sumBy { it.second }

        private fun Array<Pair<CharSequence, Int>>.subSequence(endIndex: Int): Pair<String, List<Int>> {
            if (endIndex == 0) return "" to emptyList()
            if (endIndex > length) throw IndexOutOfBoundsException(endIndex)
            var read = 0
            val codes = mutableListOf<Int>()
            val sb = StringBuilder()

            forEach { (token, tokenLength) ->
                val needed = endIndex - read
                if (needed > 0 && tokenLength == 0) {
                    sb.append(token)
                    codes.addAll(token.parseAnsiCodesAsSequence())
                } else {
                    if (needed <= tokenLength) {
                        sb.append(token.subSequence(0, needed))
                        return@subSequence "$sb" + closingControlSequence(codes) to unclosedCodes(codes)
                    }
                    sb.append(token)
                    read += tokenLength
                }
            }
            error("must not happen")
        }

        private val subSequenceCache = synchronizedMapOf<Pair<Int, Pair<Int, Int>>, String>()
        public fun Array<Pair<CharSequence, Int>>.subSequence(startIndex: Int, endIndex: Int): String =
            subSequenceCache.computeIfAbsent(hashCode() to (startIndex to endIndex)) {
                if (startIndex > 0) {
                    subSequence(startIndex).let { (prefix, unclosedCodes) ->
                        val (full, _) = subSequence(endIndex)
                        val controlSequence = controlSequence(unclosedCodes)
                        val startIndex1 = prefix.length - closingControlSequence(unclosedCodes).length
                        val endIndex1 = full.length
                        controlSequence + full.subSequence(startIndex1, endIndex1)
                    }
                } else {
                    subSequence(endIndex).first
                }
            }

        public fun Array<Pair<CharSequence, Int>>.getChar(index: Int): Char {
            if (index > length) throw IndexOutOfBoundsException(index)
            var read = 0
            forEach { (token, tokenLength) ->
                val needed = index - read
                if (tokenLength >= 0) {
                    if (needed <= tokenLength) {
                        return@getChar token[needed]
                    }
                    read += tokenLength
                }
            }
            error("must not happen")
        }

        public fun Array<Pair<CharSequence, Int>>.render(ansi: Boolean = true): String =
            if (ansi) subSequence(0, length)
            else filter { it.second != 0 }.joinToString("") { it.first }
    }

    /**
     * Contains this [string] with all ANSI escape sequences removed.
     */
    @Suppress("SpellCheckingInspection")
    public val unformatted: String by lazy { tokens.render(ansi = false) }

    /**
     * Returns the logical length of this string. That is, the same length as the unformatted [String] would return.
     */
    override val length: Int by lazy { unformatted.length }

    /**
     * Returns the unformatted char at the specified [index].
     *
     * Due to the limitation of a [Char] to two byte no formatted [Char] can be returned.
     */
    override fun get(index: Int): Char = unformatted[index]

    /**
     * Returns the same char sequence as an unformatted [String.subSequence] would do.
     *
     * Sole difference: The formatting ANSI escape sequences are kept.
     * Eventually open sequences will be closes at the of the sub sequence.
     */
    override fun subSequence(startIndex: Int, endIndex: Int): AnsiString =
        tokens.subSequence(startIndex, endIndex).asAnsiString()

    /**
     * Whether this [text] (ignoring eventually existing ANSI escape sequences)
     * is blank (≝ is empty or consists of nothing but whitespaces).
     */
    public fun isBlank(): Boolean = unformatted.isBlank()

    /**
     * Whether this [text] (ignoring eventually existing ANSI escape sequences)
     * is not blank (≝ is not empty and consists of at least one non-whitespace).
     */
    public fun isNotBlank(): Boolean = unformatted.isNotBlank()

    public fun toString(removeEscapeSequences: Boolean = false): String =
        if (removeEscapeSequences) unformatted
        else string

    override fun toString(): String = toString(false)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnsiString

        if (string != other.string) return false

        return true
    }

    override fun hashCode(): Int = string.hashCode()


    /**
     * Returns a ANSI string with content of this ANSI string padded at the beginning
     * to the specified [length] with the specified character or space.
     *
     * @param length the desired string length.
     * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
     * @return Returns an ANSI string of length at least [length] consisting of `this` ANSI string prepended with [padChar] as many times
     * as are necessary to reach that length.
     */
    public fun CharSequence.padStart(length: Int, padChar: Char = ' '): CharSequence {
        require(length >= 0) { "Desired length $length is less than zero." }
        return if (length <= this.length) this.subSequence(0, this.length)
        else padChar.repeat(length - this.length) + this
    }

    /**
     * Returns an ANSI string with content of this ANSI string padded at the end
     * to the specified [length] with the specified character or space.
     *
     * @param length the desired string length.
     * @param padChar the character to pad string with, if it has length less than the [length] specified. Space is used by default.
     * @return Returns an ANSI string of length at least [length] consisting of `this` ANSI string appended with [padChar] as many times
     * as are necessary to reach that length.
     */
    public fun padEnd(length: Int, padChar: Char = ' '): AnsiString {
        require(length >= 0) { "Desired length $length is less than zero." }
        return if (length <= this.length) this.subSequence(0, this.length)
        else this + padChar.repeat(length - this.length)
    }

    /**
     * Returns a sequence of strings of which each but possibly the last is of length [size].
     */
    public fun chunkedSequence(size: Int): Sequence<AnsiString> {
        check(size > 0)
        var processed = 0
        var unprocessed = length
        return generateSequence {
            if (unprocessed <= 0) {
                null
            } else {
                val take = size.coerceAtMost(unprocessed)
                subSequence(processed, processed + take).also {
                    processed += take
                    unprocessed -= take
                }
            }
        }
    }

    public operator fun plus(other: CharSequence): AnsiString = "$string$other".asAnsiString()
}

/**
 * Returns if this char sequence contains the specified [other] [CharSequence] as a substring.
 *
 * @param ignoreCase `true` to ignore character case when comparing strings. By default `false`.
 * @param ignoreAnsiFormatting ANSI formatting / escapes are ignored by default. Use `false` consider escape codes as well
 */
public fun <T : CharSequence> T.contains(
    other: CharSequence,
    ignoreCase: Boolean = false,
    ignoreAnsiFormatting: Boolean = false,
): Boolean =
    if (ignoreAnsiFormatting)
        removeEscapeSequences().containsRegex(other.removeEscapeSequences(), ignoreCase)
    else
        containsRegex(other, ignoreCase)
