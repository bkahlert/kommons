package koodies.logging

import koodies.debug.CapturedOutput
import koodies.io.ByteArrayOutputStream
import koodies.logging.InMemoryLogger.Companion.NO_RETURN_VALUE
import koodies.logging.InMemoryLogger.Companion.SUCCESSFUL_RETURN_VALUE
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.SystemIoExclusive
import koodies.test.toStringContainsAll
import koodies.text.containsEscapeSequences
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class InMemoryLoggerTest {

    private fun logger(vararg outputStreams: ByteArrayOutputStream, init: InMemoryLogger.() -> Unit = {}): InMemoryLogger =
        InMemoryLogger("caption", true, outputStreams = outputStreams).withUnclosedWarningDisabled.apply(init)

    @SystemIoExclusive
    @Test
    fun `should log using OutputStream`(capturedOutput: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        logger(outputStream).apply { logLine { "abc" } }

        expectThat(capturedOutput).isEmpty()
        expectThat(outputStream).toStringContainsAll("caption", "abc")
    }

    @Test
    fun `should provide access to logs`() {
        val logger = logger()

        logger.logLine { "abc" }

        logger.expectThatLogged()
            .contains("caption")
            .contains("abc")
    }

    @Test
    fun `should use BlockRenderingLogger to log`() {
        val logger = logger()

        logger.expectThatLogged().startsWith("╭──╴caption")
    }

    @Nested
    inner class ToString {

        @Test
        fun `should not contain escape sequences by default`() {
            val logger = logger()

            expectThat(logger.toString()).not { containsEscapeSequences() }
        }

        @Test
        fun `should keep escape sequences if specified`() {
            val logger = logger()

            expectThat(logger.toString(keepEscapeSequences = true)).containsEscapeSequences()
        }

        @Nested
        inner class Open {

            @Test
            fun `should render open`() {
                val openLogger = logger()

                expectThat(openLogger.toString()).isEqualTo("""
                    ╭──╴caption
                    │   
                    ╵
                    ╵
                    ⌛️ async computation
                """.trimIndent())
            }

            @Test
            fun `should use fallback return value if specified`() {
                val openLogger = logger()

                expectThat(openLogger.toString(fallbackReturnValue = SUCCESSFUL_RETURN_VALUE)).isEqualTo("""
                    ╭──╴caption
                    │   
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }

        @Nested
        inner class Closed {

            @Test
            fun `should render closed`() {
                val closedLogger = logger { logResult() }

                expectThat(closedLogger.toString()).isEqualTo("""
                    ╭──╴caption
                    │   
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should ignore fallback return value if specified`() {
                val closedLogger = logger { logResult() }

                expectThat(closedLogger.toString(fallbackReturnValue = NO_RETURN_VALUE)).isEqualTo("""
                    ╭──╴caption
                    │   
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class ExpectThatLogged {

        @Test
        fun `should not contain escape sequences`() {
            val logger = logger()

            logger.expectThatLogged().not { containsEscapeSequences() }
        }

        @Nested
        inner class Open {

            @Test
            fun `should assert as if closed by default`() {
                val openLogger = logger()

                openLogger.expectThatLogged().isEqualTo("""
                    ╭──╴caption
                    │   
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun `should assert unchanged open if specified`() {
                val openLogger = logger()

                openLogger.expectThatLogged(closeIfOpen = false).isEqualTo("""
                    ╭──╴caption
                    │   
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
}

fun <T : InMemoryLogger> T.expectThatLogged(closeIfOpen: Boolean = true) =
    expectThat(toString(SUCCESSFUL_RETURN_VALUE.takeIf { closeIfOpen }))

fun <T : InMemoryLogger> T.expectThatLogged(closeIfOpen: Boolean = true, block: Builder<String>.() -> Unit) =
    expectThat(toString(SUCCESSFUL_RETURN_VALUE.takeIf { closeIfOpen }), block)
