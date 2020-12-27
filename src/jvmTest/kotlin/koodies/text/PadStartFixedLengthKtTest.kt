@file:Suppress("NonAsciiCharacters")

package koodies.text

import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class PadStartFixedLengthKtTest {

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
            dynamicContainer("always have same length",
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
