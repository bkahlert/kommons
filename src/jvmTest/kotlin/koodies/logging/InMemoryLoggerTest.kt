package koodies.logging

import koodies.debug.CapturedOutput
import koodies.io.ByteArrayOutputStream
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.SystemIoExclusive
import koodies.test.toStringContainsAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class InMemoryLoggerTest {

    @SystemIoExclusive
    @Test
    fun `should log using OutputStream`(capturedOutput: CapturedOutput) {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger("caption", true, -1, listOf(outputStream))
        logger.logLine { "abc" }

        expectThat(capturedOutput).isEmpty()
        expectThat(outputStream).toStringContainsAll("caption", "abc")
    }

    @Test
    fun `should provide access to logs`() {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger("caption", true, -1, listOf(outputStream))
        logger.logLine { "abc" }

        expectThat(logger.logged.removeEscapeSequences())
            .contains("caption")
            .contains("abc")
    }

    @Test
    fun `should use BlockRenderingLogger to logs`() {
        val outputStream = ByteArrayOutputStream()

        val logger = InMemoryLogger("caption", true, -1, listOf(outputStream))

        expectThat(logger.logged.removeEscapeSequences()).startsWith("╭──╴caption")
    }
}

fun <T : InMemoryLogger> T.expectThatLogged() =
    expectThat(logged)
