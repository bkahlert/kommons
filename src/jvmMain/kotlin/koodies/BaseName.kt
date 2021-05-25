package koodies

import koodies.text.CharRanges
import koodies.text.CodePoint
import koodies.text.asCodePointSequence
import koodies.text.decapitalize
import koodies.text.randomString

/**
 * Creates a base name suited to be used as an ID for various programs
 * or as a filename. In contrast to a random string (which it will be
 * if `this` string is `null`) the returned base name will reflect
 * `this` string as good as possible.
 *
 * The resulting string will:
 * - have at least [minLength] characters
 * - all possibly invalid characters replaced
 * - start with a letter
 */
public fun String?.toBaseName(minLength: Int = 8): String {
    var firstCharReplaced = false
    val sanitizedChars: List<String> = this?.asCodePointSequence()?.withIndex()?.map { (index: Int, codePoint: CodePoint) ->
        if (index == 0 && !codePoint.isAtoz) {
            firstCharReplaced = true
            when (codePoint.char) {
                '0' -> "O"
                '1' -> "I"
                '2' -> "Z"
                '3' -> "B"
                '4' -> "R"
                '5' -> "P"
                '6' -> "G"
                '7' -> "Z"
                '8' -> "O"
                '9' -> "Y"
                else -> "X"
            }
        } else when {
            codePoint.isAsciiAlphanumeric -> codePoint.string
            "._-".contains(codePoint.string) -> codePoint.string
            codePoint.isWhitespace -> "-"
            else -> "_"
        }
    }?.toList() ?: emptyList()
    val fillUp = (minLength - sanitizedChars.size).takeIf { it > 0 }?.let { randomString(it, CharRanges.Alphanumeric) } ?: ""
    val decapitalize = (firstCharReplaced && sanitizedChars.drop(1).filter { it[0].isLetter() }.partition { it[0].isUpperCase() }
        .let { (upper, lower) -> lower.size > upper.size })
    return sanitizedChars.joinToString("", postfix = fillUp).let { if (decapitalize) it.decapitalize() else it }
}
