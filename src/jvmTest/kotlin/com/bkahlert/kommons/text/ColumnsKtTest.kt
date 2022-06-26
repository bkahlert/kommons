package com.bkahlert.kommons.text

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.Unicode
import com.bkahlert.kommons.test.AnsiRequiring
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons.text.AnsiStringTest.Companion.ansiString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ColumnsKtTest {

    @Nested
    inner class MaxColumns {

        @Test
        fun `should return max columns on multi line`() {
            expectThat("😀\nc".maxColumns()).isEqualTo(2)
        }

        @Test
        fun `should return max columns on single line`() {
            expectThat("😀c".maxColumns()).isEqualTo(3)
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
            expectThat("def😀lt\n${"magenta".ansi.magenta}".maxColumns()).isEqualTo(7)
        }

        @Test
        fun `should return max columns on ansi string`() {
            expectThat("def😀lt\n${"magenta".ansi.magenta}".ansi.italic.toAnsiString().maxColumns()).isEqualTo(7)
        }

        @TestFactory
        fun `should return max columns on broken ansi`() {
            expecting { "${Unicode.ESCAPE}m".maxColumns() } that { isEqualTo(1) }
            expecting { "${Unicode.ESCAPE}[".maxColumns() } that { isEqualTo(1) }
            expecting { "${Unicode.ESCAPE}m".toAnsiString().maxColumns() } that { isEqualTo(1) }
            expecting { "${Unicode.ESCAPE}[".toAnsiString().maxColumns() } that { isEqualTo(1) }
        }
    }

    @Nested
    inner class NonAnsiString {

        @Test
        fun `should add string as second column`() {
            expectThat(
                ansiString.ansiRemoved.wrapLines(26)
                    .addColumn(ansiString.ansiRemoved.wrapLines(26))
            ).isEqualTo(
                """
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.${"           "}
                This one's bold!               This one's bold!${"          "}
                Last one is clean.             Last one is clean.${"        "}
            """.trimIndent()
            )
        }

        @AnsiRequiring @Test
        fun `should add fewer lines as second column`() {
            expectThat(
                ansiString.ansiRemoved.wrapLines(26)
                    .addColumn(ansiString.ansiRemoved.lines().dropLast(1).joinToString(LineSeparators.Default).wrapLines(26))
            ).isEqualTo(
                """
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.${"           "}
                This one's bold!               This one's bold!${"          "}
                Last one is clean.${"             "}
            """.trimIndent()
            )
        }

        @Test
        fun `should add more lines as second column`() {
            expectThat(
                ansiString.ansiRemoved.wrapLines(26)
                    .addColumn(("${ansiString.ansiRemoved}\nThis is one too much.").wrapLines(26))
            ).isEqualTo(
                """
                Important: This line has n     Important: This line has n
                o ANSI escapes.                o ANSI escapes.${"           "}
                This one's bold!               This one's bold!${"          "}
                Last one is clean.             Last one is clean.${"        "}
                                               This is one too much.${"     "}
            """.trimIndent()
            )
        }

        @AnsiRequiring @Test
        fun `should wrap ANSI string as second column`() {
            expectThat(
                ansiString.ansiRemoved.wrapLines(26).toAnsiString()
                    .addColumn(ansiString.wrapLines(26).toAnsiString())
            ).isEqualTo(
                """
                Important: This line has n     ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m
                o ANSI escapes.                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m${"           "}
                This one's bold!               ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m${"          "}
                Last one is clean.             ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m${"        "}
            """.trimIndent().toAnsiString()
            )
        }
    }

    @AnsiRequiring @Nested
    inner class AnsiString {

        @Test
        fun `should add ANSI string as second column`() {
            expectThat(
                ansiString.wrapLines(26).toAnsiString()
                    .addColumn(ansiString.wrapLines(26).toAnsiString())
            ).isEqualTo(
                """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m     ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m
                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m${"           "}
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m               ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m${"          "}
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m             ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m${"        "}
            """.trimIndent().toAnsiString()
            )
        }

        @Test
        fun `should add fewer lines as second column`() {
            expectThat(
                ansiString.wrapLines(26).toAnsiString()
                    .addColumn(ansiString.lines().dropLast(1).joinToString(LineSeparators.Default).toAnsiString().wrapLines(26).toAnsiString())
            ).isEqualTo(
                """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m     ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m
                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m${"           "}
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m               ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m${"          "}
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m${"             "}
            """.trimIndent().toAnsiString()
            )
        }

        @Test
        fun `should add more lines as second column`() {
            expectThat(
                ansiString.wrapLines(26).toAnsiString()
                    .addColumn(("$ansiString\nThis is one too much.").toAnsiString().wrapLines(26).toAnsiString())
            ).isEqualTo(
                """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m     ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m
                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m${"           "}
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m               ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m${"          "}
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m             ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m${"        "}
                                               This is one too much.${"     "}
            """.trimIndent().toAnsiString()
            )
        }

        @Test
        fun `should wrap non-ANSI string as second column`() {
            expectThat(
                ansiString.wrapLines(26)
                    .addColumn(ansiString.ansiRemoved.wrapLines(26))
            ).isEqualTo(
                """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m     Important: This line has n
                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m                o ANSI escapes.${"           "}
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m               This one's bold!${"          "}
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m             Last one is clean.${"        "}
            """.trimIndent()
            )
        }
    }

    @AnsiRequiring @Test
    fun `should apply specified padding character`() {
        expectThat(
            ansiString.wrapLines(26).toAnsiString()
                .addColumn(ansiString.wrapLines(26).toAnsiString(), paddingCharacter = "*")
        ).isEqualTo(
            """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m*****${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m
                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m           *****${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m${"           "}
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m          *****${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m${"          "}
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m        *****${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m${"        "}
            """.trimIndent().toAnsiString()
        )
    }

    @AnsiRequiring @Test
    fun `should apply specified padding columns`() {
        expectThat(
            ansiString.wrapLines(26).toAnsiString()
                .addColumn(ansiString.wrapLines(26).toAnsiString(), paddingColumns = 10)
        ).isEqualTo(
            """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m          ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m
                ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m                     ${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m${"           "}
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m                    ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m${"          "}
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m                  ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m${"        "}
            """.trimIndent().toAnsiString()
        )
    }

    @Test
    fun `should handle control characters`() {
        val string = "ab".repeat(5)
        val lines = string.wrapLines(5)
        expecting { lines.addColumn(lines) } that {
            isEqualTo(
                """
                ababa     ababa
                babab     babab
            """.trimIndent()
            )
        }
    }

    @Test
    fun `should format multiple plain text columns`() {
        val plainText = ansiString.ansiRemoved
        val linedUp = formatColumns(plainText to 50, plainText to 30, plainText to 10)
        expectThat(linedUp).toStringIsEqualTo(
            """
            Important: This line has no ANSI escapes.              Important: This line has no AN     Important:
            This one's bold!                                       SI escapes.                         This line
            Last one is clean.                                     This one's bold!                    has no AN
                                                                   Last one is clean.                 SI escapes
                                                                                                      .${"         "}
                                                                                                      This one's
                                                                                                       bold!${"    "}
                                                                                                      Last one i
                                                                                                      s clean.${"  "}
        """.trimIndent()
        )
    }

    @AnsiRequiring @Test
    fun `should format multiple ansi columns`() {
        val linedUp = formatColumns(ansiString to 50, ansiString.ansiRemoved.toAnsiString() to 30, ansiString to 10)
        expectThat(linedUp).toStringIsEqualTo(
            """
            ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m              Important: This line has no AN     ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[23;39;24m
            ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m                                       SI escapes.                        ${Unicode.ESCAPE}[3;36;4m${Unicode.ESCAPE}[24m This line${Unicode.ESCAPE}[23;39m
            ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m                                     This one's bold!                   ${Unicode.ESCAPE}[3;36m has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m AN${Unicode.ESCAPE}[23;39m
                                                                   Last one is clean.                 ${Unicode.ESCAPE}[3;36mSI escapes${Unicode.ESCAPE}[23;39m
                                                                                                      ${Unicode.ESCAPE}[3;36m.${Unicode.ESCAPE}[23;39m${"         "}
                                                                                                      ${Unicode.ESCAPE}[3;36mThis one's${Unicode.ESCAPE}[23;39m
                                                                                                      ${Unicode.ESCAPE}[3;36m ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m${"    "}
                                                                                                      ${Unicode.ESCAPE}[3;36mLast one i${Unicode.ESCAPE}[23;39m
                                                                                                      ${Unicode.ESCAPE}[3;36ms clean.${Unicode.ESCAPE}[23;39m${"  "}
        """.trimIndent()
        )
    }
}
