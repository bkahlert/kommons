package koodies.text

import org.jline.utils.WCWidth

/**
 * Contains the name of this code point
 */
public val CodePoint.unicodeName: String get() = Unicode[codePoint.toLong()]

/**
 * The Unicode name, e.g. `LINE SEPARATOR`
 */
public val CodePoint.formattedName: String get() = "❲$unicodeName❳"


/**
 * Contains the character pointed to and represented by a [CharArray].
 */
public val CodePoint.chars: CharArray get() = Character.toChars(codePoint)

/**
 * Contains the number of [Char] values needed to represent this code point.
 */
public val CodePoint.charCount: Int get() = Character.charCount(codePoint)

public operator fun String.minus(amount: Int): String =
    asCodePointSequence().map { it - amount }.joinToString("")

/**
 * Number of columns needed to represent the character described by this code point.
 */
public val CodePoint.columns: Int get() = WCWidth.wcwidth(codePoint)
