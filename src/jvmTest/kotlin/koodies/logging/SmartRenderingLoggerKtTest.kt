package koodies.logging

import koodies.test.testEach
import koodies.text.LineSeparators.LF
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat

@Execution(SAME_THREAD)
class SmartRenderingLoggerKtTest {

    private fun borderedTest(borderedPattern: String, nonBorderedPattern: String, block: RenderingLogger.() -> Any) = listOf(
        true to borderedPattern,
        false to nonBorderedPattern,
    ).testEach("bordered={}") { (bordered, expectation) ->
        val label = if (bordered) "bordered" else "not-bordered"
        val logger = InMemoryLogger(caption = "InMemoryLogger", bordered = true, outputStreams = emptyList())
        with(logger) {
            logging("$label caption", bordered = bordered) {
                block()
            }
            expect { logged }.that { matchesCurlyPattern(expectation) }
        }
    }

    @TestFactory
    fun `should log caption`() = borderedTest(
        """
            ╭─────╴InMemoryLogger
            │   
            │   bordered caption ✔
        """.trimIndent(), """
            ╭─────╴InMemoryLogger
            │   
            │   not-bordered caption ✔
        """.trimIndent()) { }

    @TestFactory
    fun `should log text`() = borderedTest(
        """
            ╭─────╴InMemoryLogger
            │   
            │   
            │   ╭─────╴bordered caption
            │   │   
            │   │   text
            │   │
            │   ╰─────╴✔
            │
        """.trimIndent(), """
            ╭─────╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · text
            │   ✔
        """.trimIndent()) {
        logText { "text" }
    }

    @TestFactory
    fun `should log line`() = borderedTest(
        """
            ╭─────╴InMemoryLogger
            │   
            │   
            │   ╭─────╴bordered caption
            │   │   
            │   │   line
            │   │
            │   ╰─────╴✔
            │
        """.trimIndent(), """
            ╭─────╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · line
            │   ✔
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class LogException {

        @Test
        fun InMemoryLogger.`should log exception bordered`() {
            logging("bordered", bordered = true) {
                logException { RuntimeException("exception") }
            }

            expectThat(logged.lines().take(7).joinToString(LF)).matchesCurlyPattern(
                """
                    ╭─────╴SmartRenderingLoggerKtTest ➜ LogException ➜ should log exception bordered(InMemoryLogger)
                    │   
                    │   
                    │   ╭─────╴bordered
                    │   │   
                    │   │   java.lang.RuntimeException: exception
                    │   │   	at koodies.logging.{}
                """.trimIndent()
            )
        }

        @Test
        fun InMemoryLogger.`should log exception not bordered`() {
            logging("not-bordered", bordered = false) {
                logException { RuntimeException("exception") }
            }

            expectThat(logged.lines().take(5).joinToString(LF)).matchesCurlyPattern(
                """
                    ╭─────╴SmartRenderingLoggerKtTest ➜ LogException ➜ should log exception not bordered(InMemoryLogger)
                    │   
                    │   ▶ not-bordered
                    │   · java.lang.RuntimeException: exception
                    │   · 	at koodies.logging.{}
                """.trimIndent()
            )
        }
    }

    @TestFactory
    fun `should log status`() = borderedTest(
        """
            ╭─────╴InMemoryLogger
            │   
            │   
            │   ╭─────╴bordered caption
            │   │   
            │   │   line                                                              ◀◀ status
            │   │
            │   ╰─────╴✔
            │
        """.trimIndent(), """
            ╭─────╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · line                                                              ◀◀ status
            │   ✔
        """.trimIndent()) {
        logStatus("status") { "line" }
    }

    @TestFactory
    fun `should log result`() = borderedTest(
        """
            ╭─────╴InMemoryLogger
            │   
            │   bordered caption ✔
            │   bordered caption ✔ ✔
        """.trimIndent(), """
            ╭─────╴InMemoryLogger
            │   
            │   not-bordered caption ✔
            │   not-bordered caption ✔ ✔
        """.trimIndent()) {
        logResult { Result.success("result") }
    }

    @TestFactory
    fun `should log multiple results`() = borderedTest(
        """
            ╭─────╴InMemoryLogger
            │   
            │   bordered caption ✔
            │   bordered caption ✔ ✔
            │   bordered caption ✔ ✔ ✔
            │   bordered caption ✔ ✔ ✔ ✔
        """.trimIndent(), """
            ╭─────╴InMemoryLogger
            │   
            │   not-bordered caption ✔
            │   not-bordered caption ✔ ✔
            │   not-bordered caption ✔ ✔ ✔
            │   not-bordered caption ✔ ✔ ✔ ✔
        """.trimIndent()) {
        logResult { Result.success(1) }
        logResult { Result.success(2) }
        logResult { Result.success(3) }
    }

    @TestFactory
    fun `should log multiple entries`() = borderedTest(
        """
            ╭─────╴InMemoryLogger
            │   
            │   
            │   ╭─────╴bordered caption
            │   │   
            │   │   text
            │   │   line
            │   │   line                                                              ◀◀ status
            │   │
            │   ╰─────╴✔
            │
        """.trimIndent(), """
            ╭─────╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · text
            │   · line
            │   · line                                                              ◀◀ status
            │   ✔
        """.trimIndent()) {
        logText { "text" }
        logLine { "line" }
        logStatus("status") { "line" }
    }
}
