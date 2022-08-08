package com.bkahlert.kommons.text

import com.ibm.icu.lang.UCharacter
import java.nio.charset.MalformedInputException

/** The character pointed to and represented by a [String]. */
internal actual val CodePoint.string: String
    get() = buildString { appendCodePoint(value) }

/** Whether this code point is a letter. */
public actual val CodePoint.isLetter: Boolean
    get() = Character.isLetter(value)

/** Whether this code point is a digit. */
public actual val CodePoint.isDigit: Boolean
    get() = Character.isDigit(value)

/** Whether this code point is a [Unicode Space Character](http://www.unicode.org/versions/Unicode13.0.0/ch06.pdf). */
public actual val CodePoint.isWhitespace: Boolean
    get() = Character.isWhitespace(value)

/** The name of this code point. */
public val CodePoint.name: String
    get() = UCharacter.getName(value) ?: "0x${Integer.toHexString(value).uppercase()}"

internal actual fun CharacterCodingException(inputLength: Int): CharacterCodingException = MalformedInputException(inputLength)
