package com.bkahlert.kommons.kaomoji

import com.bkahlert.kommons.ansiRemoved
import com.github.ajalt.mordant.rendering.AnsiLevel.TRUECOLOR
import com.github.ajalt.mordant.table.horizontalLayout
import com.github.ajalt.mordant.terminal.Terminal

private val terminal: Terminal by lazy { Terminal(TRUECOLOR).also { it.info.updateTerminalSize() } }
internal val CharSequence.columns: Int get() = horizontalLayout { cell(this@columns) }.measure(terminal).max

internal fun CharSequence.padEndColumns(length: Int, padChar: Char = ' '): String {
    val ansiRemoved = ansiRemoved
    return toString() + ansiRemoved.padEnd(length, padChar).drop(ansiRemoved.length)
}
