package koodies.logging

import koodies.concurrent.process.IO
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD

@Execution(SAME_THREAD)
class MicroLoggerTest {

    @Test
    fun InMemoryLogger.`should micro log`() {
        MicroLogger("🤠", null, this).runLogging {
            logStatus { IO.OUT typed "ABC" }
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
                logStatus { IO.OUT typed "ABC" }
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

