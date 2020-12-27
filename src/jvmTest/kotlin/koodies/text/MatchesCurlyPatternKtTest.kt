package koodies.text

import koodies.terminal.ANSI
import koodies.test.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class MatchesCurlyPatternKtTest {

    @Test
    fun `should match matching single line string`() {
        expectThat("this is a test".matchesCurlyPattern("this is a {}")).isTrue()
    }

    @Test
    fun `should not match non-matching single line string`() {
        expectThat("this is a test").not { matchesCurlyPattern("this is also a {}") }
    }

    @Test
    fun `should match matching multi line string`() {
        expectThat("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] in /Users/bkahlert/Development/com.imgcstmzr.
            Started Process[pid=72692, exitValue=0]
            Process[pid=72692, exitValue=0] stopped with exit code 0
        """.trimIndent().matchesCurlyPattern("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] in {}
            Started Process[pid={}, exitValue={}]
            Process[pid={}, exitValue={}] stopped with exit code {}
        """.trimIndent())).isTrue()
    }

    @Test
    fun `should not match non-matching multi line string`() {
        expectThat("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] instantly.
            Started Process[pid=72692, exitValue=0]
            Process[pid=72692, exitValue=0] stopped with exit code 0
        """.trimIndent()).not {
            matchesCurlyPattern("""
                    Executing [sh, -c, >&1 echo "test output"
                    >&2 echo "test error"] in {}
                    Started Process[pid={}, exitValue={}]
                    Process[pid={}, exitValue={}] stopped with exit code {}
                """.trimIndent())
        }
    }

    @Test
    fun `should remove trailing line break by default`() {
        expectThat("abc\n").matchesCurlyPattern("abc")
    }

    @Test
    fun `should allow to deactivate trailing line removal`() {
        expectThat("abc\n").not { matchesCurlyPattern("abc", removeTrailingBreak = false) }
    }

    @Test
    fun `should remove ANSI escape sequences by default`() {
        expectThat("${ANSI.termColors.red("ab")}c").matchesCurlyPattern("abc")
    }

    @Test
    fun `should allow to deactivate removal of ANSI escape sequences`() {
        expectThat("${ANSI.termColors.red("ab")}c").not { matchesCurlyPattern("abc", removeEscapeSequences = false) }
    }

    @Test
    fun `should allow to ignore trailing lines`() {
        expectThat("${ANSI.termColors.red("ab")}c").not { matchesCurlyPattern("abc\nxxx\nyyy", ignoreTrailingLines = true) }
    }
}
