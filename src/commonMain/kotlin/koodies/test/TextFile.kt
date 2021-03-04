package koodies.test

/**
 * A text file [Fixture] encompassing the multi-string.
 *
 * ```
 * a // "LATIN SMALL LETTER A" + "NEXT LINE (NEL)"
 * 𝕓 // "MATHEMATICAL DOUBLE-STRUCK SMALL B" + "CARRIAGE RETURN" + "LINE FEED"
 * ☰ // "TRIGRAM OF HEAVEN" + "LINE FEED"
 * 👋 // "WAVING HAND SIGN" + "LINE FEED"
 * ```
 */
public object TextFile : Fixture by BinaryFixture.unsigned(
    "61C285F09D95930D0AE298B00AF09F918B0A.txt",
    0x61u, // LATIN SMALL LETTER A
    0xC2u, 0x85u, // NEXT LINE (NEL)
    0xF0u, 0x9Du, 0x95u, 0x93u, // MATHEMATICAL DOUBLE-STRUCK SMALL B
    0x0Du, // CARRIAGE RETURN
    0x0Au, // LINE FEED
    0xE2u, 0x98u, 0xB0u, // TRIGRAM FOR HEAVEN
    0x0Au, // LINE FEED
    0xF0u, 0x9Fu, 0x91u, 0x8Bu, // WAVING HAND SIGN
    0x0Au, // LINE FEED
)
