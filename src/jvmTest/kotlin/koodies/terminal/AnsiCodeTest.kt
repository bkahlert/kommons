package koodies.terminal

import koodies.terminal.AnsiCode.Companion.ESC
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiFormats.bold
import koodies.terminal.AnsiFormats.strikethrough
import koodies.terminal.AnsiFormats.underline
import koodies.text.LineSeparators.CRLF
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class AnsiCodeTest {

    val italicCyan = with(ANSI.termColors) { italic + cyan }
    val ansiFormattedString =
        italicCyan("${"Important:".underline()} This line has ${"no".strikethrough()} ANSI escapes.\nThis one's ${"bold!".bold()}${CRLF}Last one is clean.")
    val expectedLines = listOf(
        "Important: This line has no ANSI escapes.",
        "This one's bold!",
        "Last one is clean.",
    )

    @Suppress("SpellCheckingInspection")
    val expectedAnsiFormattedLines = listOf(
        "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.",
        "$ESC[3;36mThis one's $ESC[1mbold!$ESC[22m$ESC[0m",
        "$ESC[3;36mLast one is clean.$ESC[23;39m$ESC[0m",
    )

    @Suppress("SpellCheckingInspection", "LongLine")
    @TestFactory
    fun `stripping ANSI off of`() = listOf(
        ansiFormattedString to "Important: This line has no ANSI escapes.\nThis one's bold!${CRLF}Last one is clean.",
        "[$ESC[0;32m  OK  $ESC[0m] Listening on $ESC[0;1;39mudev Control Socket$ESC[0m." to
            "[  OK  ] Listening on udev Control Socket.",
        "Text" to "Text",
        "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___" to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___",
    ).flatMap { (formatted, expected) ->
        listOf(
            dynamicTest("\"$formatted\" should produce \"$expected\"") {
                expectThat(formatted).escapeSequencesRemoved.isEqualTo(expected)
            }
        )
    }
}

inline val <reified T : CharSequence> Assertion.Builder<T>.escapeSequencesRemoved: DescribeableBuilder<String>
    get() = get("escape sequences removed") { removeEscapeSequences() }
