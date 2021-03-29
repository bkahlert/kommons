package koodies.logging

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
    )

    private val failedExpectations = listOf(
        failedReturnValue to "return value",
        RuntimeException("exception") to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        kotlin.runCatching { failedReturnValue } to "return value",
        kotlin.runCatching { throw exception } to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
    )

    private val expectations = successfulExpectations + failedExpectations

    @TestFactory
    fun `should convert to ReturnValue`() =
        expectations.testEach { (subject, expected) ->
            expect {
                with(MutedRenderingLogger()) {
                    subject.toReturnValue().format()
                }
            }.that { matchesCurlyPattern(expected) }
        }

    @TestFactory
    fun `should render success ReturnValue`(loggerFactory: InMemoryLoggerFactory) =
        successfulExpectations.testEach { (subject, expected) ->
            expect {
                loggerFactory.render(true, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ╭──╴{}
            │   
            │
            ╰──╴✔︎
        """.trimIndent())
            }
            expect {
                loggerFactory.render(false, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ▶ {}
            ✔︎
        """.trimIndent())
            }
        }

    @TestFactory
    fun `should render failed ReturnValue`(loggerFactory: InMemoryLoggerFactory) =
        failedExpectations.testEach { (subject, expected) ->
            expect {
                loggerFactory.render(true, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ╭──╴{}
            │   
            ϟ
            ╰──╴$expected
        """.trimIndent())
            }
            expect {
                loggerFactory.render(false, "$subject ➜ $expected") { subject }
            }.that {
                matchesCurlyPattern("""
            ▶ {}
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

private fun InMemoryLoggerFactory.render(bordered: Boolean, captionSuffix: String, block: RenderingLogger.() -> Any?): String {
    val logger = createLogger(captionSuffix, bordered = bordered)
    logger.runLogging(block)
    return logger.toString()
}
