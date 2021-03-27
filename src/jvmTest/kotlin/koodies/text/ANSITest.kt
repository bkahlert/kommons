package koodies.text

import koodies.text.ANSI.Style.bold
import koodies.text.ANSI.containsEscapeSequences
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat

class ANSITest {

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
