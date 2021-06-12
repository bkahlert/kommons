package koodies.test.output

import koodies.debug.CapturedOutput
import koodies.debug.Debug
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.SystemIOExclusive
import koodies.test.toStringContains
import koodies.text.ANSI.ansiRemoved
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.contains

class InMemoryLoggerResolverDebugTest {

    @Nested
    inner class SuccessTests {

        @SystemIOExclusive
        @Debug(includeInReport = false)
        @Test
        fun InMemoryLogger.`should log to console automatically with @Debug`(output: CapturedOutput) {
            logLine { "test" }

            expectThatLogged().contains("test")
            expectThat(output).toStringContains("test")
        }
    }

    @Nested
    inner class FailureTests {

        @SystemIOExclusive
        @Debug(includeInReport = false)
        @Test
        fun InMemoryLogger.`should log to console automatically with @Debug`(output: CapturedOutput) {
            logLine { "test" }

            expectThrows<RuntimeException> {
                logResult<Any> { Result.failure(RuntimeException("test")) }
            }

            expectThatLogged().contains("test")
            expectThat(output.ansiRemoved).contains("test")
        }
    }
}
