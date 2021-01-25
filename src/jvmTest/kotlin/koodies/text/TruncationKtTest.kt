package koodies.text

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class TruncationKtTest {

    @Nested
    inner class TruncationStrategyTest {

        @Nested
        inner class StartTruncation {

            @TestFactory
            fun `should truncate`() = listOf(
                "APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace" to "…AvailableSpace",
                "A pretty long sentence works, too." to "…ce works, too.",
            ).flatMap { (input, expected) ->
                listOf(
                    dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                        val actual = input.truncate(strategy = TruncationStrategy.START)
                        expectThat(actual).isEqualTo(expected)
                    },
                    dynamicTest("\"$expected\" ？⃔ length(15)") {
                        val actual = input.truncate(strategy = TruncationStrategy.START)
                        expectThat(actual).hasLength(15)
                    },
                )
            }

            @Test
            fun `should not truncate if not needed`() {
                val actual = "Too short".truncate(strategy = TruncationStrategy.START)
                expectThat(actual).isEqualTo("Too short")
            }
        }

        @Nested
        inner class MiddleTruncation {

            @TestFactory
            fun `should truncate`() = listOf(
                "APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace" to "APretty…leSpace",
                "A pretty long sentence works, too." to "A prett…s, too.",
            ).flatMap { (input, expected) ->
                listOf(
                    dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                        val actual = input.truncate(strategy = TruncationStrategy.MIDDLE)
                        expectThat(actual).isEqualTo(expected)
                    },
                    dynamicTest("\"$expected\" ？⃔ length(15)") {
                        val actual = input.truncate(strategy = TruncationStrategy.MIDDLE)
                        expectThat(actual).hasLength(15)
                    },
                )
            }

            @Test
            fun `should not truncate if not needed`() {
                val actual = "Too short".truncate(strategy = TruncationStrategy.MIDDLE)
                expectThat(actual).isEqualTo("Too short")
            }
        }

        @Nested
        inner class EndTruncation {

            @TestFactory
            fun `should truncate`() = listOf(
                "APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace" to "APrettyLongCla…",
                "A pretty long sentence works, too." to "A pretty long …",
            ).flatMap { (input, expected) ->
                listOf(
                    dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                        val actual = input.truncate(strategy = TruncationStrategy.END)
                        expectThat(actual).isEqualTo(expected)
                    },
                    dynamicTest("\"$expected\" ？⃔ length(15)") {
                        val actual = input.truncate(strategy = TruncationStrategy.END)
                        expectThat(actual).hasLength(15)
                    },
                )
            }

            @Test
            fun `should not truncate if not needed`() {
                val actual = "Too short".truncate(strategy = TruncationStrategy.END)
                expectThat(actual).isEqualTo("Too short")
            }
        }

        @Test
        fun `should truncate to max 15 at the end of the string using ellipsis`() {
            expectThat("APrettyLongClassNameThatMightBeTooBigForTheAvailableSpace".truncate()).isEqualTo("APrettyLongCla…")
        }
    }

    @Nested
    inner class Truncate {
        @Test
        fun `should truncate`() {
            expectThat("12345678901234567890".truncate()).isEqualTo("12345678901234…")
        }

        @Test
        fun `should not truncate if not necessary`() {
            expectThat("1234567890".truncate()).isEqualTo("1234567890")
        }

        @Test
        fun `should truncate using custom marker`() {
            expectThat("12345678901234567890".truncate(marker = "...")).isEqualTo("123456789012...")
        }

        @Test
        fun `should throw if marker is longer than max length`() {
            expectCatching {
                "1234567890".truncate(maxLength = 1, marker = "XX")
            }.isFailure().isA<StringIndexOutOfBoundsException>()
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

        @Suppress("NonAsciiCharacters")
        @TestFactory
        fun `should truncate to 10 chars using ··· and _`() = listOf(
            "SomeClassName and a couple of words" to "Some···rds",
            "Short" to "_____Short",
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                    val actual = input.padStartFixedLength(10, TruncationStrategy.MIDDLE, "···", '_')
                    expectThat(actual).isEqualTo(expected)
                },
                DynamicContainer.dynamicContainer("always have same length",
                    TruncationStrategy.values().map { strategy ->
                        val actual = input.padStartFixedLength(10, strategy, "···", '_')
                        dynamicTest("\"$actual\" ？⃔ \"$input\"") {
                            expectThat(actual).hasLength(10)
                        }
                    }.toList()
                ),
            )
        }
    }

    @Nested
    inner class PadEndFixedLengthKtTest {

        @Suppress("NonAsciiCharacters")
        @TestFactory
        fun `should truncate to 10 chars using ··· and _`() = listOf(
            "SomeClassName and a couple of words" to "Some···rds",
            "Short" to "Short_____",
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("\"$expected\" ？⃔ \"$input\"") {
                    val actual = input.padEndFixedLength(10, TruncationStrategy.MIDDLE, "···", '_')
                    expectThat(actual).isEqualTo(expected)
                },
                DynamicContainer.dynamicContainer("always have same length",
                    TruncationStrategy.values().map { strategy ->
                        val actual = input.padEndFixedLength(10, strategy, "···", '_')
                        dynamicTest("\"$actual\" ？⃔ \"$input\"") {
                            expectThat(actual).hasLength(10)
                        }
                    }.toList()
                ),
            )
        }
    }

}
