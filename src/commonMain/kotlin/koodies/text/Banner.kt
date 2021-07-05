package koodies.text

import koodies.text.ANSI.Colors
import koodies.text.ANSI.Text.Companion.ansi

public object Banner {
    private val prefix = with(Colors) {
        listOf(
            black to gray, cyan to brightCyan, blue to brightBlue, green to brightGreen, yellow to brightYellow, magenta to brightMagenta, red to brightRed,
        ).joinToString("") { (normal, bright) -> (normal.bg + bright)("░") }
    }
    private val delimiters = Regex("\\s+")
    private val capitalLetter = Regex("[A-Z]")

    public fun banner(text: CharSequence): String {
        return text.split(delimiters).mapIndexed { index, word ->
            if (index == 0) {
                val (first: String, second: String) = word.splitCamelCase()
                (prefix + " " + first.toUpperCase().ansi.brightCyan + " " + second.toUpperCase().ansi.cyan).trim()
            } else {
                word.toUpperCase().ansi.brightMagenta
            }
        }.joinToString(" ")
    }

    private fun String.splitCamelCase(): Pair<String, String> =
        replace(capitalLetter) { match -> " " + match.value }
            .split(" ")
            .filter { it.isNotBlank() }
            .let { words -> words.first() to words.drop(1).joinToString("") { it.capitalize() } }
}
