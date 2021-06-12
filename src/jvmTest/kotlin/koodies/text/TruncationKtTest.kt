package koodies.text

import koodies.test.expectThrows
import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class TruncationKtTest {

    private val longText = "1234567890".repeat(1000)

    @Nested
    inner class Truncate {

        @Test
        fun `should truncate from center`() {
            expectThat("12345678901234567890".truncate()).isEqualTo("1234567…4567890")
        }

        @Test
        fun `should truncate using code points`() {
            expectThat("👨🏾👨🏾👨🏾👨🏾👨🏾".truncate(6)).isEqualTo("👨🏾👨…👨🏾")
        }

        @Test
        fun `should not truncate if not necessary`() {
            expectThat("1234567890".truncate()).isEqualTo("1234567890")
        }

        @Test
        fun `should truncate using custom marker`() {
            expectThat("12345678901234567890".truncate(marker = "...")).isEqualTo("123456...567890")
        }

        @Test
        fun `should truncate long text`() {
            expectThat(longText.truncate()).isEqualTo("1234567…4567890")
        }

        @Test
        fun `should throw if marker is wider than max length`() {
            expectThrows<IllegalArgumentException> {
                "1234567890".truncate(maxCodePoints = 1, marker = "XX")
            }
        }
    }

    @Nested
    inner class TruncateStart {

        @Test
        fun `should truncate from start`() {
            expectThat("12345678901234567890".truncateStart()).isEqualTo("…78901234567890")
        }

        @Test
        fun `should truncate using code points`() {
            expectThat("👨🏾👨🏾👨🏾👨🏾👨🏾".truncateStart(4)).isEqualTo("…🏾👨🏾")
        }

        @Test
        fun `should not truncate if not necessary`() {
            expectThat("1234567890".truncateStart()).isEqualTo("1234567890")
        }

        @Test
        fun `should truncate using custom marker`() {
            expectThat("12345678901234567890".truncateStart(marker = "...")).isEqualTo("...901234567890")
        }

        @Test
        fun `should truncate long text`() {
            expectThat(longText.truncateStart()).isEqualTo("…78901234567890")
        }

        @Test
        fun `should throw if marker is wider than max length`() {
            expectThrows<IllegalArgumentException> {
                "1234567890".truncateStart(maxCodePoints = 1, marker = "XX")
            }
        }
    }

    @Nested
    inner class TruncateEnd {

        @Test
        fun `should truncate from end`() {
            expectThat("12345678901234567890".truncateEnd()).isEqualTo("12345678901234…")
        }

        @Test
        fun `should truncate using columns`() {
            expectThat("👨🏾👨🏾👨🏾👨🏾👨🏾".truncateEnd(4)).isEqualTo("👨🏾👨…")
        }

        @Test
        fun `should not truncate if not necessary`() {
            expectThat("1234567890".truncateEnd()).isEqualTo("1234567890")
        }

        @Test
        fun `should truncate using custom marker`() {
            expectThat("12345678901234567890".truncateEnd(marker = "...")).isEqualTo("123456789012...")
        }

        @Test
        fun `should truncate long text`() {
            expectThat(longText.truncateEnd()).isEqualTo("12345678901234…")
        }

        @Test
        fun `should throw if marker is wider than max length`() {
            expectThrows<IllegalArgumentException> {
                "1234567890".truncateEnd(maxCodePoints = 1, marker = "XX")
            }
        }
    }

    @Nested
    inner class TruncateBy {
        @Test
        fun `should remove whitespaces from the right`() {
            expectThat("a   b   c".truncateBy(3)).isEqualTo("a  b c")
        }

        @Test
        fun `should use whitespaces on the right`() {
            expectThat("a   b   c    ".truncateBy(3)).isEqualTo("a   b   c ")
        }

        @Test
        fun `should use single whitespace on the right`() {
            expectThat("a   b   c ".truncateBy(1)).isEqualTo("a   b   c")
        }

        @Test
        fun `should not merge words`() {
            expectThat("a   b   c".truncateBy(10)).isEqualTo("a b c")
        }

        @Test
        fun `should consider all unicode whitespaces`() {
            val allWhitespaces = Unicode.whitespaces.joinToString("")
            expectThat("a ${allWhitespaces}b".truncateBy(allWhitespaces.length)).isEqualTo("a b")
        }

        @Test
        fun `should leave area before startIndex unchanged`() {
            expectThat("a   b   c".truncateBy(10, startIndex = 5)).isEqualTo("a   b c")
        }

        @Test
        fun `should leave whitespace sequence below minimal length unchanged`() {
            expectThat("a      b   c".truncateBy(3, minWhitespaceLength = 3)).isEqualTo("a   b   c")
        }

        @Test
        fun regression() {
            val x = "│   nested 1                                                                                            ▮▮"
            val y = "│   nested 1                                                                                      ▮▮"
            val z = "│   nested 1                                                                                         ▮▮"
            expectThat(x.truncateBy(3, minWhitespaceLength = 3)).isEqualTo(z).not { isEqualTo(y) }
        }
    }

    @Nested
    inner class TruncateTo {
        @Test
        fun `should remove whitespaces from the right`() {
            expectThat("a   b   c".truncateTo(6)).isEqualTo("a  b c")
        }

        @Test
        fun `should use whitespaces on the right`() {
            expectThat("a   b   c    ".truncateTo(10)).isEqualTo("a   b   c ")
        }

        @Test
        fun `should use single whitespace on the right`() {
            expectThat("a   b   c ".truncateTo(9)).isEqualTo("a   b   c")
        }

        @Test
        fun `should not merge words`() {
            expectThat("a   b   c".truncateTo(0)).isEqualTo("a b c")
        }

        @Test
        fun `should consider all unicode whitespaces`() {
            val allWhitespaces = Unicode.whitespaces.joinToString("")
            expectThat("a ${allWhitespaces}b".truncateTo(0)).isEqualTo("a b")
        }

        @Test
        fun `should leave area before startIndex unchanged`() {
            expectThat("a   b   c".truncateTo(0, startIndex = 5)).isEqualTo("a   b c")
        }

        @Test
        fun `should leave whitespace sequence below minimal length unchanged`() {
            expectThat("a      b   c".truncateTo(9, minWhitespaceLength = 3)).isEqualTo("a   b   c")
        }
    }

    @Nested
    inner class PadStartFixedLengthKtTest {

        @TestFactory
        fun `should truncate to 10 chars using ··· and _`() = testEach(
            "SomeClassName and a couple of words" to "Some···rds",
            "Short" to "_____Short",
        ) { (input, expected) ->
            expecting("\"$expected\" ？⃔ \"$input\"") { input.padStartFixedLength(10, "···", '_') } that { isEqualTo(expected) }
        }
    }

    @Nested
    inner class PadEndFixedLengthKtTest {

        @TestFactory
        fun `should truncate to 10 chars using ··· and _`() = testEach(
            "SomeClassName and a couple of words" to "Some···rds",
            "Short" to "Short_____",
        ) { (input, expected) ->
            expecting("\"$expected\" ？⃔ \"$input\"") { input.padEndFixedLength(10, "···", '_') } that { isEqualTo(expected) }
        }
    }
}
