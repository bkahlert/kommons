package koodies.text

/**
 * Contains the number of [Char] values needed to represent this code point.
 */
public actual val CodePoint.charCount: Int get() = if (codePoint >= Char.MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1
