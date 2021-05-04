package koodies.logging

import koodies.exec.IO
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test

class MicroLoggerTest {

    @Test
    fun InMemoryLogger.`should micro log`() {
        MicroLogger("🤠", null, null, null, { logText { it } }).runLogging {
            logStatus { IO.Output typed "ABC" }
            logLine { "" }
            logLine { "123" }
            "abc"
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   (🤠 ABC ˃ 123 ˃ ✔︎)
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should micro inside of compact scope`() {

        compactLogging("caption") {
            logLine { "something" }
            compactLogging("🤠") {
                logStatus { IO.Output typed "ABC" }
                logLine { "" }
                logLine { "123" }
                "abc"
            }
            logLine { "something" }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭──╴{}
            │
            │   caption something (🤠 ABC ˃ 123 ˃ ✔︎) something ✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
