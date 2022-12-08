package com.bkahlert.kommons.text

/**
 * The character pointed to and represented by a [String].
 */
internal actual val CodePoint.string: String
    get() = if (value < 0x10000) {
        value.toChar().toString()
    } else {
        val off = 0xD800 - (0x10000 shr 10)
        val high = off + (value shr 10)
        val low = 0xDC00 + (value and 0x3FF)
        "${high.toChar()}${low.toChar()}"
    }

private fun GeneralCategory.Companion.values(startingWith: String) =
    GeneralCategory.values().filter { it.name.startsWith(startingWith) }.toTypedArray()

private val letterClasses = GeneralCategory.values("L")
private val digitClasses = arrayOf(GeneralCategory.Nd)
private val whitespaceClasses = arrayOf(GeneralCategory.Zs)

/** Whether this code point is a letter. */
public actual val CodePoint.isLetter: Boolean
    get() = UnicodeData[value] in letterClasses

/** Whether this code point is a digit. */
public actual val CodePoint.isDigit: Boolean
    get() = UnicodeData[value] in digitClasses

/** Whether this code point is a [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf). */
public actual val CodePoint.isWhitespace: Boolean
    get() = UnicodeData[value] in whitespaceClasses

internal actual fun CharacterCodingException(inputLength: Int): CharacterCodingException = CharacterCodingException("Input length = $inputLength")
