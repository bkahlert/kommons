package koodies.text

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class RepeatKtTest {

    @TestFactory
    fun `should repeat`() = listOf(
        3 to ("AAA" to "ABC 🤯ABC 🤯ABC 🤯"),
        1 to ("A" to "ABC 🤯"),
        0 to ("" to ""),
    ).flatMap { (repeatCount, expectations) ->
        val (repeatedChar, repeatedString) = expectations
        listOf(
            dynamicTest("$repeatCount x A = $repeatedChar") {
                expectThat('A'.repeat(repeatCount)).isEqualTo(repeatedChar)
            },
            dynamicTest("$repeatCount x ABC🤯 = $repeatedString") {
                expectThat("ABC 🤯".repeat(repeatCount)).isEqualTo(repeatedString)
            },
        )
    }

    @Test
    fun `should return throw on negative Char repeat`() {
        expectCatching { 'A'.repeat(-1) }
    }

    @Test
    fun `should return throw on negative String repeat`() {
        expectCatching { "ABC 🤯".repeat(-1) }
    }
}
