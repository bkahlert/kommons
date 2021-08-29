package com.bkahlert.kommons.text

import com.bkahlert.kommons.compositionOf
import com.bkahlert.kommons.debug.asEmoji
import com.bkahlert.kommons.debug.debug
import com.bkahlert.kommons.test.AnsiRequiring
import com.bkahlert.kommons.test.test
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.text.LineSeparators.mapLines
import com.bkahlert.kommons.text.LineSeparators.trailingLineSeparatorRemoved
import com.bkahlert.kommons.text.LineSeparators.unify
import com.bkahlert.kommons.text.Semantics.Symbols
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isTrue

class CurlyPatternKtTest {

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

        expecting { block.matchesCurlyPattern(pattern) } that { isTrue() }
        expecting { block } that { matchesCurlyPattern(pattern) }
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
            ▶ JavaExec(process=Process(pid=27252, exitValue="not exited"), noErrors=✅, started=false, commandLine=/var/folders/hh/739sq9w1 … /kommons.exec.o50.sh, .isA<Failed>()execTerminationCallback=${Symbols.Null}, destroyOnShutdown=✅)
            · Executing /var/folders/hh/739sq9w11lv2hvgh7ymlwwzr20wd76/T/kommons12773028758187394965/ScriptsKtTest.SynchronousExecution.should_process_log_to_consol
            · e_by_default-CapturedOutput-UniqueId/kommons.exec.o50.sh
            · ${Symbols.Document} file:///var/folders/hh/739sq9w11lv2hvgh7ymlwwzr20wd76/T/kommons12773028758187394965/ScriptsKtTest.SynchronousExecution.should_process_log_to_console_by_default-CapturedOutput-UniqueId/kommons.exec.o50.sh
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

        expecting("matching lines") {
            """
            ▶ JavaExec(process=Process(pid=98199, exitValue="not exited"), noErrors=✅, started=false, commandLine=/bin/sh -c "echo \"test  …
              est error 2\"; sleep 1"; .isA<Failed>()execTerminationCallback=${Symbols.Null}, destroyOnShutdown=✅)
            · Executing /bin/sh -c "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"
            · ${Symbols.Document} file:///bin/sh
            · test output 1
            · test output 2
            · test error 1
            · test error 2
            Process 98199 terminated successfully at ….
            
        """.trimIndent()
        } that { matchesCurlyPattern(pattern) }

        expecting("no second line") {
            """
            ▶ JavaExec(process=Process(pid=98199, exitValue="not exited"), noErrors=✅, started=false, commandLine=/bin/sh -c "echo \"test  … 
            · Executing /bin/sh -c "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"
            · ${Symbols.Document} file:///bin/sh
            · test output 1
            · test output 2
            · test error 1
            · test error 2
            
        """.trimIndent()
        } that { matchesCurlyPattern(pattern) }

        expecting("no additional line at end") {
            """
            ▶ JavaExec(process=Process(pid=98199, exitValue="not exited"), noErrors=✅, started=false, commandLine=/bin/sh -c "echo \"test  … 
              est error 2\"; sleep 1", .isA<Failed>()execTerminationCallback=${Symbols.Null}, destroyOnShutdown=✅)
            · Executing /bin/sh -c "echo \"test output 1\"; sleep 1; >&2 echo \"test error 1\"; sleep 1; echo \"test output 2\"; >&2 echo \"test error 2\"; sleep 1"
            · ${Symbols.Document} file:///bin/sh
            · test output 1
            · test output 2
            · test error 1
            · test error 2
            
        """.trimIndent()
        } that { matchesCurlyPattern(pattern) }
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
        expectThat("ab".ansi.red.toString() + "c").matchesCurlyPattern("abc")
    }

    @AnsiRequiring @Test
    fun `should allow to deactivate removal of ANSI escape sequences`() {
        expectThat("ab".ansi.red.toString() + "c").not { matchesCurlyPattern("abc", removeAnsi = false) }
    }

    @Test
    fun `should unify whitespace`() {
        expectThat("A B C").toStringMatchesCurlyPattern("A {} C")
    }

    @TestFactory
    fun `should match leading line breaks with multi-line placeholder`() = test("""
            a
            b
            c
        """.trimIndent()) {

        asserting {
            matchesCurlyPattern("""
                                    {{}}a
                                    b
                                    c
                                """.trimIndent())
        }

        asserting {
            matchesCurlyPattern("""
                                    {{}}
                                    a
                                    b
                                    c
                                """.trimIndent())
        }
    }

    @TestFactory
    fun `should match trailing line breaks with multi-line placeholder`() = test("""
            a
            b
            c
        """.trimIndent()) {

        asserting {
            matchesCurlyPattern("""
                                    a
                                    b
                                    c{{}}
                                """.trimIndent())
        }

        asserting {
            matchesCurlyPattern("""
                                    a
                                    b
                                    c
                                    {{}}
                                """.trimIndent())
        }
    }

    @Test
    fun `should right trim spaces on each line`() {
        // first line has trailing whitespaces
        expectThat("""
            │   
            │
        """.trimIndent()).matchesCurlyPattern("""
                    │
                    │   
                """.trimIndent()) // second line has trailing whitespaces
    }
}

fun <T : CharSequence> Assertion.Builder<T>.matchesCurlyPattern(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeAnsi: Boolean = true,
    unifyWhitespaces: Boolean = true,
    trimEnd: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
): Assertion.Builder<T> = assert(if (curlyPattern.isMultiline) "matches curly pattern\n$curlyPattern" else "matches curly pattern $curlyPattern") { actual ->
    val preprocessor = compositionOf(
        true to { s: String -> unify(s) },
        removeTrailingBreak to { s: String -> s.trailingLineSeparatorRemoved },
        removeAnsi to { s: String -> s.ansiRemoved },
        unifyWhitespaces to { s: String -> Whitespaces.unify(s) },
        trimEnd to { s: String -> s.mapLines { it.trimEnd() } },
        trimmed to { s: String -> s.trim() },
    )
    val processedActual = preprocessor("$actual")
    val processedPattern = preprocessor(curlyPattern)
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
    removeAnsi: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
): Assertion.Builder<String> = get { toString() }.matchesCurlyPattern(
    curlyPattern = expected,
    removeTrailingBreak = removeTrailingBreak,
    removeAnsi = removeAnsi,
    trimmed = trimmed,
)

private fun String.highlightTooManyLinesTo(other: String): String {
    val lines = lines()
    val tooManyStart = other.lines().size
    val sb = StringBuilder()
    lines.take(tooManyStart).forEach { sb.append(it.ansi.gray.toString() + LF) }
    lines.drop(tooManyStart).forEach { sb.append(it.ansi.magenta.toString() + LF) }
    @Suppress("ReplaceToStringWithStringTemplate")
    return sb.toString()
}
