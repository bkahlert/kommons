package koodies.text

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SplittingKtTest {

    @Nested
    inner class SplitAndMap {
        @Suppress("SpellCheckingInspection")
        @Test
        fun `should map substrings if delimiter exists`() {
            expectThat("abc,def".splitAndMap(",") { this + this }).isEqualTo("abcabc,defdef")
        }

        @Suppress("SpellCheckingInspection")
        @Test
        fun `should map string if delimiter not exists`() {
            expectThat("abc".splitAndMap(",") { this + this }).isEqualTo("abcabc")
        }
    }
}
