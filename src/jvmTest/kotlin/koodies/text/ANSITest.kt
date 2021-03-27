package koodies.text

import koodies.test.testEach
import koodies.text.ANSI.Style.bold
import koodies.text.ANSI.Style.strikethrough
import koodies.text.ANSI.Style.underline
import koodies.text.ANSI.containsEscapeSequences
import koodies.text.ANSI.escapeSequencesRemoved
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class ANSITest {

    private val italicCyan = ANSI.Style.italic + ANSI.Colors.cyan
    private val ESC = Unicode.escape

    @Nested
    inner class Formatting {

        @Test
        fun `should format`() {
            expectThat(italicCyan("string")).containsEscapeSequences()
        }
    }

    @Nested
    inner class RemoveEscapeSequences {

        @TestFactory
        fun `should remove escape sequences`() = testEach(
            italicCyan("${"Important:".underline()} This line has ${"no".strikethrough()} ANSI escapes.\nThis one's ${"bold!".bold()}\r\nLast one is clean.") to
                "Important: This line has no ANSI escapes.\nThis one's bold!\r\nLast one is clean.",

            "[$ESC[0;32m  OK  $ESC[0m] Listening on $ESC[0;1;39mudev Control Socket$ESC[0m." to
                "[  OK  ] Listening on udev Control Socket.",

            "Text" to "Text",
            "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___" to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___",
            
            ) { (ansi, plain) ->
            with { ansi.escapeSequencesRemoved }.then {
                expect { this }.that { isEqualTo(plain) }
                expect { this }.that { not { containsEscapeSequences() } }
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
}

fun <T : CharSequence> Assertion.Builder<T>.containsEscapeSequences(): Assertion.Builder<T> =
    assert("contains ANSI escape sequences") {
        when (val actual = it.toString().containsEscapeSequences) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

inline val <reified T : CharSequence> Assertion.Builder<T>.escapeSequencesRemoved: DescribeableBuilder<String>
    get() = get("escape sequences removed") { escapeSequencesRemoved }
