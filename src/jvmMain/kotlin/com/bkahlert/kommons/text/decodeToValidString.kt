package com.bkahlert.kommons.text

import java.nio.charset.Charset

/**
 * Variant of [decodeToString] that converts this byte array to a valid
 * [String], that is, a [String] that end does not end with an illegal character.
 *
 * Due to the fact that some Unicode codepoints take up more than one byte
 * corrupted characters can occur which are ignored by this implementation.
 */
public fun ByteArray.decodeToValidString(charset: Charset = Charsets.UTF_8): String {
    var decoded = decodeToString()
    var skipBytes = 1
    while (skipBytes <= size && decoded.endsWithReplacementCharacter) {
        decoded = String(this, 0, lastIndex - skipBytes, charset)
        skipBytes++
    }
    return decoded
}

public val CharSequence.endsWithReplacementCharacter: Boolean
    get() = this.endsWith(Unicode.REPLACEMENT_CHARACTER)
