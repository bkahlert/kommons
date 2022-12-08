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

private val letterClass = "L"
private val digitClass = "Nd"
private val whitespaceClass = "Zs"
private fun Array<IntArray>.contains(value: Int): Boolean {
    var offset = 0
    while (offset < size && value > get(offset)[1]) offset++
    return offset < size && value >= get(offset)[0]
}

/** Whether this code point is a letter. */
public actual val CodePoint.isLetter: Boolean
    get() = unicodeCategoryRanges.any { (cat, ranges) ->
        cat.startsWith(letterClass) && ranges.contains(value)
    }

/** Whether this code point is a digit. */
public actual val CodePoint.isDigit: Boolean
    get() = unicodeCategoryRanges[digitClass]?.contains(value) ?: false

/** Whether this code point is a [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf). */
public actual val CodePoint.isWhitespace: Boolean
    get() = unicodeCategoryRanges[whitespaceClass]?.contains(value) ?: false

internal actual fun CharacterCodingException(inputLength: Int): CharacterCodingException = CharacterCodingException("Input length = $inputLength")
