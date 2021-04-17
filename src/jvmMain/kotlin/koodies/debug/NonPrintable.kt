package koodies.debug

import koodies.math.toHexadecimalString
import koodies.text.LineSeparators
import koodies.text.Unicode
import koodies.text.Unicode.replacementSymbol
import koodies.text.Whitespaces
import koodies.text.mapCodePoints
import koodies.text.unicodeName

/**
 * Replaces control (e.g. [Unicode.escape], surrogate (e.g. `\ubd00`) and whitespace (e.g. [Unicode.lineFeed]) characters
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
            codePoint.isHighSurrogate -> codePoint.codePoint.toHexadecimalString(pad = true) + "▌﹍"
            codePoint.isLowSurrogate -> "﹍▐" + codePoint.codePoint.toHexadecimalString(pad = true)
            codePoint.isWhitespace -> "❲${codePoint.unicodeName}❳"
            codePoint.isZeroWidthWhitespace -> "❲${Whitespaces.ZeroWidthWhitespaces[codePoint.string]}❳"
            codePoint.isDefined -> codePoint.string
            else -> codePoint.string
        } + suffix
    }.joinToString("")
}
