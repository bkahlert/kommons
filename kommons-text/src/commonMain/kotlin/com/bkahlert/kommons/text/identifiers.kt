package com.bkahlert.kommons.text

import com.bkahlert.kommons.ansiRemoved

/**
 * Creates an identifier suited for applications that
 * ("only" respectively "at least") support the
 * letters `a` to `z`, `A` to `Z` and the digits `0` to `9`.
 *
 * In contrast to [randomString] the returned identifier
 * resembles the string derived from at much as possible.
 *
 * The resulting string:
 * - has at least [minLength] characters
 * - has problematic characters replaced
 * - start with a letter
 * - always be the same for the same input
 */
public fun CharSequence?.toIdentifier(minLength: Int = 8): String {
    val string = this?.ansiRemoved?.toString()
    var firstCharReplaced = false
    var lowerCaseLetters = 0
    var upperCaseLetters = 0

    val identifier = buildString {
        (string?.asCodePointSequence() ?: emptySequence()).forEachIndexed { index: Int, codePoint: CodePoint ->
            val char = if (index == 0 && !codePoint.isAtoz) {
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
                codePoint.isAsciiAlphanumeric || "._-".contains(codePoint.string) -> {
                    codePoint.apply {
                        char?.also { if (it.isUpperCase()) upperCaseLetters++ else if (it.isLowerCase()) lowerCaseLetters++ }
                    }.string
                }

                codePoint.isWhitespace -> "-"
                else -> "_"
            }
            append(char)
        }

        val missing = minLength - length
        if (missing > 0) {
            val fill = toString().hashCode().toString()
            for (i in 0 until missing) this.append(fill[i.mod(fill.length)])
        }
    }

    return if (firstCharReplaced && lowerCaseLetters > upperCaseLetters) identifier.replaceFirstChar { it.lowercase() } else identifier
}
