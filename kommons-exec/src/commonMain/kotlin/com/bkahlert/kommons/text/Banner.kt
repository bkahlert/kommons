package com.bkahlert.kommons.text

import com.bkahlert.kommons.text.ANSI.Colors
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi

public object Banner {
    private val prefix = with(Colors) {
        listOf(
            black to gray, cyan to brightCyan, blue to brightBlue, green to brightGreen, yellow to brightYellow, magenta to brightMagenta, red to brightRed,
        ).joinToString("") { (normal, bright) -> (normal.bg + bright)("▒") }
    }
    private val delimiters = Regex("\\s+")
    private val capitalLetter = Regex("[A-Z]")

    /**
     * Renders the given [text] with bright colors and the optional [prefix]
     * which defaults to a rainbow spanning from cyan to red.
     */
    public fun banner(text: CharSequence, prefix: String = Banner.prefix): String {
        return text.split(delimiters).mapIndexed { index, word ->
            if (index == 0) {
                val (first: String, second: String) = word.splitCamelCase()
                ((prefix.takeIf { it.isNotEmpty() }?.let { "$it " } ?: "") + first.uppercase().ansi.brightCyan + " " + second.uppercase().ansi.cyan).trim()
            } else {
                word.uppercase().ansi.brightMagenta
            }
        }.joinToString(" ")
    }

    private fun String.splitCamelCase(): Pair<String, String> =
        replace(capitalLetter) { match -> " " + match.value }
            .split(" ")
            .filter { it.isNotBlank() }
            .let { words -> words.first() to words.drop(1).joinToString("") { it.capitalize() } }
}
