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

private val XRegExp = js("require('xregexp')")
private val letterRegexp = XRegExp("^\\p{L}$")
private val digitRegexp = XRegExp("^\\p{N}$")
private val whitespaceRegexp = XRegExp("^\\p{Zs}$")

/** Whether this code point is a letter. */
public actual val CodePoint.isLetter: Boolean
    get() = letterRegexp.test(string) as Boolean

/** Whether this code point is a digit. */
public actual val CodePoint.isDigit: Boolean
    get() = digitRegexp.test(value.toChar().toString()) as Boolean

/** Whether this code point is a [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf). */
public actual val CodePoint.isWhitespace: Boolean
    get() = whitespaceRegexp.test(value.toChar().toString()) as Boolean

internal actual fun CharacterCodingException(inputLength: Int): CharacterCodingException = CharacterCodingException("Input length = $inputLength")
