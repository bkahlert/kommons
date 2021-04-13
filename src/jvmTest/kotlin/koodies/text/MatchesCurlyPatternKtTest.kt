package koodies.text

import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.functional.compositionOf
import koodies.terminal.ANSI
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.gray
import koodies.terminal.AnsiColors.magenta
import koodies.test.test
import koodies.test.testEach
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.isMultiline
import koodies.text.LineSeparators.unify
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.Semantics.Symbols
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isTrue

@Execution(SAME_THREAD)
class MatchesCurlyPatternKtTest {

    @Test
    fun `should match matching single line string`() {
        expectThat("this is a test".matchesCurlyPattern("this is a {}")).isTrue()
    }

    @Test
    fun `should not match non-matching single line string`() {
        expectThat("this is a test").not { matchesCurlyPattern("this is also a {}") }
    }

    @TestFactory
    fun `should match matching multi line string`() = LineSeparators.testEach { lineSeparator ->

        val block = listOf(
            """Executing [sh, -c, >&1 echo "test output"""",
            """>&2 echo "test error"] in /Users/bkahlert/Development/com.imgcstmzr.""",
            """Started Process(pid=72692, exitValue=0)""",
            """Process(pid=72692, exitValue=0) stopped with exit code 0""",
        ).joinToString(lineSeparator)

        val pattern = """
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] in {}
            Started Process(pid={}, exitValue={})
            Process(pid={}, exitValue={}) stopped with exit code {}
        """.trimIndent()

        expect { block.matchesCurlyPattern(pattern) }.that { isTrue() }
        expect { block }.that { matchesCurlyPattern(pattern) }
    }

    @Test
    fun `should not match non-matching multi line string`() {
        expectThat("""
            Executing [sh, -c, >&1 echo "test output"
            >&2 echo "test error"] instantly.
            Started Process(pid=72692, exitValue=0)
            Process(pid=72692, exitValue=0) stopped with exit code 0
        """.trimIndent()).not {
            matchesCurlyPattern("""
                    Executing [sh, -c, >&1 echo "test output"
                    >&2 echo "test error"] in {}
                    Started Process(pid={}, exitValue={})
                    Process(pid={}, exitValue={}) stopped with exit code {}
                """.trimIndent())
        }
    }

    @Test
    fun `should not match non-matching multi line string2`() {
        expectThat("""
            ▶ ManagedJavaProcess(delegate=Process(pid=27252, exitValue="not exited"), noErrors=✅, started=false, commandLine=/var/folders/hh/739sq9w1 … /koodies.process.o50.sh, .isA<Failed>()processTerminationCallback=${Symbols.Null}, destroyOnShutdown=✅)
            · Executing /var/folders/hh/739sq9w11lv2hvgh7ymlwwzr20wd76/T/koodies12773028758187394965/ScriptsKtTest.SynchronousExecution.should_process_log_to_consol
            · e_by_default-CapturedOutput-UniqueId/koodies.process.o50.sh
            · ${Symbols.Document} file:///var/folders/hh/739sq9w11lv2hvgh7ymlwwzr20wd76/T/koodies12773028758187394965/ScriptsKtTest.SynchronousExecution.should_process_log_to_console_by_default-CapturedOutput-UniqueId/koodies.process.o50.sh
            · test output 1
            · test output 2
            · test error 1
            · test error 2
            · Process 27252 terminated successfully at 2021-02-23T02:31:53.968444Z.
        """.trimIndent()) {
            matchesCurlyPattern("""
                    ▶{}commandLine{{}}
                    · Executing {{}}
                    · {} file:{}
                    · test output 1
                    · test output 2
                    · test error 1
                    · test error 2{{}} 
                """.trimIndent())
        }
    }

    @TestFactory
    fun `should match line breaks`() = test("""
            ▶{}commandLine{{}}
            · Executing {}
            · {} file:{}
            · test output 1
            · test output 2
            · test error 1
            · test error 2{{}}
        """.trimIndent()) { pattern ->

        test("matching lines") {
            expectThat("""
            ▶ ManagedJavaProcess(delegate=Process(pid=98199, exitValue="not exited"), noErrors=✅, started=false, commandLine=/bin/sh -c "echo \"test  …
              est error 2\"; sleep 1"; .isA<Failed>()processTerminationCallback=${Symbols.Null}, destroyOnShutdown=✅)
            · Executing /bin/sh -c "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"
            · ${Symbols.Document} file:///bin/sh
            · test output 1
            · test output 2
            · test error 1
            · test error 2
            Process 98199 terminated successfully at ….
            
        """.trimIndent()).matchesCurlyPattern(pattern)
        }

        test("no second line") {
            expectThat("""
            ▶ ManagedJavaProcess(delegate=Process(pid=98199, exitValue="not exited"), noErrors=✅, started=false, commandLine=/bin/sh -c "echo \"test  … 
            · Executing /bin/sh -c "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"
            · ${Symbols.Document} file:///bin/sh
            · test output 1
            · test output 2
            · test error 1
            · test error 2
            
        """.trimIndent()).matchesCurlyPattern(pattern)
        }

        test("no additional line at end") {
            expectThat("""
            ▶ ManagedJavaProcess(delegate=Process(pid=98199, exitValue="not exited"), noErrors=✅, started=false, commandLine=/bin/sh -c "echo \"test  … 
              est error 2\"; sleep 1", .isA<Failed>()processTerminationCallback=${Symbols.Null}, destroyOnShutdown=✅)
            · Executing /bin/sh -c "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"
            · ${Symbols.Document} file:///bin/sh
            · test output 1
            · test output 2
            · test error 1
            · test error 2
            
        """.trimIndent()).matchesCurlyPattern(pattern)
        }
    }

    @Test
    fun `should remove trailing line break by default`() {
        expectThat("abc$LF").matchesCurlyPattern("abc")
    }

    @Test
    fun `should allow to deactivate trailing line removal`() {
        expectThat("abc$LF").not { matchesCurlyPattern("abc", removeTrailingBreak = false) }
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

    @Test
    fun `should unify whitespace`() {
        expectThat("A B C").toStringMatchesCurlyPattern("A {} C")
    }
}

fun <T : CharSequence> Assertion.Builder<T>.matchesCurlyPattern(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    unifyWhitespaces: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
): Assertion.Builder<T> = assert(if (curlyPattern.isMultiline) "matches curly pattern\n$curlyPattern" else "matches curly pattern $curlyPattern") { actual ->
    val preprocessor = compositionOf(
        true to { s: String -> unify(s) },
        removeTrailingBreak to { s: String -> s.withoutTrailingLineSeparator },
        removeEscapeSequences to { s: String -> s.removeEscapeSequences() },
        unifyWhitespaces to { s: String -> Whitespaces.unify(s) },
        trimmed to { s: String -> s.trim() },
    )
    var processedActual = preprocessor("$actual")
    var processedPattern = preprocessor(curlyPattern)
    if (ignoreTrailingLines) {
        val lines = processedActual.lines().size.coerceAtMost(processedPattern.lines().size)
        processedActual = processedActual.lines().take(lines).joinToString(LF)
        processedPattern = processedPattern.lines().take(lines).joinToString(LF)
    }
    if (processedActual.matchesCurlyPattern(preprocessor.invoke(curlyPattern))) pass()
    else {
        if (processedActual.lines().size == processedPattern.lines().size) {
            val analysis = processedActual.lines().zip(processedPattern.lines()).joinToString("\n$LF") { (actualLine, patternLine) ->
                val lineMatches = actualLine.matchesCurlyPattern(patternLine)
                lineMatches.asEmoji + "   <-\t${actualLine.debug}\nmatch?\t${patternLine.debug}"
            }
            fail(description = "\nbut was: ${if (curlyPattern.isMultiline) "\n$processedActual" else processedActual}\nAnalysis:\n$analysis")
        } else {
            if (processedActual.lines().size > processedPattern.lines().size) {
                fail(description = "\nactual has too many lines:\n${processedActual.highlightTooManyLinesTo(processedPattern)}")
            } else {
                fail(description = "\npattern has too many lines:\n${processedPattern.highlightTooManyLinesTo(processedActual)}")
            }
        }
    }
}

fun <T> Assertion.Builder<T>.toStringMatchesCurlyPattern(
    expected: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
): Assertion.Builder<String> = get { toString() }.matchesCurlyPattern(expected, removeTrailingBreak, removeEscapeSequences, trimmed, ignoreTrailingLines)

private fun String.highlightTooManyLinesTo(other: String): String {
    val lines = lines()
    val tooManyStart = other.lines().size
    val sb = StringBuilder()
    lines.take(tooManyStart).forEach { sb.append(it.gray() + LF) }
    lines.drop(tooManyStart).forEach { sb.append(it.magenta() + LF) }
    @Suppress("ReplaceToStringWithStringTemplate")
    return sb.toString()
}
