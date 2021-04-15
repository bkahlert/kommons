package koodies.text

import koodies.text.Unicode.escape as ESC
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.AnsiStringTest.Companion.ansiString
import koodies.text.AnsiStringTest.Companion.nonAnsiString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class AddColumnKtTest {

    @Nested
    inner class NonAnsiString {
        @Test
        fun `should add string as second column`() {
            expectThat(nonAnsiString.wrapLines(26)
                .addColumn(nonAnsiString.wrapLines(26))).isEqualTo("""
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.
                This one's bold!               This one's bold!
                Last one is clean.             Last one is clean.
            """.trimIndent())
        }

        @Test
        fun `should add fewer lines as second column`() {
            expectThat(nonAnsiString.wrapLines(26)
                .addColumn(nonAnsiString.lines().dropLast(1).joinLinesToString().wrapLines(26))).isEqualTo("""
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.
                This one's bold!               This one's bold!
                Last one is clean.             
            """.trimIndent())
        }

        @Test
        fun `should add more lines as second column`() {
            expectThat(nonAnsiString.wrapLines(26)
                .addColumn(("$nonAnsiString\nThis is one too much.").wrapLines(26))).isEqualTo("""
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.
                This one's bold!               This one's bold!
                Last one is clean.             Last one is clean.
                                               This is one too much.
            """.trimIndent())
        }

        @Test
        fun `should wrap ANSI string as second column`() {
            expectThat(nonAnsiString.wrapLines(26).asAnsiString()
                .addColumn(ansiString.wrapLines(26).asAnsiString())).isEqualTo("""
                Important: This line has n     $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m
                o ANSI escapes.                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m
                This one's bold!               $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                Last one is clean.             $ESC[3;36mLast one is clean.$ESC[23;39m
            """.trimIndent().asAnsiString())
        }
    }

    @Nested
    inner class AnsiString {
        @Test
        fun `should add ANSI string as second column`() {
            expectThat(ansiString.wrapLines(26).asAnsiString()
                .addColumn(ansiString.wrapLines(26).asAnsiString())).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m     $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m
                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m               $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m             $ESC[3;36mLast one is clean.$ESC[23;39m
            """.trimIndent().asAnsiString())
        }

        @Test
        fun `should add fewer lines as second column`() {
            expectThat(ansiString.wrapLines(26).asAnsiString()
                .addColumn(ansiString.lines().dropLast(1).joinLinesToString().asAnsiString().wrapLines(26).asAnsiString())).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m     $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m
                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m               $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m             
            """.trimIndent().asAnsiString())
        }

        @Test
        fun `should add more lines as second column`() {
            expectThat(ansiString.wrapLines(26).asAnsiString()
                .addColumn(("$ansiString\nThis is one too much.").asAnsiString().wrapLines(26).asAnsiString())).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m     $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m
                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m               $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m             $ESC[3;36mLast one is clean.$ESC[23;39m
                                               This is one too much.
            """.trimIndent().asAnsiString())
        }

        @Test
        fun `should wrap non-ANSI string as second column`() {
            expectThat(ansiString.wrapLines(26)
                .addColumn(nonAnsiString.wrapLines(26))).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m     Important: This line has n
                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m                o ANSI escapes.
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m               This one's bold!
                $ESC[3;36mLast one is clean.$ESC[23;39m             Last one is clean.
            """.trimIndent())
        }
    }

    @Test
    fun `should apply specified padding character`() {
        expectThat(ansiString.wrapLines(26).asAnsiString()
            .addColumn(ansiString.wrapLines(26).asAnsiString(), paddingCharacter = '*')).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m*****$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m
                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m****************$ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m***************$ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m*************$ESC[3;36mLast one is clean.$ESC[23;39m
            """.trimIndent().asAnsiString())
    }

    @Test
    fun `should apply specified padding width`() {
        expectThat(ansiString.wrapLines(26).asAnsiString()
            .addColumn(ansiString.wrapLines(26).asAnsiString(), paddingWidth = 10)).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m          $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m
                $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m                     $ESC[3;36;9mo$ESC[29m ANSI escapes.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m                    $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m                  $ESC[3;36mLast one is clean.$ESC[23;39m
            """.trimIndent().asAnsiString())
    }
}
