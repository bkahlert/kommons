package koodies.text

import koodies.runtime.isDeveloping
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.Style
import koodies.text.ANSI.Style.bold
import koodies.text.ANSI.Style.hidden
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.ANSI.colorize
import koodies.text.ANSI.containsEscapeSequences
import koodies.text.LineSeparators.CRLF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import koodies.text.Unicode.escape as e

class ANSITest {

    private val italicCyan = Style.italic + ANSI.Colors.cyan

    @Nested
    inner class Formatting {

        @Test
        fun `should format`() {
            expectThat(italicCyan("string")).containsEscapeSequences()
        }
    }

    @Nested
    inner class AnsiRemoved {

        @TestFactory
        fun `should remove escape sequences`() = testEach(
            italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean.") to
                "Important: This line has no ANSI escapes.\nThis one's bold!${CRLF}Last one is clean.",

            "[$e[0;32m  OK  $e[0m] Listening on $e[0;1;39mudev Control Socket$e[0m." to
                "[  OK  ] Listening on udev Control Socket.",

            "Text" to "Text",
            "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___" to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___",

            ) { (ansi, plain) ->
            with { ansi.ansiRemoved }.then {
                expecting { this } that { isEqualTo(plain) }
                expecting { this } that { not { containsEscapeSequences() } }
            }
        }
    }

    @Nested
    inner class ContainsEscapeSequences {

        @Test
        fun `should be true`() {
            expectThat("string".bold()).containsEscapeSequences()
        }

        @Test
        fun `should be false`() {
            expectThat("string").not { containsEscapeSequences() }
        }
    }

    @Nested
    inner class ColorsTest {

        @Test
        fun `should colorize magenta on cyan`() {
            expectThat("magenta on cyan".ansi.magenta.on.cyan).toStringIsEqualTo(ANSI.Colors.magenta.on(ANSI.Colors.cyan)("magenta on cyan").toString(), false)
        }

        @Test
        fun `should colorize black`() {
            expectThat("black".ansi.black).toStringIsEqualTo(ANSI.Colors.black("black").toString(), false)
        }

        @Test
        fun `should colorize red`() {
            expectThat("red".ansi.red).toStringIsEqualTo(ANSI.Colors.red("red").toString(), false)
        }

        @Test
        fun `should colorize green`() {
            expectThat("green".ansi.green).toStringIsEqualTo(ANSI.Colors.green("green").toString(), false)
        }

        @Test
        fun `should colorize yellow`() {
            expectThat("yellow".ansi.yellow).toStringIsEqualTo(ANSI.Colors.yellow("yellow").toString(), false)
        }

        @Test
        fun `should colorize blue`() {
            expectThat("blue".ansi.blue).toStringIsEqualTo(ANSI.Colors.blue("blue").toString(), false)
        }

        @Test
        fun `should colorize magenta`() {
            expectThat("magenta".ansi.magenta).toStringIsEqualTo(ANSI.Colors.magenta("magenta").toString(), false)
        }

        @Test
        fun `should colorize cyan`() {
            expectThat("cyan".ansi.cyan).toStringIsEqualTo(ANSI.Colors.cyan("cyan").toString(), false)
        }

        @Test
        fun `should colorize white`() {
            expectThat("white".ansi.white).toStringIsEqualTo(ANSI.Colors.white("white").toString(), false)
        }

        @Test
        fun `should colorize gray`() {
            expectThat("gray".ansi.gray).toStringIsEqualTo(ANSI.Colors.gray("gray").toString(), false)
        }

        @Test
        fun `should colorize brightRed`() {
            expectThat("brightRed".ansi.brightRed).toStringIsEqualTo(ANSI.Colors.brightRed("brightRed").toString(), false)
        }

        @Test
        fun `should colorize brightGreen`() {
            expectThat("brightGreen".ansi.brightGreen).toStringIsEqualTo(ANSI.Colors.brightGreen("brightGreen").toString(), false)
        }

        @Test
        fun `should colorize brightYellow`() {
            expectThat("brightYellow".ansi.brightYellow).toStringIsEqualTo(ANSI.Colors.brightYellow("brightYellow").toString(), false)
        }

        @Test
        fun `should colorize brightBlue`() {
            expectThat("brightBlue".ansi.brightBlue).toStringIsEqualTo(ANSI.Colors.brightBlue("brightBlue").toString(), false)
        }

        @Test
        fun `should colorize brightMagenta`() {
            expectThat("brightMagenta".ansi.brightMagenta).toStringIsEqualTo(ANSI.Colors.brightMagenta("brightMagenta").toString(), false)
        }

        @Test
        fun `should colorize brightCyan`() {
            expectThat("brightCyan".ansi.brightCyan).toStringIsEqualTo(ANSI.Colors.brightCyan("brightCyan").toString(), false)
        }

        @Test
        fun `should colorize brightWhite`() {
            expectThat("brightWhite".ansi.brightWhite).toStringIsEqualTo(ANSI.Colors.brightWhite("brightWhite").toString(), false)
        }

        @Test
        fun `should colorize background`() {
            expectThat("cyan bg".ansi.cyan.bg).toStringIsEqualTo(ANSI.Colors.cyan.bg("cyan bg").toString(), false)
        }

        @Test
        fun `should colorize foreground and background`() {
            expectThat("cyan on magenta".ansi.cyan.on.magenta).toStringIsEqualTo((ANSI.Colors.cyan + ANSI.Colors.magenta.bg)("cyan on magenta").toString(),
                false)
        }

        @Test
        fun `should colorize random`() {
            expectThat("random".ansi.random).toStringIsEqualTo("random").get { filter { it == Unicode.escape }.count() }.isEqualTo(2)
        }

        @Test
        fun `should colorize each character`() {
            expectThat("colorized".colorize()).toStringIsEqualTo("colorized").get { filter { it == Unicode.escape }.count() }.isEqualTo(2 * 9)
        }
    }

    @Nested
    inner class StyleTest {

        @Test
        fun `should format bold`() {
            expectThat("bold".ansi.bold).toStringIsEqualTo(Style.bold("bold").toString(), false)
        }

        @Test
        fun `should format dim`() {
            expectThat("dim".ansi.dim).toStringIsEqualTo(Style.dim("dim").toString(), false)
        }

        @Test
        fun `should format italic`() {
            expectThat("italic".ansi.italic).toStringIsEqualTo(Style.italic("italic").toString(), false)
        }

        @Test
        fun `should format underline`() {
            expectThat("underline".ansi.underline).toStringIsEqualTo(Style.underline("underline").toString(), false)
        }

        @Test
        fun `should format inverse`() {
            expectThat("inverse".ansi.inverse).toStringIsEqualTo(Style.inverse("inverse").toString(), false)
        }

        @Test
        fun `should format hidden`() {
            expectThat("hidden".ansi.hidden)
                .toStringIsEqualTo(if (isDeveloping) " ".repeat(("hidden".length * 1.35).toInt()) else hidden("hidden").toString(), false)
        }

        @Test
        fun `should format strikethrough`() {
            expectThat("strikethrough".ansi.strikethrough).toStringIsEqualTo(Style.strikethrough("strikethrough").toString(), false)
        }
    }
}

fun <T : CharSequence> Assertion.Builder<T>.containsEscapeSequences(): Assertion.Builder<T> =
    assert("contains ANSI escape sequences") {
        when (val actual = it.toString().containsEscapeSequences) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

inline val <reified T : CharSequence> Assertion.Builder<T>.ansiRemoved: DescribeableBuilder<String>
    get() = get("escape sequences removed") { ansiRemoved }
