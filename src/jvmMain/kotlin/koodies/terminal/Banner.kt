package koodies.terminal

import koodies.number.mod
import koodies.terminal.AnsiCode.Companion.colors.cyan
import koodies.terminal.AnsiColors.brightCyan
import koodies.terminal.AnsiColors.brightMagenta
import koodies.terminal.AnsiColors.cyan

object Banner {
    private val delimiters = Regex("\\s+")
    private val capitalLetter = Regex("[A-Z]")
    private val palette = listOf(cyan, cyan, cyan, cyan, cyan, cyan)

    fun banner(text: String): String {
        return text.split(delimiters).mapIndexed { index, word ->
            if (index == 0) {
                val (first: String, second: String) = word.splitCamelCase()
                (ANSI.termColors.prefix + " " + first.toUpperCase().brightCyan() + " " + second.toUpperCase().cyan()).trim()
            } else {
                word.toUpperCase().brightMagenta()
            }
        }.joinToString(" ")
    }

    private fun String.splitCamelCase() =
        replace(capitalLetter) { match -> " " + match.value }
            .split(" ")
            .filter { it.isNotBlank() }
            .let { words -> words.first() to words.drop(1).joinToString("") { it.capitalize() } }
}

private fun <T : CharSequence> T.colorize(palette: List<AnsiCode>): String =
    mapIndexed { index, letter -> palette[index.mod(palette.size)].invoke("$letter") }.joinToString("")
