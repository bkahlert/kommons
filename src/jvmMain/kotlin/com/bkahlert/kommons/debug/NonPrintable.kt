package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.Unicode
import com.bkahlert.kommons.text.Unicode.replacementSymbol
import com.bkahlert.kommons.text.Whitespaces
import com.bkahlert.kommons.text.mapCodePoints
import com.bkahlert.kommons.text.unicodeName
import com.bkahlert.kommons.toHexadecimalString

/**
 * Replaces control (e.g. [Unicode.ESCAPE], surrogate (e.g. `\ubd00`) and whitespace (e.g. [Unicode.LINE_FEED]) characters
 * with a visual representation or, if unavailable, with their written Unicode name.
 */
public fun String.replaceNonPrintableCharacters(): String {
    return mapCodePoints { codePoint ->
        val prefix = if (codePoint.string in LineSeparators) "⏎" else ""
        val suffix = if (codePoint.char in Unicode.controlCharacters.values) "ꜝ" else ""
        prefix + when {
            codePoint.char == ' ' -> " "
            codePoint.replacementSymbol != null -> codePoint.replacementSymbol.toString()
            codePoint.isLineSeparator -> when (codePoint.string) {
                LineSeparators.NEL -> "␤"
                LineSeparators.PS -> "ₛᷮ"
                LineSeparators.LS -> "ₛᷞ"
                else -> "⏎"
            }
            codePoint.isHighSurrogate -> codePoint.codePoint.toHexadecimalString() + "▌﹍"
            codePoint.isLowSurrogate -> "﹍▐" + codePoint.codePoint.toHexadecimalString()
            codePoint.isWhitespace -> "❲${codePoint.unicodeName}❳"
            codePoint.isZeroWidthWhitespace -> "❲${Whitespaces.ZeroWidthWhitespaces[codePoint.string]}❳"
            codePoint.isDefined -> codePoint.string
            else -> codePoint.string
        } + suffix
    }.joinToString("")
}
