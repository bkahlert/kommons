package koodies.test.output

import koodies.concurrent.process.IO
import koodies.debug.CapturedOutput
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.Debug
import koodies.test.SystemIOExclusive
import koodies.text.ANSI.ansiRemoved
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class InMemoryLoggerResolverDebugTest {

    @Nested
    inner class SuccessTests {

        @SystemIOExclusive
        @Debug(includeInReport = false)
        @Test
        fun InMemoryLogger.`should log to console automatically with @Debug`(output: CapturedOutput) {
            logLine { IO.Output typed "☎Σ⊂⊂(☉ω☉∩)" }

            expectThatLogged().contains("☎Σ⊂⊂(☉ω☉∩)")
            expectThat(output.ansiRemoved).contains("☎Σ⊂⊂(☉ω☉∩)")
        }
    }

    @Nested
    inner class FailureTests {

        @SystemIOExclusive
        @Debug(includeInReport = false)
        @Test
        fun InMemoryLogger.`should log to console automatically with @Debug`(output: CapturedOutput) {
            logLine { IO.Output typed "(*｀へ´*)" }

            expectCatching { logResult { Result.failure<Any>(IllegalStateException("test")) } }
                .isFailure().isA<IllegalStateException>()

            expectThatLogged().contains("(*｀へ´*)")
            expectThat(output.ansiRemoved).contains("(*｀へ´*)")
        }
    }
}
