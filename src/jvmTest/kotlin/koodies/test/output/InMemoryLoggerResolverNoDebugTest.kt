package koodies.test.output

import koodies.concurrent.process.IO
import koodies.debug.CapturedOutput
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.test.SystemIoRead
import koodies.test.toStringContains
import koodies.text.containsEscapeSequences
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isFailure
import strikt.assertions.isNotEmpty

@Execution(SAME_THREAD)
class InMemoryLoggerResolverNoDebugTest {

    @SystemIoRead
    @Test
    fun InMemoryLogger.`should not automatically log to console without @Debug`(output: CapturedOutput) {
        logStatus { IO.OUT typed "☎Σ⊂⊂(☉ω☉∩)" }

        expectThatLogged().contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.removeEscapeSequences()).not { toStringContains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun InMemoryLogger.`should not catch exceptions`() {
        logStatus { IO.OUT typed "(*｀へ´*)" }

        expectCatching { logResult<Any> { Result.failure(IllegalStateException("test")) } }
            .isFailure().isA<IllegalStateException>()
    }


    @Nested
    inner class ToString {

        @Test
        fun InMemoryLogger.`should not contain escape sequences by default`() {
            logLine { "line" }
            expectThat(toString())
                .isNotEmpty()
                .not { containsEscapeSequences() }
        }

        @Test
        fun InMemoryLogger.`should keep escape sequences if specified`() {
            logLine { "line" }
            expectThat(toString(keepEscapeSequences = true))
                .isNotEmpty()
                .containsEscapeSequences()
        }

        @Nested
        inner class Initial {

            @Test
            fun InMemoryLogger.`should render empty`() {
                expectThat(toString()).isEmpty()
            }

            @Test
            fun InMemoryLogger.`should ignore fallback`() {
                expectThat(toString(fallbackReturnValue = InMemoryLogger.NO_RETURN_VALUE)).isEmpty()
            }
        }

        @Nested
        inner class Open {

            @Test
            fun InMemoryLogger.`should render successful`() {
                logLine { "line" }
                expectThat(toString()).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   line
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should use fallback return value if specified`() {
                logLine { "line" }
                expectThat(toString(fallbackReturnValue = InMemoryLogger.NO_RETURN_VALUE)).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │   line
                    ╵
                    ╵
                    ⌛️ async computation
                """.trimIndent())
            }
        }

        @Nested
        inner class Closed {

            @Test
            fun InMemoryLogger.`should render closed`() {
                logResult()

                expectThat(toString()).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should ignore fallback return value if specified`() {
                logResult()

                expectThat(toString(fallbackReturnValue = InMemoryLogger.NO_RETURN_VALUE)).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }

            @Test
            fun InMemoryLogger.`should ignore further result logs`() {
                logResult()
                logResult()

                expectThat(toString()).matchesCurlyPattern("""
                    ╭──╴{}
                    │   
                    │
                    ╰──╴✔︎
                """.trimIndent())
            }
        }
    }
}
