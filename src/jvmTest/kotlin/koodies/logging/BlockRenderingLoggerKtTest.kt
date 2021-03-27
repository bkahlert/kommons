package koodies.logging

import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.output.Bordered
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT

@Execution(CONCURRENT)
class BlockRenderingLoggerKtTest {

    private fun borderedTest(borderedPattern: String, nonBorderedPattern: String, block: RenderingLogger.() -> Any) = listOf(
        true to borderedPattern,
        false to nonBorderedPattern,
    ).testEach("bordered={}") { (bordered, expectation) ->
        val label = if (bordered) "bordered" else "not-bordered"
        val logger = InMemoryLogger(caption = "$label caption", bordered = bordered).withUnclosedWarningDisabled.apply { block() }
        test { logger.expectThatLogged().toStringMatchesCurlyPattern(expectation) }
    }

    @TestFactory
    fun `should log caption`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │{}
            ╰──╴✔︎{}
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
        """.trimIndent()) { }

    @TestFactory
    fun `should log text`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   text
            │{}
            ╰──╴✔︎
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
            ╭──╴bordered caption
            │{}
            │   line
            │{}
            ╰──╴✔︎
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

            expectThatLogged().matchesCurlyPattern(
                """
                    ╭──╴{}
                    │{}
                    │   java.lang.RuntimeException: exception
                    │   	at koodies.logging.{}
                    {{}}
                """.trimIndent()
            )
        }

        @Test
        fun @receiver:Bordered(false) InMemoryLogger.`should log exception not bordered`() {
            logException { RuntimeException("exception") }

            expectThatLogged().matchesCurlyPattern(
                """
                    ▶ {}
                    · java.lang.RuntimeException: exception
                    · 	at koodies.logging.{}
                    {{}}
                """.trimIndent()
            )
        }
    }

    @TestFactory
    fun `should log status`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   line                                                                  ◀◀ status
            │{}
            ╰──╴✔︎
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
            ╭──╴bordered caption
            │{}
            │{}
            ╰──╴✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
        """.trimIndent()) {
        logResult { Result.success("result") }
    }

    @TestFactory
    fun `should log incomplete result`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            ╵
            ╵
            ⌛️ async computation
        """.trimIndent(), """
            ▶ not-bordered caption
            ⌛️ async computation
        """.trimIndent()) {
        logResult {
            Result.success(InMemoryLogger.NO_RETURN_VALUE)
        }
    }

    @TestFactory
    fun `should log multiple results`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │{}
            ╰──╴✔︎
            bordered caption ⌛️ ✔︎
            bordered caption ⌛️ ✔︎
        """.trimIndent(), """
            ▶ not-bordered caption
            ✔︎
            not-bordered caption ⌛️ ✔︎
            not-bordered caption ⌛️ ✔︎
        """.trimIndent()) {
        logResult()
        logResult()
        logResult()
    }

    @TestFactory
    fun `should log multiple entries`() = borderedTest(
        """
            ╭──╴bordered caption
            │{}
            │   text
            │   line
            │   line                                                                  ◀◀ status
            │{}
            ╰──╴✔︎
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

    @Nested
    inner class LoggingAfterResult {

        @TestFactory
        fun InMemoryLoggerFactory.`should log after logged result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: BlockRenderingLogger? = null
            logger.blockLogging("test") {
                delegate = this
                logLine { "line" }
            }
            delegate?.op()
            expect { logger }.that {
                toStringMatchesCurlyPattern(
                    """
                        ╭──╴{}
                        │   
                        │   ╭──╴test
                        │   │   
                        │   │   line
                        │   │
                        │   ╰──╴✔︎
                        │   test ⌛️ {}
                        {{}}
                    """.trimIndent()
                )
            }
        }

        @TestFactory
        fun InMemoryLoggerFactory.`should log after logged message and result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: BlockRenderingLogger? = null
            logger.blockLogging("test") {
                delegate = this
            }
            delegate?.op()
            expect { logger }.that {
                toStringMatchesCurlyPattern(
                    """
                        ╭──╴{}
                        │   
                        │   ╭──╴test
                        │   │   
                        │   │
                        │   ╰──╴✔︎
                        │   test ⌛️ {}
                        {{}}
                    """.trimIndent()
                )
            }
        }
    }
}
