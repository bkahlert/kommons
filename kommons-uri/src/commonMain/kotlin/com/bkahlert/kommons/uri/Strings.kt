package com.bkahlert.kommons.uri

import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.encodeToByteArray

/**
 * Returns the character sequence parsed as a [Uri]
 * as specified in [RFC3986 Appendix B](https://www.rfc-editor.org/rfc/rfc3986#appendix-B), or
 * throws an [IllegalArgumentException] otherwise.
 */
public fun CharSequence.toUri(): Uri = Uri.parse(this)

/**
 * Returns the character sequence parsed as a [Uri]
 * as specified in [RFC3986 Appendix B](https://www.rfc-editor.org/rfc/rfc3986#appendix-B), or
 * `null` otherwise.
 */
public fun CharSequence.toUriOrNull(): Uri? = Uri.parseOrNull(this)

/**
 * Encodes this character sequence using the specified [charset].
 */
internal fun CharSequence.encodeToByteArray(charset: Charset): ByteArray =
    charset.newEncoder().encodeToByteArray(this)
