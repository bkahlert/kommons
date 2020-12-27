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
object TextFile : UByteFixture(
    "61C285F09D95930D0AE298B00AF09F918B0A.txt",
    0x61u, // LATIN SMALL LETTER A
    0xC2U, 0x85U, // NEXT LINE (NEL)
    0xF0U, 0x9DU, 0x95U, 0x93U, // MATHEMATICAL DOUBLE-STRUCK SMALL B
    0x0DU, // CARRIAGE RETURN
    0x0AU, // LINE FEED
    0xE2U, 0x98U, 0xB0U, // TRIGRAM FOR HEAVEN
    0x0AU, // LINE FEED
    0xF0U, 0x9FU, 0x91U, 0x8BU, // WAVING HAND SIGN
    0x0AU, // LINE FEED
)
