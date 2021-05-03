package koodies.text

import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class PrefixLinesKtTest {
    @Test
    fun `should add prefix to each line`() {
        val prefixedLines = "12345     12345\nsnake    snake".prefixLinesWith("ab ", ignoreTrailingSeparator = true)
        expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake")
    }

    @Test
    fun `should do nothing on empty prefix`() {
        val prefixedLines = "12345     12345\nsnake    snake".prefixLinesWith("", ignoreTrailingSeparator = true)
        expectThat(prefixedLines).isEqualTo("12345     12345\nsnake    snake")
    }

    @Test
    fun `should keep trailing new line`() {
        val prefixedLines = "12345     12345\nsnake    snake$LF".prefixLinesWith("ab ", ignoreTrailingSeparator = true)
        expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake$LF")
    }

    @Test
    fun `should prefix trailing new line if not ignored`() {
        val prefixedLines = "12345     12345\nsnake    snake$LF".prefixLinesWith("ab ", ignoreTrailingSeparator = false)
        expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake\nab ")
    }
}
