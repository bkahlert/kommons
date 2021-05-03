package koodies.text


import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue


class CharRangesTest {

    @TestFactory
    fun `alphanumeric contains`() = listOf(
        CharRanges.LowerCaseAtoZ to
            (listOf('a', 'b', 'c') to
                listOf('A', '1', '$')),
        CharRanges.UpperCaseAtoZ to
            (listOf('A', 'B', 'C') to
                listOf('a', '1', '$')),
        CharRanges.Numeric to
            (listOf('1', '2', '3') to
                listOf('A', 'a', '$')),
    ).flatMap { (characterRange, expectations) ->
        val (contained, notContained) = expectations
        listOf(
            dynamicContainer("contained in $characterRange",
                contained.map { char ->
                    dynamicTest("$char") {
                        expectThat(characterRange.contains<Any>(char)).isTrue()
                    }
                }),
            dynamicContainer("not contained in $characterRange",
                notContained.map { char ->
                    dynamicTest("$char") {
                        expectThat(characterRange.contains<Any>(char)).isFalse()
                    }
                })
        )
    }
}
