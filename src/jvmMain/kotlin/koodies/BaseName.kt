package koodies

import koodies.text.CharRanges
import koodies.text.CodePoint
import koodies.text.asCodePointSequence
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
 * - have no extension
 */
public fun String?.toBaseName(minLength: Int = 8): String {
    val sanitizedChars: List<String> = this?.asCodePointSequence()?.withIndex()?.map { (index: Int, codePoint: CodePoint) ->
        if (index == 0 && !codePoint.isAsciiAlphanumeric) "X"
        else when {
            codePoint.isAsciiAlphanumeric -> codePoint.string
            "._-".contains(codePoint.string) -> codePoint.string
            codePoint.isWhitespace -> "-"
            else -> "_"
        }
    }?.toList() ?: emptyList()
    val fillUp = (minLength - sanitizedChars.size).takeIf { it > 0 }?.let { randomString(it, CharRanges.Alphanumeric) } ?: ""
    return sanitizedChars.joinToString("", postfix = fillUp)
}
