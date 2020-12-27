package koodies.debug

import koodies.number.toHexString
import koodies.terminal.AnsiColors.brightCyan
import koodies.terminal.AnsiColors.gray
import koodies.text.LineSeparators
import koodies.text.Unicode
import koodies.text.Unicode.replacementSymbol
import koodies.text.mapCodePoints
import koodies.text.wrap

inline val CharSequence?.debug: String
    get() = if (this == null) null.wrap("❬".brightCyan(), "❭".brightCyan())
    else toString().replaceNonPrintableCharacters().wrap("❬".brightCyan(), "⫻".brightCyan() + "${this.length}".gray() + "❭".brightCyan())
inline val <T> Iterable<T>?.debug: String get() = this?.joinToString("") { it.toString().debug }.debug
inline val List<Byte>?.debug: String get() = this?.toByteArray()?.let { bytes: ByteArray -> String(bytes) }.debug
inline val Char?.debug: String get() = this.toString().replaceNonPrintableCharacters().wrap("❬", "❭")
inline val Byte?.debug: String get() = this?.let { byte: Byte -> "❬$byte=${byte.toChar().toString().replaceNonPrintableCharacters()}❭" } ?: "❬null❭"
inline val Boolean?.debug: String get() = asEmoji
inline val Any?.debug: String
    get() =
        when (this) {
            null -> "❬null❭"
            is Iterable<*> -> this.debug
            is CharSequence -> this.debug
            is ByteArray -> toList().debug
            is Byte -> this.debug
            else -> toString().debug
        }

/**
 * Replaces control (e.g. [Unicode.escape], surrogate (e.g. `\ubd00`) and whitespace (e.g. [Unicode.lineFeed]) characters
 * with a visual representation or, if unavailable, with their written Unicode name.
 */
fun String.replaceNonPrintableCharacters(): String {
    return mapCodePoints { codePoint ->
        val prefix = if (codePoint.string in LineSeparators) "⏎" else ""
        val suffix = if (codePoint.char in Unicode.controlCharacters.values) "ꜝ" else ""
        prefix + when {
            codePoint.char == ' ' -> " "
            codePoint.replacementSymbol != null -> codePoint.replacementSymbol.toString()
            codePoint.string in LineSeparators -> "⏎"
            codePoint.isHighSurrogate -> codePoint.codePoint.toHexString(pad = true) + "▌﹍"
            codePoint.isLowSurrogate -> "﹍▐" + codePoint.codePoint.toHexString(pad = true)
            codePoint.isWhitespace || codePoint.char in Unicode.whitespaces -> "❲${codePoint.unicodeName}❳"
            else -> codePoint.string
        } + suffix
    }.joinToString("")
}
