package koodies.logging

import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.output.Columns
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

    @Test
    fun InMemoryLogger.`should log using compact logger if only result logged`() {
        logging("caption") { }
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │   
            │   caption ✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log using block logger if not only result logged`() {
        logging("caption") { logText { "text" } }
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │   
            │   ╭──╴caption
            │   │   
            │   │   text
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Nested
    inner class RenderingAsBlock {
        @Test
        fun InMemoryLogger.`should log`() {
            logging("caption") {
                logLine { "line" }
                logStatus { "text" }
            }

            expectThatLogged().matchesCurlyPattern("""
                ╭──╴{}
                │   
                │   ╭──╴caption
                │   │   
                │   │   line
                │   │   text {} ▮▮
                │   │
                │   ╰──╴✔︎
                │
                ╰──╴✔︎
            """.trimIndent())
        }

        @Test
        fun InMemoryLogger.`should log nested`() {
            logging("caption") {
                logLine { "outer 1" }
                logLine { "outer 2" }
                logging("nested") {
                    logLine { "nested 1" }
                    logLine { "nested 2" }
                    logLine { "nested 3" }
                }
                logLine { "outer 3" }
                logLine { "outer 4" }
            }

            expectThatLogged().matchesCurlyPattern("""
                ╭──╴{}
                │   
                │   ╭──╴caption
                │   │   
                │   │   outer 1
                │   │   outer 2
                │   │   ╭──╴nested
                │   │   │   
                │   │   │   nested 1
                │   │   │   nested 2
                │   │   │   nested 3
                │   │   │
                │   │   ╰──╴✔︎
                │   │   outer 3
                │   │   outer 4
                │   │
                │   ╰──╴✔︎
                │
                ╰──╴✔︎
            """.trimIndent())
        }

        @Test
        fun @receiver:Columns(60) InMemoryLogger.`should log status in same column`() {
            logging("caption") {
                logStatus("status") { "text" }
                logging("nested") {
                    logStatus("status") { "text" }
                }
            }

            expectThatLogged().matchesCurlyPattern("""
                {{}}
                │   │   text                                                              ◀◀ status
                {{}}
                │   │   │   text                                                          ◀◀ status
                {{}}
            """.trimIndent())
        }
    }


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
    fun `should log captionX`() = borderedTest(
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
    fun `should log textX`() = borderedTest(
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
    fun `should log lineX`() = borderedTest(
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
                    │   ⌛️ {}
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
                    │   ⌛️ {}
                    {{}}""".trimIndent())
            }
        }
    }
}
