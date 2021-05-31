package koodies.test.output

import koodies.debug.CapturedOutput
import koodies.exec.IO
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.SystemIORead
import koodies.test.toStringContains
import koodies.text.ANSI.ansiRemoved
import koodies.text.containsAnsi
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isFailure
import strikt.assertions.isNotEmpty

class InMemoryLoggerResolverNoDebugTest {

    @SystemIORead
    @Test
    fun InMemoryLogger.`should not automatically log to console without @Debug`(output: CapturedOutput) {
        logLine { IO.Output typed "☎Σ⊂⊂(☉ω☉∩)" }

        expectThatLogged().contains("☎Σ⊂⊂(☉ω☉∩)")
        expectThat(output.ansiRemoved).not { toStringContains("☎Σ⊂⊂(☉ω☉∩)") }
    }

    @Test
    fun InMemoryLogger.`should not catch exceptions`() {
        logLine { IO.Output typed "(*｀へ´*)" }

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
                .not { containsAnsi() }
        }

        @Test
        fun InMemoryLogger.`should keep escape sequences if specified`() {
            logLine { "line" }
            expectThat(toString(keepEscapeSequences = true))
                .isNotEmpty()
                .containsAnsi()
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
                    ⏳️
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
