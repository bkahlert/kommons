package com.bkahlert.kommons.text

import com.github.ajalt.mordant.rendering.AnsiLevel.TRUECOLOR
import com.github.ajalt.mordant.table.row
import com.github.ajalt.mordant.terminal.Terminal

/** An [Iterator] that iterates [Grapheme] positions. */
public actual class GraphemeBreakIterator actual constructor(
    text: CharSequence,
) : BreakIterator, AbstractIterator<Int>() {

    private val codePoints = text.toCodePointList().toMutableList()
    private var pos = 0

    override fun computeNext() {
        if (codePoints.isEmpty()) {
            done()
            return
        }

        val candidate = codePoints[0]
        if (candidate.isRegionalIndicatorSymbol && codePoints.getOrNull(1)?.isRegionalIndicatorSymbol == true) {
            pos += codePoints.removeAndCountCharacters(2)
            setNext(pos)
            return
        }

        val candidateColumns = candidate.columns
        var next = 1
        while (++next <= codePoints.size) {
            val codePoint = codePoints[next - 1]
            when {
                codePoint.isZwj -> {
                    next++
                    continue
                }

                codePoint.isSkinToneModifier -> {
                    continue
                }

                else -> {
                    val currentColumns = codePoints.take(next).columns
                    if (currentColumns > candidateColumns) break
                }
            }
        }
        pos += codePoints.removeAndCountCharacters(next - 1)
        setNext(pos)
    }
}

private fun MutableList<CodePoint>.removeAndCountCharacters(count: Int) = 0.until(count).sumOf { removeAt(0).length }
private val CodePoint.isRegionalIndicatorSymbol get() = value in 0x1F1E6..0x1F1FF
private val CodePoint.isZwj get() = value == Unicode.ZWJ.code
private val CodePoint.isSkinToneModifier get() = value in 0x1F3FB..0x1F3FF

internal val terminal: Terminal by lazy { Terminal(TRUECOLOR).also { it.info.updateTerminalSize() } }
internal val CharSequence.columns: Int get() = row { cell(this@columns) }.measure(terminal).max
internal val Iterable<CodePoint>.columns: Int get() = joinToString("").columns
