package koodies.logging

import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.SimpleRenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.test.output.Columns
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class SmartRenderingLoggerKtTest {

    @Test
    fun InMemoryLogger.`should log using compact logger if only result logged`() {
        logging("name") { }
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   name ✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log using block logger if not only result logged`() {
        logging("name") { logText { "text" } }
        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   ╭──╴name
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
            logging("name") {
                logLine { "line" }
                logStatus { "text" }
            }

            expectThatLogged().matchesCurlyPattern("""
                ╭──╴{}
                │
                │   ╭──╴name
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
            logging("name") {
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
                │   ╭──╴name
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
            logging("name") {
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


    private fun borderTest(borderPattern: String, noBorderPattern: String, block: SimpleRenderingLogger.() -> Any) = testEach(
        SOLID to borderPattern,
        DOTTED to noBorderPattern,
        containerNamePattern = "border={}") { (border, expectation) ->
        val label = border.name
        val logger = InMemoryLogger(name = "InMemoryLogger", border = SOLID).withUnclosedWarningDisabled
            .apply { logging(name = "$label name", border = border) { block() } }
        expecting { logger.toString(fallbackReturnValue = null) } that { toStringMatchesCurlyPattern(expectation) }
    }

    @TestFactory
    fun `should log nameX`() = borderTest(
        """
            ╭──╴InMemoryLogger
            │
            │   SOLID name ✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │
            │   DOTTED name ✔︎
        """.trimIndent()) { }

    @TestFactory
    fun `should log textX`() = borderTest(
        """
            ╭──╴InMemoryLogger
            │
            │   ╭──╴SOLID name
            │   │
            │   │   text
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │
            │   ▶ DOTTED name
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
            │   ╭──╴SOLID name
            │   │
            │   │   line
            │   │
            │   ╰──╴✔︎
        """.trimIndent(), """
            ╭──╴InMemoryLogger
            │
            │   ▶ DOTTED name
            │   · line
            │   ✔︎
        """.trimIndent()) {
        logLine { "line" }
    }

    @Nested
    inner class ReturnException {

        private fun InMemoryLogger.testLog(border: Border, init: SimpleRenderingLogger.() -> Any) {
            logging(name = border.name, border = border) {
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

        private fun InMemoryLogger.testLog(border: Border, init: SimpleRenderingLogger.() -> Any) {
            kotlin.runCatching {
                logging(name = border.name, border = border) {
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
}
