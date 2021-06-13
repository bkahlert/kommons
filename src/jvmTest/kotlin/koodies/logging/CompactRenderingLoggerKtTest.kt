package koodies.logging

import koodies.exec.IO
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test

class CompactRenderingLoggerKtTest {

    @Test
    fun InMemoryLogger.`should log name`() {
        compactLogging("name") { }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │
                │   name ✔︎
                │
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log text`() {
        compactLogging("name") {
            logText { "text" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │
                │   name text ✔︎
                │
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log line`() {
        compactLogging("name") {
            logLine { "line" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │
                │   name line ✔︎
                │
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log exception`() {
        kotlin.runCatching { compactLogging("name") { throw RuntimeException("exception") } }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │
                │   name ϟ RuntimeException: exception at.(CompactRenderingLoggerKtTest.kt:{})
                │
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log status`() {
        compactLogging("name") {
            logStatus("status") { "line" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │
                │   name line (◀◀ status) ✔︎
                │
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log result`() {
        compactLogging("name") {
            "result"
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │
                │   name ✔︎
                │
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log multiple entries`() {
        kotlin.runCatching {
            compactLogging("name") {
                logText { "text" }
                logLine { "line" }
                throw RuntimeException("exception")
            }
        }

        expectThatLogged().matchesCurlyPattern(
            """
                ╭──╴{}
                │
                │   name text line ϟ RuntimeException: exception at.(CompactRenderingLoggerKtTest.kt:{})
                │
                ╰──╴✔︎{}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should log nested compact`() {
        blockLogging("block") {
            logLine { "something" }
            compactLogging("single") {
                compactLogging {
                    logStatus { IO.Output typed "ABC" }
                    logLine { "" }
                    logLine { "123" }
                    "abc"
                }
                logLine { "456" }
                compactLogging {
                    logStatus { IO.Output typed "XYZ" }
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
}
