package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.UnicodeOld
import com.bkahlert.kommons.text.UnicodeOld.replacementSymbol
import com.bkahlert.kommons.text.asCodePointSequence
import com.bkahlert.kommons.toHexadecimalString

/**
 * Replaces control (e.g. [UnicodeOld.ESCAPE], surrogate (e.g. `\ubd00`) and whitespace (e.g. [UnicodeOld.LINE_FEED]) characters
 * with a visual representation or, if unavailable, with their written Unicode name.
 */
public fun String.replaceNonPrintableCharacters(): String =
    asCodePointSequence().map { codePoint ->
        val codePointIndex = codePoint.value
        val codePointChar: Char? = codePoint.char
        val codePointString = codePoint.toString()

        val prefix = if (codePointString in LineSeparators.Unicode) "⏎" else ""
        val suffix = if (codePointChar in UnicodeOld.controlCharacters.values) "ꜝ" else ""
        val infix = when {
            codePointChar == ' ' -> " "
            codePoint.replacementSymbol != null -> codePoint.replacementSymbol.toString()
            LineSeparators.Unicode.any { it == codePointString } -> when (codePointString) {
                LineSeparators.NEL -> "␤"
                LineSeparators.PS -> "ₛᷮ"
                LineSeparators.LS -> "ₛᷞ"
                else -> "⏎"
            }

            codePointChar in Char.MIN_HIGH_SURROGATE..Char.MAX_HIGH_SURROGATE -> codePointIndex.toHexadecimalString() + "▌﹍"
            codePointChar in Char.MIN_LOW_SURROGATE..Char.MAX_LOW_SURROGATE -> "﹍▐" + codePointIndex.toHexadecimalString()
            else -> codePointString
        }
        prefix + infix + suffix
    }.joinToString("")
