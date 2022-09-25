package com.bkahlert.kommons_deprecated.tracing.rendering

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons_deprecated.exec.IOSequence
import com.bkahlert.kommons_deprecated.exec.Process.State.Exited.Failed
import com.bkahlert.kommons_deprecated.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons_deprecated.exec.mock.ExecMock
import com.bkahlert.kommons_deprecated.test.AnsiRequiring
import com.bkahlert.kommons_deprecated.text.Semantics.Symbols
import com.bkahlert.kommons_deprecated.tracing.TestSpanScope
import com.bkahlert.kommons_deprecated.tracing.runSpanning
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
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
        ExecMock.SUCCEEDED_EXEC to "Process * terminated successfully at *",
    )

    private val failedExpectations = listOf(
        failedReturnValue to "ϟ return value",
        RuntimeException("exception") to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:*)",
        kotlin.runCatching { failedReturnValue } to "return value",
        kotlin.runCatching { throw exception } to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:*)",
        failedState to "ϟ Process 12345 terminated with exit code 42.**A dump has**terminated with exit code 42"
    )

    private val expectations = successfulExpectations + failedExpectations

    @AnsiRequiring @TestFactory
    fun `should format as return value`() = testEach(
        null to Symbols.Null.ansiRemoved,
        Unit to "✔︎",
        "string" to "✔︎",
        succeededState to "✔︎",
        failedReturnValue to "ϟ return value",
        RuntimeException("exception") to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:*)",
        kotlin.runCatching { failedReturnValue } to "ϟ return value",
        kotlin.runCatching { throw exception } to "ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:*)",
    ) { (subject, expected) ->
        ReturnValue.format(subject).ansiRemoved shouldMatchGlob expected
    }

    @Nested
    inner class SuccessReturnValue {

        @Test
        fun TestSpanScope.`should format null as nul`() {
            format(null).ansiRemoved shouldEndWith " ␀"
        }

        @Test
        fun TestSpanScope.`should format Unit as success`() {
            format(Unit).ansiRemoved shouldEndWith "✔︎"
        }

        @Test
        fun TestSpanScope.`should format string as success`() {
            format("string").ansiRemoved shouldEndWith "✔︎"
        }

        @Test
        fun TestSpanScope.`should format succeededState as success`() {
            format(succeededState).ansiRemoved shouldEndWith "✔︎"
        }
    }

    @Nested
    inner class FailedReturnValue {

        @Test
        fun TestSpanScope.`should format failed return value as failed`() {
            format(failedReturnValue).ansiRemoved shouldEndWith "ϟ return value"
        }

        @Test
        fun TestSpanScope.`should format exception as failed`() {
            format(RuntimeException("exception")).ansiRemoved
                .shouldMatchGlob("* ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:*)")
        }

        @Test
        fun TestSpanScope.`should format result with failed return value as failed`() {
            format(kotlin.runCatching { failedReturnValue }).ansiRemoved shouldEndWith "ϟ return value"
        }

        @Test
        fun TestSpanScope.`should format failed result as failed`() {
            format(runCatching { throw exception }).ansiRemoved
                .shouldMatchGlob("* ϟ RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:*)")
        }

        @Test
        fun TestSpanScope.`should format failed state as failed`() {
            format(failedState).ansiRemoved shouldEndWith "ϟ Process 12345 terminated with exit code 42"
        }
    }

    @Nested
    inner class WithReturnValues {

        @Nested
        inner class EmptyReturnValues {
            private val emptyReturnValues = ReturnValues<Any?>()

            @Test
            fun `should be successful`() {
                emptyReturnValues.successful shouldBe true
            }
        }

        @Nested
        inner class SuccessfulReturnValues {
            private val successfulReturnValues = ReturnValues(*successfulExpectations.map { (subject, _) -> subject }.toTypedArray())

            @Test
            fun `should be successful`() {
                successfulReturnValues.successful shouldBe true
            }
        }

        @Nested
        inner class SingleFailedReturnValue {
            private val singleUnsuccessfulReturnValues = ReturnValues(
                failedExpectations.first().first,
                *successfulExpectations.map { (subject, _) -> subject }.toTypedArray()
            )

            @Test
            fun `should be unsuccessful`() {
                singleUnsuccessfulReturnValues.successful shouldBe false
            }

            @Test
            fun `should render only unsuccessful`() {
                val expected = failedExpectations.first().second
                singleUnsuccessfulReturnValues.format().ansiRemoved shouldMatchGlob expected
            }
        }

        @Nested
        inner class MultipleFailedReturnValues {
            private val partlyUnsuccessfulReturnValues = ReturnValues(*expectations.map { (subject, _) -> subject }.toTypedArray())

            @Test
            fun `should be unsuccessful`() {
                partlyUnsuccessfulReturnValues.successful shouldBe false
            }

            @Test
            fun `should render only unsuccessful`() {
                partlyUnsuccessfulReturnValues.format().ansiRemoved shouldMatchGlob """
                      ϟ Multiple problems encountered:
                          ϟ return value
                          ϟ RuntimeException: exception at.(*)
                          ϟ return value
                          ϟ RuntimeException: exception at.(*)
                          ϟ Process 12345 terminated with exit code 42
                  """.trimIndent()
            }
        }
    }

    @AnsiRequiring @Nested
    inner class OfSuccessful {

        private val tick = "\u001B[32m✔︎\u001B[39m"

        @Test
        fun `should format as text representation + tick`() {
            ReturnValue.successful("value") { this }.format() shouldBe "$tick value"
        }

        @Test
        fun `should format tick on null text representation`() {
            val successful = ReturnValue.successful("value") { null }
            successful.format() shouldBe tick
        }

        @Test
        fun `should format tick on blank text representation`() {
            ReturnValue.successful("value") { "   " }.format() shouldBe tick
        }

        @Test
        fun `should format tick on missing transform`() {
            ReturnValue.successful("value").format() shouldBe tick
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
