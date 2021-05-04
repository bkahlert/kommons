package koodies.text

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import koodies.text.Unicode.escape as e

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
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m
                $e[3;36mLast one is clean.$e[23;39m
            """.trimIndent())
    }
}
