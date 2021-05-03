package koodies.text

import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class LinesKtTest {

    @Nested
    inner class MaxLength {

        @Test
        fun `should return max length on multi line`() {
            expectThat("ab\nc".maxLength()).isEqualTo(2)
        }

        @Test
        fun `should return max length on single line`() {
            expectThat("abc".maxLength()).isEqualTo(3)
        }

        @Test
        fun `should return max length on empty line`() {
            expectThat("".maxLength()).isEqualTo(0)
        }

        @Test
        fun `should return max length on trailing line`() {
            expectThat(LF.maxLength()).isEqualTo(0)
        }

        @Test
        fun `should return max length on mixed ansi string line`() {
            expectThat("default\n${"magenta".ansi.magenta}".maxLength()).isEqualTo(7)
        }

        @Test
        fun `should return max length on ansi string`() {
            expectThat("default\n${"magenta".ansi.magenta}".ansi.italic.asAnsiString().maxLength()).isEqualTo(7)
        }
    }
}
