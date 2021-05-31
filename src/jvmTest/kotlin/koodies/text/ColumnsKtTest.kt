package koodies.text

import koodies.text.ANSI.ansiRemoved
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.AnsiStringTest.Companion.ansiString
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.wrapLines
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import koodies.text.Unicode.escape as e

class ColumnsKtTest {

    @Nested
    inner class NonAnsiString {

        @Test
        fun `should add string as second column`() {
            expectThat(ansiString.ansiRemoved.wrapLines(26)
                .addColumn(ansiString.ansiRemoved.wrapLines(26))).isEqualTo("""
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.           
                This one's bold!               This one's bold!          
                Last one is clean.             Last one is clean.        
            """.trimIndent())
        }

        @Test
        fun `should add fewer lines as second column`() {
            expectThat(ansiString.ansiRemoved.wrapLines(26)
                .addColumn(ansiString.ansiRemoved.lines().dropLast(1).joinToString(LF).wrapLines(26))).isEqualTo("""
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.           
                This one's bold!               This one's bold!          
                Last one is clean.             
            """.trimIndent())
        }

        @Test
        fun `should add more lines as second column`() {
            expectThat(ansiString.ansiRemoved.wrapLines(26)
                .addColumn(("${ansiString.ansiRemoved}\nThis is one too much.").wrapLines(26))).isEqualTo("""
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.           
                This one's bold!               This one's bold!          
                Last one is clean.             Last one is clean.        
                                               This is one too much.     
            """.trimIndent())
        }

        @Test
        fun `should wrap ANSI string as second column`() {
            expectThat(ansiString.ansiRemoved.wrapLines(26).asAnsiString()
                .addColumn(ansiString.wrapLines(26).asAnsiString())).isEqualTo("""
                Important: This line has n     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                o ANSI escapes.                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                This one's bold!               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                Last one is clean.             $e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().asAnsiString())
        }
    }

    @Nested
    inner class AnsiString {

        @Test
        fun `should add ANSI string as second column`() {
            expectThat(ansiString.wrapLines(26).asAnsiString()
                .addColumn(ansiString.wrapLines(26).asAnsiString())).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m             $e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().asAnsiString())
        }

        @Test
        fun `should add fewer lines as second column`() {
            expectThat(ansiString.wrapLines(26).asAnsiString()
                .addColumn(ansiString.lines().dropLast(1).joinToString(LF).asAnsiString().wrapLines(26).asAnsiString())).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m             
            """.trimIndent().asAnsiString())
        }

        @Test
        fun `should add more lines as second column`() {
            expectThat(ansiString.wrapLines(26).asAnsiString()
                .addColumn(("$ansiString\nThis is one too much.").asAnsiString().wrapLines(26).asAnsiString())).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m             $e[3;36mLast one is clean.$e[23;39m        
                                               This is one too much.     
            """.trimIndent().asAnsiString())
        }

        @Test
        fun `should wrap non-ANSI string as second column`() {
            expectThat(ansiString.wrapLines(26)
                .addColumn(ansiString.ansiRemoved.wrapLines(26))).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m     Important: This line has n
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                o ANSI escapes.           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m               This one's bold!          
                $e[3;36mLast one is clean.$e[23;39m             Last one is clean.        
            """.trimIndent())
        }
    }

    @Test
    fun `should apply specified padding character`() {
        expectThat(ansiString.wrapLines(26).asAnsiString()
            .addColumn(ansiString.wrapLines(26).asAnsiString(), paddingCharacter = '*')).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m*****$e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           *****$e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m          *****$e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m        *****$e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().asAnsiString())
    }

    @Test
    fun `should apply specified padding columns`() {
        expectThat(ansiString.wrapLines(26).asAnsiString()
            .addColumn(ansiString.wrapLines(26).asAnsiString(), paddingColumns = 10)).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m          $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                     $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m                    $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m                  $e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().asAnsiString())
    }
}
