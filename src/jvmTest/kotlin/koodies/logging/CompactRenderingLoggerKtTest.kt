package koodies.logging

import koodies.concurrent.process.IO
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class CompactRenderingLoggerKtTest {

    @Test
    fun InMemoryLogger.`should log caption`() {
        compactLogging("caption") { }
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should log text`() {
        compactLogging("caption") {
            logText { "text" }
        }
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption text ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should log line`() {
        compactLogging("caption") {
            logLine { "line" }
        }
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption line ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should log exception`() {
        compactLogging("caption") {
            logException { RuntimeException("exception") }
        }
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption ϟ RuntimeException: exception at.(CompactRenderingLoggerKtTest.kt:{}) ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should log status`() {
        compactLogging("caption") {
            logStatus("status") { "line" }
        }
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption line (◀◀ status) ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should log result`() {
        compactLogging("caption") {
            "result"
        }
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should log multiple results`() {
        compactLogging("caption") {
            logResult { Result.success(1) }
            logResult { Result.success(2) }
            3
        }
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption ✔
                    │   caption ✔ ✔
                    │   caption ✔ ✔ ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
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
        logResult()

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   caption text line ϟ RuntimeException: exception at.(CompactRenderingLoggerKtTest.kt:146) line (◀◀ status) ✔
                    │{}
                    ╰─────╴✔{}
                """.trimIndent()
        )
    }

    @Test
    fun InMemoryLogger.`should log nested compact`() {
        blockLogging("block") {
            logLine { "something" }
            compactLogging("single") {
                compactLogging {
                    logStatus { IO.Type.OUT typed "ABC" }
                    logLine { "" }
                    logLine { "123" }
                    "abc"
                }
                logLine { "456" }
                compactLogging {
                    logStatus { IO.Type.OUT typed "XYZ" }
                    logLine { "" }
                    logLine { "789" }
                }
            }
            logLine { "something" }
        }

        expectThatLogged().matchesCurlyPattern(
            """
            ╭─────╴{}
            │   
            │   ╭─────╴block
            │   │   
            │   │   something
            │   │   single (ABC ˃  ˃ 123 ˃ ✔) 456 (XYZ ˃  ˃ 789 ˃ ✔) ✔
            │   │   something
            │   │
            │   ╰─────╴✔
        """.trimIndent()
        )
    }
}
