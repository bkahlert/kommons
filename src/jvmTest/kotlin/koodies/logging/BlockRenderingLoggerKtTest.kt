package koodies.logging

import koodies.test.output.Bordered
import koodies.test.testEach
import koodies.text.LineSeparators.LF
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class BlockRenderingLoggerKtTest {

    private fun borderedTest(borderedPattern: String, nonBorderedPattern: String, block: RenderingLogger.() -> Any) = listOf(
        true to borderedPattern,
        false to nonBorderedPattern,
    ).testEach("bordered={}") { (bordered, expectation) ->
        val label = if (bordered) "bordered" else "not-bordered"
        val logger = InMemoryLogger(caption = "$label caption", bordered = bordered, outputStreams = emptyList())
        with(logger) {
            block()
            logResult()
            expect { logged }.that { matchesCurlyPattern(expectation) }
        }
    }

    @TestFactory
    fun `should log caption`() = borderedTest(
        """
            ╭─────╴bordered caption
            │{}
            │{}
            ╰─────╴✔︎{}
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
        """.trimIndent()) { }

    @TestFactory
    fun `should log text`() = borderedTest(
        """
            ╭─────╴bordered caption
            │{}
            │   text
            │{}
            ╰─────╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · text
            ✔︎
        """.trimIndent()) {
        logText { "text" }
    }

    @TestFactory
    fun `should log line`() = borderedTest(
        """
            ╭─────╴bordered caption
            │{}
            │   line
            │{}
            ╰─────╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · line
            ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class LogException {

        @Test
        fun @receiver:Bordered(true) InMemoryLogger.`should log exception bordered`() {
            logException { RuntimeException("exception") }

            expectThat(logged.lines().take(4).joinToString(LF)).matchesCurlyPattern(
                """
                    ╭─────╴{}
                    │{}
                    │   java.lang.RuntimeException: exception
                    │   	at koodies.logging.{}
                """.trimIndent()
            )
        }

        @Test
        fun @receiver:Bordered(false) InMemoryLogger.`should log exception not bordered`() {
            logException { RuntimeException("exception") }

            expectThat(logged.lines().take(3).joinToString(LF)).matchesCurlyPattern(
                """
                    ▶ {}
                    · java.lang.RuntimeException: exception
                    · 	at koodies.logging.{}
                """.trimIndent()
            )
        }
    }

    @TestFactory
    fun `should log status`() = borderedTest(
        """
            ╭─────╴bordered caption
            │{}
            │   line                                                                  ◀◀ status
            │{}
            ╰─────╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · line                                                                  ◀◀ status
            ✔︎
        """.trimIndent()) {
        logStatus("status") { "line" }
    }

    @TestFactory
    fun `should log result`() = borderedTest(
        """
            ╭─────╴bordered caption
            │{}
            │{}
            ╰─────╴✔︎
            │
            ╰─────╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
            ✔︎
        """.trimIndent()) {
        logResult { Result.success("result") }
    }

    @TestFactory
    fun `should log multiple results`() = borderedTest(
        """
            ╭─────╴bordered caption
            │{}
            │{}
            ╰─────╴✔︎
            │
            ╰─────╴✔︎
            │
            ╰─────╴✔︎
            │
            ╰─────╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
            ✔︎
            ✔︎
            ✔︎
        """.trimIndent()) {
        logResult { Result.success(1) }
        logResult { Result.success(2) }
        logResult { Result.success(3) }
    }

    @TestFactory
    fun `should log multiple entries`() = borderedTest(
        """
            ╭─────╴bordered caption
            │{}
            │   text
            │   line
            │   line                                                                  ◀◀ status
            │{}
            ╰─────╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            · text
            · line
            · line                                                                  ◀◀ status
            ✔︎
        """.trimIndent()) {
        logText { "text" }
        logLine { "line" }
        logStatus("status") { "line" }
    }
}
