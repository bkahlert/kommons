package com.bkahlert.kommons.tracing.rendering

import com.bkahlert.kommons.exec.IOSequence
import com.bkahlert.kommons.exec.Process.State.Exited.Failed
import com.bkahlert.kommons.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons.exec.mock.ExecMock
import com.bkahlert.kommons.test.AnsiRequiring
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.text.Semantics.Symbols
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kommons.text.matchesCurlyPattern
import com.bkahlert.kommons.toSimpleString
import com.bkahlert.kommons.tracing.TestSpanScope
import com.bkahlert.kommons.tracing.runSpanning
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.time.Instant

class ReturnValueKtTest {
    private val failedReturnValue: ReturnValue = object : ReturnValue {
        override val successful: Boolean get() = false
        override val textRepresentation: String = "return value"
    }

    private val exception = RuntimeException("exception")

    private val succeededState = Succeeded(Instant.MIN, Instant.MAX, 12345L, IOSequence.EMPTY)
    private val failedState = Failed(Instant.MIN, Instant.MAX, 12345L, 42)

    private val successfulExpectations = listOf(
        null to Symbols.Null,
        "string" to "string",
        ExecMock.SUCCEEDED_EXEC to "Process {} terminated successfully at {}",
    )

    private val failedExpectations = listOf(
        failedReturnValue to "ϟ return value",
        RuntimeException("exception") to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        kotlin.runCatching { failedReturnValue } to "return value",
        kotlin.runCatching { throw exception } to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        failedState to "ϟ Process 12345 terminated with exit code 42.{{}}A dump has{{}}terminated with exit code 42"
    )

    private val expectations = successfulExpectations + failedExpectations

    @AnsiRequiring @TestFactory
    fun `should format as return value`() = testEach(
        null to Symbols.Null,
        Unit to "✔︎",
        "string" to "✔︎",
        succeededState to "✔︎",
        failedReturnValue to "ϟ return value",
        RuntimeException("exception") to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        kotlin.runCatching { failedReturnValue } to "ϟ return value",
        kotlin.runCatching { throw exception } to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
    ) { (subject, expected) ->
        expecting(subject.toSimpleString()) { ReturnValue.format(subject) } that { matchesCurlyPattern(expected) }
    }

    @Nested
    inner class SuccessReturnValue {

        @Test
        fun TestSpanScope.`should format null as nul`() {
            expectThat(format(null)).ansiRemoved.endsWith(" ␀")
        }

        @Test
        fun TestSpanScope.`should format Unit as success`() {
            expectThat(format(Unit)).ansiRemoved.endsWith("✔︎")
        }

        @Test
        fun TestSpanScope.`should format string as success`() {
            expectThat(format("string")).ansiRemoved.endsWith("✔︎")
        }

        @Test
        fun TestSpanScope.`should format succeededState as success`() {
            expectThat(format(succeededState)).ansiRemoved.endsWith("✔︎")
        }
    }

    @Nested
    inner class FailedReturnValue {

        @Test
        fun TestSpanScope.`should format failed return value as failed`() {
            expectThat(format(failedReturnValue)).ansiRemoved.endsWith("ϟ return value")
        }

        @Test
        fun TestSpanScope.`should format exception as failed`() {
            expectThat(format(RuntimeException("exception")))
                .matchesCurlyPattern("{} ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})")
        }

        @Test
        fun TestSpanScope.`should format result with failed return value as failed`() {
            expectThat(format(kotlin.runCatching { failedReturnValue })).ansiRemoved.endsWith("ϟ return value")
        }

        @Test
        fun TestSpanScope.`should format failed result as failed`() {
            expectThat(format(runCatching { throw exception }))
                .matchesCurlyPattern("{} ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})")
        }

        @Test
        fun TestSpanScope.`should format failed state as failed`() {
            expectThat(format(failedState)).ansiRemoved.endsWith("ϟ Process 12345 terminated with exit code 42")
        }
    }

    @Nested
    inner class WithReturnValues {

        @Nested
        inner class EmptyReturnValues {
            private val emptyReturnValues = ReturnValues<Any?>()

            @Test
            fun `should be successful`() {
                expectThat(emptyReturnValues.successful).isTrue()
            }
        }

        @Nested
        inner class SuccessfulReturnValues {
            private val successfulReturnValues = ReturnValues(*successfulExpectations.map { (subject, _) -> subject }.toTypedArray())

            @Test
            fun `should be successful`() {
                expectThat(successfulReturnValues.successful).isTrue()
            }
        }

        @Nested
        inner class SingleFailedReturnValue {
            private val singleUnsuccessfulReturnValues = ReturnValues(
                failedExpectations.first().first,
                *successfulExpectations.map { (subject, _) -> subject }.toTypedArray())

            @Test
            fun `should be unsuccessful`() {
                expectThat(singleUnsuccessfulReturnValues.successful).isFalse()
            }

            @Test
            fun `should render only unsuccessful`() {
                val expected = failedExpectations.first().second
                expectThat(singleUnsuccessfulReturnValues.format()).matchesCurlyPattern(expected)
            }
        }

        @Nested
        inner class MultipleFailedReturnValues {
            private val partlyUnsuccessfulReturnValues = ReturnValues(*expectations.map { (subject, _) -> subject }.toTypedArray())

            @Test
            fun `should be unsuccessful`() {
                expectThat(partlyUnsuccessfulReturnValues.successful).isFalse()
            }

            @Test
            fun `should render only unsuccessful`() {
                expectThat(partlyUnsuccessfulReturnValues.format()).matchesCurlyPattern("""
                      ϟ Multiple problems encountered: 
                          ϟ return value
                          ϟ RuntimeException: exception at.({})
                          ϟ return value
                          ϟ RuntimeException: exception at.({})
                          ϟ Process 12345 terminated with exit code 42
                  """.trimIndent())
            }
        }
    }

    @AnsiRequiring @Nested
    inner class OfSuccessful {

        private val tick = "\u001B[32m✔︎\u001B[39m"

        @Test
        fun `should format as text representation + tick`() {
            expectThat(ReturnValue.successful("value") { this }.format()).isEqualTo("$tick value")
        }

        @Test
        fun `should format tick on null text representation`() {
            val successful = ReturnValue.successful("value") { null }
            expectThat(successful.format()).isEqualTo(tick)
        }

        @Test
        fun `should format tick on blank text representation`() {
            expectThat(ReturnValue.successful("value") { "   " }.format()).isEqualTo(tick)
        }

        @Test
        fun `should format tick on missing transform`() {
            expectThat(ReturnValue.successful("value").format()).isEqualTo(tick)
        }
    }
}

private fun TestSpanScope.format(returnValue: Any?): String =
    capturing { printer ->
        @Suppress("UNUSED_VARIABLE")
        val result = runSpanning(returnValue.toString(), printer = printer) {
            @Suppress("UNUSED_EXPRESSION")
            returnValue
        }
    }
