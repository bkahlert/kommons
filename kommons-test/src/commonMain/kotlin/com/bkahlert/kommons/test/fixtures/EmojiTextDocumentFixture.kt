package com.bkahlert.kommons.test.fixtures

/**
 * A [TextResourceFixture] encompassing differently composed emoji.
 *
 * ```
 * a // "LATIN SMALL LETTER A"
 * 𝕓 // "MATHEMATICAL DOUBLE-STRUCK SMALL B"
 * 🫠 // "MELTING FACE EMOJI"
 * 🇩🇪 // "REGIONAL INDICATOR SYMBOL LETTER D" + "REGIONAL INDICATOR SYMBOL LETTER E"
 * 👨🏾‍🦱 // "MAN" + "EMOJI MODIFIER FITZPATRICK TYPE-5" + "ZERO WIDTH JOINER" + "EMOJI COMPONENT CURLY HAIR"
 * 👩‍👩‍👦‍👦 // "WOMAN" + "ZERO WIDTH JOINER" + "WOMAN" + "ZERO WIDTH JOINER" + "BOY" + "ZERO WIDTH JOINER" + "BOY"
 * ```
 */
public object EmojiTextDocumentFixture : TextResourceFixture(
    "emoji.txt",
    "text/plain",
    0x61u.toByte(), // LATIN SMALL LETTER A
    0xf0u.toByte(), 0x9du.toByte(), 0x95u.toByte(), 0x93u.toByte(), // 𝕓
    0xf0u.toByte(), 0x9fu.toByte(), 0xabu.toByte(), 0xa0u.toByte(), // MELTING FACE EMOJI
    0xf0u.toByte(), 0x9fu.toByte(), 0x87u.toByte(), 0xa9u.toByte(), // REGIONAL INDICATOR SYMBOL LETTER D
    0xf0u.toByte(), 0x9fu.toByte(), 0x87u.toByte(), 0xaau.toByte(), // REGIONAL INDICATOR SYMBOL LETTER E
    0xf0u.toByte(), 0x9fu.toByte(), 0x91u.toByte(), 0xa8u.toByte(), // MAN
    0xf0u.toByte(), 0x9fu.toByte(), 0x8fu.toByte(), 0xbeu.toByte(), // EMOJI MODIFIER FITZPATRICK TYPE-5
    0xe2u.toByte(), 0x80u.toByte(), 0x8du.toByte(), // ZERO WIDTH JOINER
    0xf0u.toByte(), 0x9fu.toByte(), 0xa6u.toByte(), 0xb1u.toByte(), // EMOJI COMPONENT CURLY HAIR
    0xf0u.toByte(), 0x9fu.toByte(), 0x91u.toByte(), 0xa9u.toByte(), // WOMAN
    0xe2u.toByte(), 0x80u.toByte(), 0x8du.toByte(), // ZERO WIDTH JOINER
    0xf0u.toByte(), 0x9fu.toByte(), 0x91u.toByte(), 0xa9u.toByte(), // WOMAN
    0xe2u.toByte(), 0x80u.toByte(), 0x8du.toByte(), // ZERO WIDTH JOINER
    0xf0u.toByte(), 0x9fu.toByte(), 0x91u.toByte(), 0xa6u.toByte(), // BOY
    0xe2u.toByte(), 0x80u.toByte(), 0x8du.toByte(), // ZERO WIDTH JOINER
    0xf0u.toByte(), 0x9fu.toByte(), 0x91u.toByte(), 0xa6u.toByte(), // BOY
)
