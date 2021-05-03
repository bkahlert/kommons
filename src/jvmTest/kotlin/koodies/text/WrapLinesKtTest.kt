package koodies.text

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import koodies.text.Unicode.escape as ESC


class WrapLinesKtTest {
    @Test
    fun `should wrap non-ANSI lines`() {
        expectThat(AnsiStringTest.nonAnsiString.wrapLines(26)).isEqualTo("""
                Important: This line has n
                o ANSI escapes.
                This one's bold!
                Last one is clean.
            """.trimIndent())
    }

    @Test
    fun `should wrap ANSI lines`() {
        expectThat(AnsiStringTest.ansiString.wrapLines(26)).isEqualTo("""
                ${ESC}[3;36m${ESC}[4mImportant:${ESC}[24m This line has ${ESC}[9mn${ESC}[23;39;29m
                ${ESC}[3;36;9mo${ESC}[29m ANSI escapes.${ESC}[23;39m
                ${ESC}[3;36mThis one's ${ESC}[1mbold!${ESC}[23;39;22m
                ${ESC}[3;36mLast one is clean.${ESC}[23;39m
            """.trimIndent())
    }
}
