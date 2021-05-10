package koodies.logging

import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger.Companion.LOG_OPERATIONS
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.Slow
import koodies.test.output.Columns
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

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


    private fun borderTest(borderPattern: String, noBorderPattern: String, block: RenderingLogger.() -> Any) = testEach(
        SOLID to borderPattern,
        DOTTED to noBorderPattern,
        containerNamePattern = "border={}") { (border, expectation) ->
        val label = border.name
        val logger = InMemoryLogger(caption = "InMemoryLogger", border = SOLID).withUnclosedWarningDisabled
            .apply { logging(caption = "$label caption", border = border) { block() } }
        expecting { logger.toString(fallbackReturnValue = null) } that { toStringMatchesCurlyPattern(expectation) }
    }

    @TestFactory
    fun `should log captionX`() = borderTest(
        """
            ╭──╴InMemoryLogger
            │
            │   SOLID caption ✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │
            │   DOTTED caption ✔︎
        """.trimIndent()) { }

    @TestFactory
    fun `should log textX`() = borderTest(
        """
            ╭──╴InMemoryLogger
            │
            │   ╭──╴SOLID caption
            │   │
            │   │   text
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │
            │   ▶ DOTTED caption
            │   · text
            │   ✔︎
        """.trimIndent()) {
        logText { "text" }
    }

    @TestFactory
    fun `should log lineX`() = borderTest(
        """
            ╭──╴InMemoryLogger
            │
            │   ╭──╴SOLID caption
            │   │
            │   │   line
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │
            │   ▶ DOTTED caption
            │   · line
            │   ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class ReturnException {

        private fun InMemoryLogger.testLog(border: Border, init: RenderingLogger.() -> Any) {
            logging(caption = border.name, border = border) {
                init()
            }
        }

        @Nested
        inner class AsFirstLog {

            @Test
            fun InMemoryLogger.`should log exception SOLID`() {
                testLog(SOLID) { RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   SOLID ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception DOTTED`() {
                testLog(DOTTED) { RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   DOTTED ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception NONE`() {
                testLog(NONE) { RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   NONE ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }

        @Nested
        inner class AsSecondLog {

            @Test
            fun InMemoryLogger.`should log exception SOLID`() {
                testLog(SOLID) { logLine { "line" };RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴SOLID
                    │   │
                    │   │   line
                    │   ϟ
                    │   ╰──╴RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception DOTTED`() {
                testLog(DOTTED) { logLine { "line" };RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ▶ DOTTED
                    │   · line
                    │   ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception NONE`() {
                testLog(NONE) { logLine { "line" };RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   NONE
                    │   line
                    │   ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class ThrowingException {

        private fun InMemoryLogger.testLog(border: Border, init: RenderingLogger.() -> Any) {
            kotlin.runCatching {
                logging(caption = border.name, border = border) {
                    init()
                }
            }
        }

        @Nested
        inner class AsFirstLog {

            @Test
            fun InMemoryLogger.`should log exception SOLID`() {
                testLog(SOLID) { throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   SOLID ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception DOTTED`() {
                testLog(DOTTED) { throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   DOTTED ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception NONE`() {
                testLog(NONE) { throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   NONE ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }

        @Nested
        inner class AsSecondLog {

            @Test
            fun InMemoryLogger.`should log exception SOLID`() {
                testLog(SOLID) { logLine { "line" };throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴SOLID
                    │   │
                    │   │   line
                    │   ϟ
                    │   ╰──╴RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception DOTTED`() {
                testLog(DOTTED) { logLine { "line" };throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ▶ DOTTED
                    │   · line
                    │   ϟ RuntimeException: exception{}
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should log exception NONE`() {
                testLog(NONE) { logLine { "line" };throw RuntimeException("exception") }

                expectThatLogged(closeIfOpen = false).matchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   NONE
                    │   line
                    │   ϟ RuntimeException: exception{}
                """.trimIndent())
            }
        }
    }

    @Nested
    inner class LoggingAfterResult {

        @Slow @TestFactory
        fun InMemoryLoggerFactory.`should log after logged result`() = testEach(*LOG_OPERATIONS) { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: RenderingLogger? = null
            logger.logging("test") {
                delegate = this
                logLine { "line" }
            }
            delegate?.op()
            expecting { logger } that {
                toStringMatchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   ╭──╴test
                    │   │
                    │   │   line
                    │   │
                    │   ╰──╴✔︎
                    │   ⏳️ {}
                    {{}}""".trimIndent())
            }
        }

        @Slow @TestFactory
        fun InMemoryLoggerFactory.`should log after logged message and result`() = testEach(*LOG_OPERATIONS) { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: RenderingLogger? = null
            logger.logging("test") {
                delegate = this
            }
            delegate?.op()
            expecting { logger } that {
                toStringMatchesCurlyPattern("""
                    ╭──╴{}
                    │
                    │   test ✔︎
                    │   ⏳️ {}
                    {{}}""".trimIndent())
            }
        }
    }
}
