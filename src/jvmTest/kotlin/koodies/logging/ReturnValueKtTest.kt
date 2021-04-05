package koodies.logging

import koodies.concurrent.process.ManagedProcessMock
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.LineSeparators.LF
import koodies.text.Semantics
import koodies.text.Semantics.Error
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(SAME_THREAD)
class ReturnValueKtTest {
    private val failedReturnValue: ReturnValue = object : ReturnValue {
        override val successful: Boolean get() = false
        override fun format(): String = "return value"
    }

    private val exception = RuntimeException("exception")


    private val successfulExpectations = listOf(
        null to Semantics.Null,
        "string" to "string",
        ManagedProcessMock.SUCCEEDED_MANAGED_PROCESS to "Process {} terminated successfully at {}",
    )

    private val failedExpectations = listOf(
        failedReturnValue to "return value",
        RuntimeException("exception") to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        kotlin.runCatching { failedReturnValue } to "return value",
        kotlin.runCatching { throw exception } to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        ManagedProcessMock.FAILED_MANAGED_PROCESS to "failed"
    )

    private val expectations = successfulExpectations + failedExpectations

    @TestFactory
    fun `should convert to ReturnValue`() =
        expectations.testEach { (subject, expected) ->
            expect { ReturnValue.of(subject).format() }.that { matchesCurlyPattern(expected) }
        }

    @TestFactory
    fun `should render success ReturnValue`(loggerFactory: InMemoryLoggerFactory) =
        successfulExpectations.testEach { (subject, expected) ->
            expect {
                loggerFactory.render(SOLID, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ╭──╴{}
            │   
            │
            ╰──╴✔︎
        """.trimIndent())
            }

            expect {
                loggerFactory.render(DOTTED, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ▶ {}
            ✔︎
        """.trimIndent())
            }

            expect {
                loggerFactory.render(NONE, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            {}
            ✔︎
        """.trimIndent())
            }
        }

    @TestFactory
    fun `should render failed ReturnValue`(loggerFactory: InMemoryLoggerFactory) =
        failedExpectations.testEach { (subject, expected) ->
            expect {
                loggerFactory.render(SOLID, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ╭──╴{}
            │   
            ϟ
            ╰──╴$expected
        """.trimIndent())
            }
            expect {
                loggerFactory.render(DOTTED, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ▶ {}
            ϟ $expected
        """.trimIndent())
            }

            expect {
                loggerFactory.render(NONE, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            {}
            ϟ $expected
        """.trimIndent())
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
            private val successfulReturnValues = ReturnValues<Any?>(*successfulExpectations.map { (subject, _) -> subject }.toTypedArray())

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
                val expected = "Multiple problems encountered: " + failedExpectations.joinToString("") { (_, expectation) -> "$LF    $Error $expectation" }
                expectThat(partlyUnsuccessfulReturnValues.format()).matchesCurlyPattern(expected)
            }
        }
    }
}

private fun InMemoryLoggerFactory.render(border: Border, captionSuffix: String, block: RenderingLogger.() -> Any?): String {
    val logger = createLogger(captionSuffix, border)
    logger.runLogging(block)
    return logger.toString()
}
