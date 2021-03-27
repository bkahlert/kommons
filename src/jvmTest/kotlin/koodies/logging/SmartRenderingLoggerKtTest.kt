package koodies.logging

import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD

@Execution(SAME_THREAD)
class SmartRenderingLoggerKtTest {

    private fun borderedTest(borderedPattern: String, nonBorderedPattern: String, block: RenderingLogger.() -> Any) = listOf(
        true to borderedPattern,
        false to nonBorderedPattern,
    ).testEach("bordered={}") { (bordered, expectation) ->
        val label = if (bordered) "bordered" else "not-bordered"
        val logger = InMemoryLogger(caption = "InMemoryLogger", bordered = true).withUnclosedWarningDisabled
            .apply { logging(caption = "$label caption", bordered = bordered) { block() } }
        expect { logger.toString(fallbackReturnValue = null) }.that { toStringMatchesCurlyPattern(expectation) }
    }

    @TestFactory
    fun `should log caption`() = borderedTest(
        """
            ╭──╴InMemoryLogger
            │   
            │   bordered caption ✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │   
            │   not-bordered caption ✔︎
        """.trimIndent()) { }

    @TestFactory
    fun `should log text`() = borderedTest(
        """
            ╭──╴InMemoryLogger
            │   
            │   ╭──╴bordered caption
            │   │   
            │   │   text
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · text
            │   ✔︎
        """.trimIndent()) {
        logText { "text" }
    }

    @TestFactory
    fun `should log line`() = borderedTest(
        """
            ╭──╴InMemoryLogger
            │   
            │   ╭──╴bordered caption
            │   │   
            │   │   line
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · line
            │   ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class ReturnException {

        private fun InMemoryLogger.testLog(bordered: Boolean, init: RenderingLogger.() -> Any) {
            logging(caption = if (bordered) "bordered" else "not-bordered", bordered = bordered) {
                init()
            }
        }

        @Nested
        inner class AsFirstLog {

            @Test
            fun InMemoryLogger.`should log exception bordered`() {
                testLog(true) { RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   bordered ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception not bordered`() {
                testLog(false) { RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   not-bordered ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }

        @Nested
        inner class AsSecondLog {

            @Test
            fun InMemoryLogger.`should log exception bordered`() {
                testLog(true) { logLine { "line" };RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴bordered
                    │   │   
                    │   │   line
                    │   ϟ
                    │   ╰──╴RuntimeException: exception{}
                    {{}}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception not bordered`() {
                testLog(false) { logLine { "line" };RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ▶ not-bordered
                    │   · line
                    │   ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class ThrowingException {

        private fun InMemoryLogger.testLog(bordered: Boolean, init: RenderingLogger.() -> Any) {
            kotlin.runCatching {
                logging(caption = if (bordered) "bordered" else "not-bordered", bordered = bordered) {
                    init()
                }
            }
        }

        @Nested
        inner class AsFirstLog {

            @Test
            fun InMemoryLogger.`should log exception bordered`() {
                testLog(true) { throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   bordered ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception not bordered`() {
                testLog(false) { throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   not-bordered ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }

        @Nested
        inner class AsSecondLog {

            @Test
            fun InMemoryLogger.`should log exception bordered`() {
                testLog(true) { logLine { "line" };throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴bordered
                    │   │   
                    │   │   line
                    │   ϟ
                    │   ╰──╴RuntimeException: exception{}
                    {{}}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception not bordered`() {
                testLog(false) { logLine { "line" };throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ▶ not-bordered
                    │   · line
                    │   ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }
    }

    @TestFactory
    fun `should log status`() = borderedTest(
        """
            ╭──╴InMemoryLogger
            │   
            │   ╭──╴bordered caption
            │   │   
            │   │   line                                                              ◀◀ status
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · line                                                              ◀◀ status
            │   ✔︎
        """.trimIndent()) {
        logStatus("status") { "line" }
    }

    @TestFactory
    fun `should log result`() = borderedTest(
        """
            ╭──╴InMemoryLogger
            │   
            │   bordered caption ✔︎
            │   bordered caption ⌛️ ✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │   
            │   not-bordered caption ✔︎
            │   not-bordered caption ⌛️ ✔︎
        """.trimIndent()) {
        logResult { Result.success("result") }
    }

    @TestFactory
    fun `should log multiple results`() = borderedTest(
        """
            ╭──╴InMemoryLogger
            │   
            │   bordered caption ✔︎
            │   bordered caption ⌛️ ✔︎
            │   bordered caption ⌛️ ✔︎
            │   bordered caption ⌛️ ✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │   
            │   not-bordered caption ✔︎
            │   not-bordered caption ⌛️ ✔︎
            │   not-bordered caption ⌛️ ✔︎
            │   not-bordered caption ⌛️ ✔︎
        """.trimIndent()) {
        logResult { Result.success(1) }
        logResult { Result.success(2) }
        logResult { Result.success(3) }
    }

    @TestFactory
    fun `should log multiple entries`() = borderedTest(
        """
            ╭──╴InMemoryLogger
            │   
            │   ╭──╴bordered caption
            │   │   
            │   │   text
            │   │   line
            │   │   line                                                              ◀◀ status
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │   
            │   ▶ not-bordered caption
            │   · text
            │   · line
            │   · line                                                              ◀◀ status
            │   ✔︎
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
            var delegate: RenderingLogger? = null
            logger.logging("test") {
                delegate = this
                logLine { "line" }
            }
            delegate?.op()
            expect { logger }.that {
                toStringMatchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   ╭──╴test
                    │   │   
                    │   │   line
                    │   │
                    │   ╰──╴✔︎
                    │   test ⌛️ {}
                    {{}}""".trimIndent())
            }
        }

        @TestFactory
        fun InMemoryLoggerFactory.`should log after logged message and result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: RenderingLogger? = null
            logger.logging("test") {
                delegate = this
            }
            delegate?.op()
            expect { logger }.that {
                toStringMatchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   test ✔︎
                    │   test ⌛️ {}
                    {{}}""".trimIndent())
            }
        }
    }
}
