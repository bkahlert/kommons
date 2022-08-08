package com.bkahlert.kommons.test.fixtures

/**
 * A [TextResourceFixture] encompassing different line separators
 * and at least one one-, two-, three, and four-byte UTF-8 encoded character.
 *
 * ```
 * a // "LATIN SMALL LETTER A" + "NEXT LINE (NEL)"
 * 𝕓 // "MATHEMATICAL DOUBLE-STRUCK SMALL B" + "CARRIAGE RETURN" + "LINE FEED"
 * ☰ // "TRIGRAM OF HEAVEN" + "LINE FEED"
 * 👋 // "WAVING HAND SIGN" + "LINE FEED"
 * ```
 */
public object UnicodeTextDocumentFixture : TextResourceFixture(
    "unicode.txt",
    "text/plain",
    0x61u.toByte(), // LATIN SMALL LETTER A
    0xC2u.toByte(), 0x85u.toByte(), // NEXT LINE (NEL)
    0xF0u.toByte(), 0x9Du.toByte(), 0x95u.toByte(), 0x93u.toByte(), // MATHEMATICAL DOUBLE-STRUCK SMALL B
    0x0Du.toByte(), // CARRIAGE RETURN
    0x0Au.toByte(), // LINE FEED
    0xE2u.toByte(), 0x98u.toByte(), 0xB0u.toByte(), // TRIGRAM FOR HEAVEN
    0x0Au.toByte(), // LINE FEED
    0xF0u.toByte(), 0x9Fu.toByte(), 0x91u.toByte(), 0x8Bu.toByte(), // WAVING HAND SIGN
    0x0Au.toByte(), // LINE FEED
)
