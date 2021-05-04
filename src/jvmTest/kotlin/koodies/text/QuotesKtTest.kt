package koodies.text

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class QuotesKtTest {

    @Nested
    inner class QuotedKtTest {

        @Test
        fun `should wrap string with double quotes`() {
            expectThat("text".quoted).isEqualTo("\"text\"")
        }

        @Test
        fun `should wrap empty string with double quotes`() {
            expectThat("".quoted).isEqualTo("\"\"")
        }

        @Test
        fun `should wrap null replacement character with double quotes on null`() {
            expectThat(null.quoted).ansiRemoved.isEqualTo("\"\u2400\"")
        }

        @Test
        fun `should wrap string with single quotes`() {
            expectThat("text".singleQuoted).isEqualTo("'text'")
        }

        @Test
        fun `should wrap empty string with single quotes`() {
            expectThat("".singleQuoted).isEqualTo("''")
        }

        @Test
        fun `should wrap null replacement character with single quotes on null`() {
            expectThat(null.singleQuoted).ansiRemoved.isEqualTo("'\u2400'")
        }
    }

    @Nested
    inner class WrapKtTest {

        @Test
        fun `should wrap string with independent strings`() {
            expectThat("text".wrap("<<", "➬")).isEqualTo("<<text➬")
        }

        @Test
        fun `should wrap string with equal strings`() {
            expectThat("text".wrap("⋆")).isEqualTo("⋆text⋆")
        }

        @Test
        fun `should wrap empty string with double quotes`() {
            expectThat("".wrap("⋆")).isEqualTo("⋆⋆")
        }

        @Test
        fun `should wrap null replacement character with double quotes on null`() {
            expectThat(null.wrap("⋆")).ansiRemoved.isEqualTo("⋆\u2400⋆")
        }
    }
}
