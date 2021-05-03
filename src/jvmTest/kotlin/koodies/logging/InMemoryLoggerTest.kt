package koodies.logging

import koodies.debug.CapturedOutput
import koodies.io.ByteArrayOutputStream
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger.Companion.NO_RETURN_VALUE
import koodies.logging.InMemoryLogger.Companion.SUCCESSFUL_RETURN_VALUE
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.SystemIOExclusive
import koodies.test.output.TestLogger
import koodies.test.toStringContainsAll
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.containsEscapeSequences
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.startsWith


class InMemoryLoggerTest {

    private fun logger(outputStream: ByteArrayOutputStream? = null, init: InMemoryLogger.() -> Unit = {}): InMemoryLogger =
        InMemoryLogger("caption", SOLID, outputStream = outputStream).withUnclosedWarningDisabled.apply(init)

    @SystemIOExclusive
    @Test
    fun `should log using OutputStream`(capturedOutput: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        logger(outputStream) { logLine { "abc" } }

        expectThat(capturedOutput).isEmpty()
        expectThat(outputStream).toStringContainsAll("caption", "abc")
    }

    @Test
    fun `should provide access to logs`() {
        val logger = logger { logLine { "abc" } }

        logger.expectThatLogged()
            .contains("caption")
            .contains("abc")
    }

    @Test
    fun `should use BlockRenderingLogger to log`() {
        val logger = logger { logLine { "line" } }

        logger.expectThatLogged().startsWith("╭──╴caption")
    }

    @Test
    fun `should not log border if specified`() {
        val logger = InMemoryLogger("test", DOTTED).withUnclosedWarningDisabled.apply { runLogging { logLine { "line" } } }
        logger.expectThatLogged().matchesCurlyPattern("""
            ▶ test
            · line
            ✔︎
        """.trimIndent())
    }

    @Test
    fun `should be clearable`() {
        val logger = InMemoryLogger("test", DOTTED).withUnclosedWarningDisabled.apply { runLogging { logLine { "line" } } }
        logger.clear()
        logger.logLine { "single logged line" }
        logger.expectThatLogged(closeIfOpen = false).isEqualTo("· single logged line")
    }

    @Nested
    inner class ToString {

        @Test
        fun `should not contain escape sequences by default`() {
            val logger = logger { logLine { "line" } }

            expectThat(logger.toString())
                .isNotEmpty()
                .not { containsEscapeSequences() }
        }

        @Test
        fun `should keep escape sequences if specified`() {
            val logger = logger { logLine { "line" } }

            expectThat(logger.toString(keepEscapeSequences = true))
                .isNotEmpty()
                .containsEscapeSequences()
        }

        @Nested
        inner class Initial {

            @Test
            fun `should be empty`() {
                val logger = logger()

                expectThat(logger.toString()).isEmpty()
            }

            @Test
            fun `should ignore fallback return value`() {
                val logger = logger()

                expectThat(logger.toString(fallbackReturnValue = SUCCESSFUL_RETURN_VALUE)).isEmpty()
            }
        }

        @Nested
        inner class Open {

            @Test
            fun `should render open`() {
                val logger = logger { logLine { "line" } }

                expectThat(logger.toString()).isEqualTo("""
                    ╭──╴caption
                    │
                    │   line
                    ╵
                    ╵
                    ⏳️
                """.trimIndent())
            }

            @Test
            fun `should use fallback return value if specified`() {
                val logger = logger { logLine { "line" } }

                expectThat(logger.toString(fallbackReturnValue = SUCCESSFUL_RETURN_VALUE)).isEqualTo("""
                    ╭──╴caption
                    │
                    │   line
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class Closed {

            @Test
            fun `should render closed`() {
                val logger = logger { logResult() }

                expectThat(logger.toString()).isEqualTo("""
                    ╭──╴caption
                    │
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should ignore fallback return value if specified`() {
                val logger = logger { logResult() }

                expectThat(logger.toString(fallbackReturnValue = NO_RETURN_VALUE)).isEqualTo("""
                    ╭──╴caption
                    │
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class ExpectThatLoggedFn {

        @Test
        fun `should not contain escape sequences`() {
            val logger = logger()

            logger.expectThatLogged().not { containsEscapeSequences() }
        }

        @Nested
        inner class Initial {

            @Test
            fun `should assert as if nothing logged`() {
                val openLogger = logger()

                openLogger.expectThatLogged().isEmpty()
            }

            @Test
            fun `should assert unchanged open if specified`() {
                val openLogger = logger()

                openLogger.expectThatLogged(closeIfOpen = false).isEmpty()
            }
        }

        @Nested
        inner class Open {

            @Test
            fun `should assert as if closed by default`() {
                val openLogger = logger { logLine { "line" } }

                openLogger.expectThatLogged().isEqualTo("""
                    ╭──╴caption
                    │
                    │   line
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should assert unchanged open if specified`() {
                val openLogger = logger { logLine { "line" } }

                openLogger.expectThatLogged(closeIfOpen = false).isEqualTo("""
                    ╭──╴caption
                    │
                    │   line
                """.trimIndent())
            }
        }

        @Nested
        inner class Closed {

            @Test
            fun `should assert unchanged closed by default`() {
                val closedLogger = logger { logResult() }

                closedLogger.expectThatLogged().isEqualTo("""
                    ╭──╴caption
                    │
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should assert unchanged closed if specified`() {
                val closedLogger = logger { logResult() }

                closedLogger.expectThatLogged(closeIfOpen = false).isEqualTo("""
                    ╭──╴caption
                    │
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }


    @Nested
    inner class TestLoggerAssertion {

        @Nested
        inner class WithExpectLogged {


            @Test
            fun TestLogger.`should be initially empty`() {
                expectLogged.isEmpty()
            }

            @Test
            fun TestLogger.`should not contain escape sequences`() {
                logLine { "line" }
                expectLogged.not { containsEscapeSequences() }
            }

            @Test
            fun TestLogger.`should not contain header or footer`() {
                logLine { "line" }
                expectLogged.isEqualTo("line")
            }

            @Test
            fun TestLogger.`should assert exactly what was logged`() {
                blockLogging("test") { logLine { "line" }; 42 }
                expectLogged.isEqualTo("""
                    test
                    line
                    ✔︎
                """.trimIndent())
            }
        }
    }
}

val <T : InMemoryLogger> T.expectLogged
    get() = expectThat(toString(fallbackReturnValue = null,
        keepEscapeSequences = false,
        lineSkip = 1).withoutTrailingLineSeparator)

fun <T : InMemoryLogger> T.expectThatLogged(closeIfOpen: Boolean = true) =
    expectThat(toString(SUCCESSFUL_RETURN_VALUE.takeIf { closeIfOpen }))

fun <T : InMemoryLogger> T.expectThatLogged(closeIfOpen: Boolean = true, block: Builder<String>.() -> Unit) =
    expectThat(toString(SUCCESSFUL_RETURN_VALUE.takeIf { closeIfOpen }), block)
