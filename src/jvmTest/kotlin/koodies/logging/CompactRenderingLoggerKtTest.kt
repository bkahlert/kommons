package koodies.logging

import koodies.concurrent.process.IO
import koodies.test.output.InMemoryLoggerFactory
import koodies.test.testEach
import koodies.text.matchesCurlyPattern
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class CompactRenderingLoggerKtTest {

    @Test
    fun InMemoryLogger.`should log caption`() {
        compactLogging("caption") { }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log text`() {
        compactLogging("caption") {
            logText { "text" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption text ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log line`() {
        compactLogging("caption") {
            logLine { "line" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption line ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log exception`() {
        compactLogging("caption") {
            logException { RuntimeException("exception") }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption ϟ RuntimeException: exception at.(CompactRenderingLoggerKtTest.kt:{}) ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log status`() {
        compactLogging("caption") {
            logStatus("status") { "line" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption line (◀◀ status) ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log result`() {
        compactLogging("caption") {
            "result"
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log multiple results`() {
        compactLogging("caption") {
            logResult { Result.success(1) }
            logResult { Result.success(2) }
            3
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption ✔︎
                │   ⌛️ ✔︎
                │   ⌛️ ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log multiple entries`() {
        compactLogging("caption") {
            logText { "text" }
            logLine { "line" }
            logException { RuntimeException("exception") }
            logStatus("status") { "line" }
            "result"
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │{}
                │   caption text line ϟ RuntimeException: exception at.(CompactRenderingLoggerKtTest.kt:{}) line (◀◀ status) ✔︎
                │{}
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log nested compact`() {
        blockLogging("block") {
            logLine { "something" }
            compactLogging("single") {
                compactLogging {
                    logStatus { IO.OUT typed "ABC" }
                    logLine { "" }
                    logLine { "123" }
                    "abc"
                }
                logLine { "456" }
                compactLogging {
                    logStatus { IO.OUT typed "XYZ" }
                    logLine { "" }
                    logLine { "789" }
                }
            }
            logLine { "something" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
            ╭──╴{}
            │   
            │   ╭──╴block
            │   │   
            │   │   something
            │   │   single (ABC ˃ 123 ˃ ✔︎) 456 (XYZ ˃ 789 ˃ ✔︎) ✔︎
            │   │   something
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Nested
    inner class LoggingAfterResult {

        @TestFactory
        fun InMemoryLoggerFactory.`should log after logged result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: CompactRenderingLogger? = null
            logger.compactLogging("test") {
                delegate = this
                logLine { "line" }
            }
            delegate?.op()
            expecting { logger } that {
                toStringMatchesCurlyPattern(
                    """
                     ╭──╴{}
                     │   
                     │   test line ✔︎
                     │   ⌛️ {}
                     │
                     ╰──╴✔︎
                    """.trimIndent())
            }
        }

        @TestFactory
        fun InMemoryLoggerFactory.`should log after logged message and result`() = InMemoryLogger.LOG_OPERATIONS.testEach { (opName, op) ->
            val logger = createLogger(opName)
            var delegate: CompactRenderingLogger? = null
            logger.compactLogging("test") {
                delegate = this
            }
            delegate?.op()
            expecting { logger } that {
                toStringMatchesCurlyPattern(
                    """
                     ╭──╴{}
                     │   
                     │   test ✔︎
                     │   ⌛️ {}
                     │
                     ╰──╴✔︎
                    """.trimIndent())
            }
        }
    }
}
