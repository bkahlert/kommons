package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JvmStringsKtTest {

    @Suppress("SpellCheckingInspection")
    @Test fun transform() = testAll {
        withClue("composed") { "© А-З ÄÖÜäöüẞß".transform("de_DE", "de_DE-ASCII") shouldBe "(C) A-Z AEOEUeaeoeueSSss" }
        withClue("composed") { "ÄÖÜäöüẞß".transform("Any-Latin; de_De-ASCII") shouldBe "AEOEUeaeoeueSSss" }
        withClue("decomposed") { "ÄÖÜäöü".transform("Any-Latin; de_De-ASCII") shouldBe "AEOEUeaeoeue" }
        withClue("emojis") { "a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".transform("Any-Latin; de_De-ASCII") shouldBe "a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦" }
        withClue("illegal") { shouldThrow<IllegalArgumentException> { "a𝕓🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".transform("-illegal-") } }
        withClue("compound ID") {
            "The quick brown fox jumps over the lazy dog".transform(
                "NFKD", "Lower(Null)", "Latin-Katakana", "NFC", globalFilter = "[:Latin:]", globalInverseFilter = "[:Katakana:]",
            ) shouldBe "The quick brown fox jumps over the lazy dog".transform("[:Latin:]; NFKD; Lower(); Latin-Katakana; NFC; ([:Katakana:]);")
        }
    }
}
