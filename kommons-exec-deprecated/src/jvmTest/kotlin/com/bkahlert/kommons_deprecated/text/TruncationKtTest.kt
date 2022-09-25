package com.bkahlert.kommons_deprecated.text

import com.bkahlert.kommons_deprecated.test.testEachOld
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class TruncationKtTest {

    @Nested
    inner class PadStartFixedLengthKtTest {

        @Suppress("NonAsciiCharacters")
        @TestFactory
        fun `should truncate to 10 chars using ··· and _`() = testEachOld(
            "SomeClassName and a couple of words" to "Some···rds",
            "Short" to "_____Short",
        ) { (input, expected) ->
            expecting("\"$expected\" ？⃔ \"$input\"") { input.padStartFixedLength(10, "···", '_') } that { isEqualTo(expected) }
        }
    }

    @Nested
    inner class PadEndFixedLengthKtTest {

        @Suppress("NonAsciiCharacters")
        @TestFactory
        fun `should truncate to 10 chars using ··· and _`() = testEachOld(
            "SomeClassName and a couple of words" to "Some···rds",
            "Short" to "Short_____",
        ) { (input, expected) ->
            expecting("\"$expected\" ？⃔ \"$input\"") { input.padEndFixedLength(10, "···", '_') } that { isEqualTo(expected) }
        }
    }
}
