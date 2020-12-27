package koodies.text

import koodies.terminal.AnsiColors.magenta
import koodies.terminal.AnsiFormats.italic
import koodies.terminal.AnsiString.Companion.asAnsiString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class MaxLineLengthKtTest {

    @Test
    fun `should return max length on multi line`() {
        expectThat("ab\nc".maxLineLength()).isEqualTo(2)
    }

    @Test
    fun `should return max length on single line`() {
        expectThat("abc".maxLineLength()).isEqualTo(3)
    }

    @Test
    fun `should return max length on empty line`() {
        expectThat("".maxLineLength()).isEqualTo(0)
    }

    @Test
    fun `should return max length on trailing line`() {
        expectThat("\n".maxLineLength()).isEqualTo(0)
    }

    @Test
    fun `should return max length on mixed ansi string line`() {
        expectThat("default\n${"magenta".magenta()}".maxLineLength()).isEqualTo(7)
    }

    @Test
    fun `should return max length on ansi string`() {
        expectThat("default\n${"magenta".magenta()}".italic().asAnsiString().maxLineLength()).isEqualTo(7)
    }
}
