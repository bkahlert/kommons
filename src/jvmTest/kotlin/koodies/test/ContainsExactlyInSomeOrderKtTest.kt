package koodies.test

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.opentest4j.AssertionFailedError
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure


class ContainsExactlyInSomeOrderKtTest {

    val list: List<String> = listOf("1", "2.a", "2.b", "3")

    @Nested
    inner class PassingAssertion {
        @TestFactory
        fun `should assert`() = listOf(
            arrayOf(listOf("1"), listOf("2.a", "2.b"), listOf("3")),
            arrayOf(listOf("1"), listOf("2.b", "2.a"), listOf("3")),
        ).map {
            dynamicTest("${it.toList()}") {
                expectThat(list).containsExactlyInSomeOrder(*it)
            }
        }

        @Test
        fun `should assert using builder`() {
            expectThat(list).containsExactlyInSomeOrder {
                +"1"
                +"2.b" + "2.a"
                +"3"
            }
        }
    }

    @Nested
    inner class FailingAssertion {
        @TestFactory
        fun `should not assert`() = listOf(
            arrayOf(listOf("3"), listOf("2.a", "2.b"), listOf("1")),
            arrayOf(listOf("1"), listOf("2.b"), listOf("2.a"), listOf("3")),
        ).map {
            dynamicTest("${it.toList()}") {
                expectCatching {
                    expectThat(list).containsExactlyInSomeOrder(*it)
                }.isFailure().isA<AssertionFailedError>()
            }
        }

        @Test
        fun `should not assert using builder`() {
            expectCatching {
                expectThat(list).containsExactlyInSomeOrder {
                    +"3"
                    +"2.b" + "2.a"
                    +"1"
                }
            }.isFailure().isA<AssertionFailedError>()
        }
    }
}
