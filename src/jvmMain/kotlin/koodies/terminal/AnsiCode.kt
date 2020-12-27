package koodies.terminal

import com.github.ajalt.mordant.TermColors
import koodies.regex.namedGroups
import koodies.text.Unicode
import com.github.ajalt.mordant.AnsiCode as MordantAnsiCode

/**
 * An [AnsiCode] that provides access to the [openCodes] and [closeCode] of an [MordantAnsiCode].
 */
class AnsiCode(
    /**
     * All codes used to open a control sequence.
     */
    val openCodes: List<Int>,
    /**
     * The code needed to close a control sequence.
     */
    val closeCode: Int,
) : MordantAnsiCode(listOf(openCodes to closeCode)) {
    /**
     * Creates an [AnsiCode] using a [Pair] of which the [Pair.first]
     * element forms the codes used to open a control sequence and [Pair.second]
     * the code to close a control sequence.
     */
    constructor(pair: Pair<List<Int>, Int>) : this(pair.first, pair.second)

    /**
     * Creates an [AnsiCode] based on a [MordantAnsiCode] providing access
     * to the open and close codes of the latter.
     */
    constructor(mordantAnsiCode: MordantAnsiCode) : this(mordantAnsiCode.publicCodes)

    /**
     * Convenience view on all codes used by this [AnsiCode].
     */
    val allCodes: List<Int> by lazy { openCodes + closeCode }

    companion object {
        const val ESC = Unicode.escape // `ESC[` also called 7Bit Control Sequence Introducer
        const val CSI = Unicode.controlSequenceIntroducer
        private val termColors by lazy { TermColors(IDE.ansiSupport) }

        object colors {
            val black: AnsiCode = AnsiCode(termColors.black)
            val gray: AnsiCode = AnsiCode(termColors.gray)
            val red: AnsiCode = AnsiCode(termColors.red)
            val brightRed: AnsiCode = AnsiCode(termColors.brightRed)
            val green: AnsiCode = AnsiCode(termColors.green)
            val brightGreen: AnsiCode = AnsiCode(termColors.brightGreen)
            val yellow: AnsiCode = AnsiCode(termColors.yellow)
            val brightYellow: AnsiCode = AnsiCode(termColors.brightYellow)
            val blue: AnsiCode = AnsiCode(termColors.blue)
            val brightBlue: AnsiCode = AnsiCode(termColors.brightBlue)
            val magenta: AnsiCode = AnsiCode(termColors.magenta)
            val brightMagenta: AnsiCode = AnsiCode(termColors.brightMagenta)
            val cyan: AnsiCode = AnsiCode(termColors.cyan)
            val brightCyan: AnsiCode = AnsiCode(termColors.brightCyan)
        }

        object formats {
            val bold: AnsiCode = AnsiCode(termColors.bold)
            val dim: AnsiCode = AnsiCode(termColors.dim)
            val italic: AnsiCode = AnsiCode(termColors.italic)
            val underline: AnsiCode = AnsiCode(termColors.underline)
            val inverse: AnsiCode = AnsiCode(termColors.inverse)
            val hidden: AnsiCode = AnsiCode(termColors.hidden)
        }


        /**
         * Partial Line Forward
         *
         * Deletes the line from the cursor position to the end of the line
         * (using all existing formatting).
         *
         * Usage:
         * ```
         * termColors.yellow.bg("Text on yellow background$PARTIAL_LINE_FORWARD")
         * ```
         */
        const val PARTIAL_LINE_FORWARD = "$ESC\\[K"

        const val splitCodeMarker = "👈 ansi code splitter 👉"

        /**
         * Returns the control sequence needed to close all [codes] that are
         * not already closed in the list.
         *
         * Think of it as a bunch of HTML tags of which a few have not been closed,
         * whereas this function returns the string that renders the HTML valid again.
         */
        fun closingControlSequence(codes: List<Int>): String = controlSequence(closingCodes(unclosedCodes(codes)))

        /**
         * Iterates through the codes and returns the ones that have no closing counterpart.
         *
         * Think of it as a bunch of HTML tags of which a few have not been closed,
         * whereas this function returns those tags that render the HTML invalid.
         */
        fun unclosedCodes(codes: List<Int>): List<Int> {
            val unclosedCodes = mutableListOf<Int>()
            codes.forEach { code ->
                val ansiCodes: List<AnsiCode> = codeToAnsiCodeMappings[code] ?: emptyList()
                ansiCodes.forEach { ansiCode ->
                    if (ansiCode.closeCode != code) {
                        unclosedCodes.addAll(ansiCode.openCodes)
                    } else {
                        unclosedCodes.removeAll { ansiCode.openCodes.contains(it) }
                    }
                }
            }
            return unclosedCodes
        }

        /**
         * Returns the codes needed to close the given ones.
         */
        fun closingCodes(codes: List<Int>): List<Int> =
            codes.flatMap { openCode -> codeToAnsiCodeMappings[openCode]?.map { ansiCode -> ansiCode.closeCode } ?: emptyList() }

        /**
         * Returns the rendered control sequence for the given codes.
         */
        fun controlSequence(codes: List<Int>): String =
            MordantAnsiCode(codes, 0)(splitCodeMarker).split(splitCodeMarker)[0]

        /**
         * A map that maps the open and close codes of all supported instances of [AnsiCode]
         * to their respective [AnsiCode].
         */
        internal val codeToAnsiCodeMappings: Map<Int, List<AnsiCode>> by lazy {
            hashMapOf<Int, MutableList<AnsiCode>>().apply {
                with(termColors) {
                    sequenceOf(
                        arrayOf(black, red, green, yellow, blue, magenta, cyan, white, gray),
                        arrayOf(brightRed, brightGreen, brightYellow, brightBlue, brightMagenta, brightCyan, brightWhite),
                        arrayOf(reset, bold, dim, italic, underline, inverse, hidden, strikethrough)
                    )
                }
                    .flatMap { it.asSequence() }
                    .map { AnsiCode(it) }
                    .forEach {
                        it.allCodes.forEach { code ->
                            getOrPut(code, { mutableListOf() }).add(it)
                        }
                    }
            }
        }

        /**
         * [Regex] that matches an [AnsiCode].
         */
        val ansiCodeRegex: Regex = Regex("(?<CSI>$CSI|$ESC\\[)(?<parameterBytes>[0-?]*)(?<intermediateBytes>[ -/]*)(?<finalByte>[@-~])")

        /**
         * Provides (comparably expensive) access to the [Pair] of opening and closing [codes] of a [MordantAnsiCode].
         */
        val MordantAnsiCode.publicCodes: Pair<List<Int>, Int>
            get() = splitCodeMarker
                .let<String, List<String>> { this(it).split(it) }
                .map { it.parseAnsiCodesAsSequence().toList() }
                .let<List<List<Int>>, Pair<List<Int>, Int>> { (openCodes: List<Int>, closeCode: List<Int>) -> openCodes to closeCode.single() }

        /**
         * Searches this char sequence for [AnsiCode] and returns a stream of codes.
         *
         * *Note: This method makes no difference between opening and closing codes.
         */
        fun CharSequence.parseAnsiCodesAsSequence(): Sequence<Int> = ansiCodeRegex.findAll(this).flatMap { parseAnsiCode(it) }

        /**
         * Given a [matchResult] resulting from [ansiCodeRegex] all found ANSI codes are returned.
         *
         * *Note: This method makes no difference between opening and closing codes.
         */
        fun parseAnsiCode(matchResult: MatchResult): Sequence<Int> {
            val intermediateBytes = matchResult.namedGroups["intermediateBytes"]?.value ?: ""
            val lastByte = matchResult.namedGroups["finalByte"]?.value ?: ""
            return if (intermediateBytes.isBlank() && lastByte == "m") {
                matchResult.namedGroups["parameterBytes"]?.value?.split(";")?.mapNotNull {
                    kotlin.runCatching { it.toInt() }.getOrNull()
                }?.asSequence() ?: emptySequence()
            } else {
                emptySequence()
            }
        }

        /**
         * Returns the [String] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
         */
        fun CharSequence.removeEscapeSequences(): String = ansiCodeRegex.replace(this, "")

        /**
         * Returns the [String] with [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code) removed.
         */
        fun String.removeEscapeSequences(): String = (this as CharSequence).removeEscapeSequences()
    }
}
