package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JvmCodePointKtTest {

    @Test fun name() = testAll {
        "a".asCodePoint().name shouldBe "LATIN SMALL LETTER A"
        "¶".asCodePoint().name shouldBe "PILCROW SIGN"
        "☰".asCodePoint().name shouldBe "TRIGRAM FOR HEAVEN"
        "𝕓".asCodePoint().name shouldBe "MATHEMATICAL DOUBLE-STRUCK SMALL B"
        "🫠".asCodePoint().name shouldBe "MELTING FACE"
    }
}
