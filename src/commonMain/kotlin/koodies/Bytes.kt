package koodies

/**
 * Decodes a string from the bytes in UTF-8 encoding in this array of byte arrays.
 *
 * Malformed byte sequences are replaced by the replacement char `\uFFFD`.
 */
public fun Iterable<ByteArray>.decodeToString(): String =
    joinToString("") { it.decodeToString() }
