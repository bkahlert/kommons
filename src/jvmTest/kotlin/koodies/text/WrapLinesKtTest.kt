package koodies.text

import koodies.terminal.AnsiCode
import koodies.terminal.AnsiStringTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
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
                ${AnsiCode.ESC}[3;36m${AnsiCode.ESC}[4mImportant:${AnsiCode.ESC}[24m This line has ${AnsiCode.ESC}[9mn${AnsiCode.ESC}[23;39;29m
                ${AnsiCode.ESC}[3;36;9mo${AnsiCode.ESC}[29m ANSI escapes.${AnsiCode.ESC}[23;39m
                ${AnsiCode.ESC}[3;36mThis one's ${AnsiCode.ESC}[1mbold!${AnsiCode.ESC}[23;39;22m
                ${AnsiCode.ESC}[3;36mLast one is clean.${AnsiCode.ESC}[23;39m
            """.trimIndent())
    }
}
