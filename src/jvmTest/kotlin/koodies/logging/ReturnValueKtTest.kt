package koodies.logging

import koodies.exec.IOSequence
import koodies.exec.Process.State.Exited.Failed
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.mock.ExecMock
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.Semantics.Symbols
import koodies.text.matchesCurlyPattern
import koodies.toSimpleString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
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

    @TestFactory
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

    @TestFactory
    fun `should render success ReturnValue`(loggerFactory: InMemoryLoggerFactory) = testEach(
        null to "␀",
        Unit to "✔︎",
        "string" to "✔︎",
        succeededState to "✔︎",
    ) { (subject, expected) ->

        expecting(subject.toSimpleString()) {
            loggerFactory.render(SOLID, "$subject ➜ $expected") { subject }
        } that {
            matchesCurlyPattern("""
                ╭──╴{}
                │
                │
                ╰──╴$expected
                """.trimIndent())
        }

        expecting(subject.toSimpleString()) {
            loggerFactory.render(DOTTED, "$subject ➜ $expected") { subject }
        } that {
            matchesCurlyPattern("""
                ▶ {}
                $expected
                """.trimIndent())
        }

        expecting(subject.toSimpleString()) {
            loggerFactory.render(NONE, "$subject ➜ $expected") { subject }
        } that {
            matchesCurlyPattern("""
                {}
                $expected
                """.trimIndent())
        }
    }

    @TestFactory
    fun `should render failed ReturnValue`(loggerFactory: InMemoryLoggerFactory) = testEach(
        failedReturnValue to "return value",
        RuntimeException("exception") to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        kotlin.runCatching { failedReturnValue } to "return value",
        kotlin.runCatching { throw exception } to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:{})",
        failedState to "Process 12345 terminated with exit code 42"
    ) { (subject, expected) ->

        expecting(subject.toSimpleString()) {
            loggerFactory.render(SOLID, "$subject ➜ $expected") { subject }
        } that {
            matchesCurlyPattern("""
                ╭──╴{}
                │
                ϟ
                ╰──╴$expected
                """.trimIndent())
        }

        expecting(subject.toSimpleString()) {
            loggerFactory.render(DOTTED, "$subject ➜ $expected") { subject }
        } that {
            matchesCurlyPattern("""
                ▶ {}
                ϟ $expected
                """.trimIndent())
        }

        expecting(subject.toSimpleString()) {
            loggerFactory.render(NONE, "$subject ➜ $expected") { subject }
        } that {
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
}

private fun InMemoryLoggerFactory.render(border: Border, nameSuffix: String, block: SimpleRenderingLogger.() -> Any?): String {
    val logger = createLogger(nameSuffix, border)
    logger.runLogging(block)
    return logger.toString()
}
