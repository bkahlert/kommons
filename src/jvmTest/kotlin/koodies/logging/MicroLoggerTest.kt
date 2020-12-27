package koodies.logging

import koodies.concurrent.process.IO
import koodies.test.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT

@Execution(CONCURRENT)
class MicroLoggerTest {

    @Test
    fun InMemoryLogger.`should micro log`() {
        logging("segment") {
            logLine { "something" }
            singleLineLogging("single") {
                microLogging {
                    logStatus { IO.Type.OUT typed "ABC" }
                    logLine { "" }
                    logLine { "123" }
                    "abc"
                }
                logLine { "456" }
                microLogging {
                    logStatus { IO.Type.OUT typed "XYZ" }
                    logLine { "" }
                    logLine { "789" }
                }
            }
            logLine { "something" }
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴{}
            │   
            │   
            │   ╭─────╴segment
            │   │   
            │   │   something
            │   │   single: (ABC ˃  ˃ 123 ˃ ✔ returned abc) 456 (XYZ ˃  ˃ 789 ˃ ✔) ✔
            │   │   something
            │   │
            │   ╰─────╴✔
            │
        """.trimIndent())
    }
}
