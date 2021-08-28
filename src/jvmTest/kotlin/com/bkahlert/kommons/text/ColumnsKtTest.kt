package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.AnsiRequired
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.AnsiStringTest.Companion.ansiString
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.wrapLines
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import com.bkahlert.kommons.text.Unicode.ESCAPE as e

class ColumnsKtTest {

    @Nested
    inner class MaxColumns {

        @Test
        fun `should test with correct assumptions`() {
            expectThat("c".also { it.asGraphemeClusterSequence().sumOf { it.columns } }.also { it.columns }.maxColumns()).isEqualTo(1)
            expectThat("曲".also { it.asGraphemeClusterSequence().sumOf { it.columns } }.also { it.columns }.maxColumns()).isEqualTo(2)
        }

        @Test
        fun `should return max columns on multi line`() {
            expectThat("曲\nc".maxColumns()).isEqualTo(2)
        }

        @Test
        fun `should return max columns on single line`() {
            expectThat("曲c".maxColumns()).isEqualTo(3)
        }

        @Test
        fun `should return max columns on empty line`() {
            expectThat("".maxColumns()).isEqualTo(0)
        }

        @Test
        fun `should return max columns on trailing line`() {
            expectThat(LF.maxColumns()).isEqualTo(0)
        }

        @Test
        fun `should return max columns on mixed ansi string line`() {
            expectThat("def曲lt\n${"magenta".ansi.magenta}".maxColumns()).isEqualTo(7)
        }

        @Test
        fun `should return max columns on ansi string`() {
            expectThat("def曲lt\n${"magenta".ansi.magenta}".ansi.italic.toAnsiString().maxColumns()).isEqualTo(7)
        }

        @TestFactory
        fun `should return max columns on broken ansi`() {
            expecting { "${e}m".maxColumns() } that { isEqualTo(1) }
            expecting { "${e}[".maxColumns() } that { isEqualTo(1) }
            expecting { "${e}m".toAnsiString().maxColumns() } that { isEqualTo(1) }
            expecting { "${e}[".toAnsiString().maxColumns() } that { isEqualTo(1) }
        }
    }

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

        @AnsiRequired @Test
        fun `should add fewer lines as second column`() {
            expectThat(ansiString.ansiRemoved.wrapLines(26)
                .addColumn(ansiString.ansiRemoved.lines().dropLast(1).joinLinesToString().wrapLines(26))).isEqualTo("""
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

        @AnsiRequired @Test
        fun `should wrap ANSI string as second column`() {
            expectThat(ansiString.ansiRemoved.wrapLines(26).toAnsiString()
                .addColumn(ansiString.wrapLines(26).toAnsiString())).isEqualTo("""
                Important: This line has n     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                o ANSI escapes.                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                This one's bold!               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                Last one is clean.             $e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().toAnsiString())
        }
    }

    @AnsiRequired @Nested
    inner class AnsiString {

        @Test
        fun `should add ANSI string as second column`() {
            expectThat(ansiString.wrapLines(26).toAnsiString()
                .addColumn(ansiString.wrapLines(26).toAnsiString())).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m             $e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().toAnsiString())
        }

        @Test
        fun `should add fewer lines as second column`() {
            expectThat(ansiString.wrapLines(26).toAnsiString()
                .addColumn(ansiString.lines().dropLast(1).joinLinesToString().toAnsiString().wrapLines(26).toAnsiString())).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m             
            """.trimIndent().toAnsiString())
        }

        @Test
        fun `should add more lines as second column`() {
            expectThat(ansiString.wrapLines(26).toAnsiString()
                .addColumn(("$ansiString\nThis is one too much.").toAnsiString().wrapLines(26).toAnsiString())).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m     $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m               $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m             $e[3;36mLast one is clean.$e[23;39m        
                                               This is one too much.     
            """.trimIndent().toAnsiString())
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

    @AnsiRequired @Test
    fun `should apply specified padding character`() {
        expectThat(ansiString.wrapLines(26).toAnsiString()
            .addColumn(ansiString.wrapLines(26).toAnsiString(), paddingCharacter = '*')).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m*****$e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           *****$e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m          *****$e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m        *****$e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().toAnsiString())
    }

    @AnsiRequired @Test
    fun `should apply specified padding columns`() {
        expectThat(ansiString.wrapLines(26).toAnsiString()
            .addColumn(ansiString.wrapLines(26).toAnsiString(), paddingColumns = 10)).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m          $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m                     $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m           
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m                    $e[3;36mThis one's $e[1mbold!$e[23;39;22m          
                $e[3;36mLast one is clean.$e[23;39m                  $e[3;36mLast one is clean.$e[23;39m        
            """.trimIndent().toAnsiString())
    }

    @Test
    fun `should handle control characters`() {
        val string = "ab".repeat(5)
        val lines = string.wrapLines(5)
        expecting { lines.addColumn(lines) } that {
            isEqualTo("""
                ababa     ababa
                babab     babab
            """.trimIndent())
        }
    }

    @Test
    fun `should format multiple plain text columns`() {
        val plainText = ansiString.ansiRemoved
        val linedUp = formatColumns(plainText to 50, plainText to 30, plainText to 10)
        expectThat(linedUp).toStringIsEqualTo("""
            Important: This line has no ANSI escapes.              Important: This line has no AN     Important:
            This one's bold!                                       SI escapes.                         This line
            Last one is clean.                                     This one's bold!                    has no AN
                                                                   Last one is clean.                 SI escapes
                                                                                                      .         
                                                                                                      This one's
                                                                                                       bold!    
                                                                                                      Last one i
                                                                                                      s clean.  
        """.trimIndent())
    }

    @AnsiRequired @Test
    fun `should format multiple ansi columns`() {
        val linedUp = formatColumns(ansiString to 50, ansiString.ansiRemoved.toAnsiString() to 30, ansiString to 10)
        expectThat(linedUp).toStringIsEqualTo("""
            $e[3;36m$e[4mImportant:$e[24m This line has $e[9mno$e[29m ANSI escapes.$e[23;39m              Important: This line has no AN     $e[3;36m$e[4mImportant:$e[23;39;24m
            $e[3;36mThis one's $e[1mbold!$e[23;39;22m                                       SI escapes.                        $e[3;36;4m$e[24m This line$e[23;39m
            $e[3;36mLast one is clean.$e[23;39m                                     This one's bold!                   $e[3;36m has $e[9mno$e[29m AN$e[23;39m
                                                                   Last one is clean.                 $e[3;36mSI escapes$e[23;39m
                                                                                                      $e[3;36m.$e[23;39m         
                                                                                                      $e[3;36mThis one's$e[23;39m
                                                                                                      $e[3;36m $e[1mbold!$e[23;39;22m    
                                                                                                      $e[3;36mLast one i$e[23;39m
                                                                                                      $e[3;36ms clean.$e[23;39m  
        """.trimIndent())
    }
}
