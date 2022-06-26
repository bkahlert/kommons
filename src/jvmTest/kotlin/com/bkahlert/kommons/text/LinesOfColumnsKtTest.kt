package com.bkahlert.kommons.text

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.Unicode
import com.bkahlert.kommons.test.AnsiRequiring
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class LinesOfColumnsKtTest {

    @TestFactory
    fun `should be split into lines with columns`() = testEach<CharSequence.() -> List<CharSequence>>(
        { linesOfColumnsSequence(3).toList() },
        { linesOfColumns(3) },
    ) { fn ->
        "12345😀7890${LineSeparators.LF}".fn().map { it.toString() }.shouldContainExactly(
            "123",
            "45",
            "😀7",
            "890",
            "",
        )
    }

    @Nested
    inner class WrapLinesKtTest {

        private val space = " "

        @Nested
        inner class NonAnsi {

            private val text = "12345😀7890"

            @Test
            fun `should wrap non-ANSI lines`() {
                expectThat(text.wrapLines(3)).isEqualTo(
                    """
                123
                45$space
                😀7
                890
            """.trimIndent()
                )
            }

            @Test
            fun `should wrap non-ANSI lines idempotent`() {
                expectThat(text.wrapLines(3).wrapLines(3)).isEqualTo(text.wrapLines(3))
            }
        }

        @Nested
        inner class Ansi {

            private val text = "${"12345".ansi.cyan}😀7890".ansi.bold

            @AnsiRequiring @Test
            fun `should wrap ANSI lines`() {
                expectThat(text.wrapLines(3)).isEqualTo(
                    """
                ${Unicode.ESCAPE}[1m${Unicode.ESCAPE}[36m123${Unicode.ESCAPE}[22;39m
                ${Unicode.ESCAPE}[1;36m45${Unicode.ESCAPE}[22;39m$space
                ${Unicode.ESCAPE}[1;36m${Unicode.ESCAPE}[39m😀7${Unicode.ESCAPE}[22m
                ${Unicode.ESCAPE}[1m890${Unicode.ESCAPE}[22m
            """.trimIndent()
                )
            }

            @Test
            fun `should wrap ANSI lines idempotent`() {
                expectThat(text.wrapLines(3).wrapLines(3)).isEqualTo(text.wrapLines(3))
            }
        }
    }
}
