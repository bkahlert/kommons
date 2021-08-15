package com.bkahlert.kommons.text.styling

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class BordersTest {

    private val border = "╭─╮\n│*│\n╰─╯"

    @Nested
    inner class SingleLine {
        private val singleLine = "test"

        @Test
        fun `should draw border with no padding and no margin`() {

            expectThat(singleLine.wrapWithBorder(border, padding = 0, margin = 0)).isEqualTo("""
                ╭────╮
                │test│
                ╰────╯
            """.trimIndent())
        }

        @Test
        fun `should draw border with no padding but margin`() {
            expectThat(singleLine.wrapWithBorder(border, padding = 2, margin = 0)).isEqualTo("""
                ╭────────╮
                │********│
                │**test**│
                │********│
                ╰────────╯
            """.trimIndent())
        }

        @Test
        fun `should draw border with padding but no margin`() {
            expectThat(singleLine.wrapWithBorder(border, padding = 0, margin = 2)).isEqualTo("""
                **********
                **╭────╮**
                **│test│**
                **╰────╯**
                **********
            """.trimIndent())
        }

        @Test
        fun `should draw border with padding and margin`() {
            expectThat(singleLine.wrapWithBorder(border, padding = 2, margin = 2)).isEqualTo("""
                **************
                **╭────────╮**
                **│********│**
                **│**test**│**
                **│********│**
                **╰────────╯**
                **************
            """.trimIndent())
        }
    }

    @Nested
    inner class MultiLine {
        private val multiLine = """
                   foo
              bar baz
        """

        @Test
        fun `should center text and draw border with no padding and no margin`() {
            expectThat(multiLine.wrapWithBorder(border, padding = 0, margin = 0)).isEqualTo("""
                ╭───────╮
                │*******│
                │**foo**│
                │bar baz│
                │*******│
                ╰───────╯
            """.trimIndent())
        }

        @Test
        fun `should center text and draw border with no padding but margin`() {
            expectThat(multiLine.wrapWithBorder(border, padding = 2, margin = 0)).isEqualTo("""
                ╭───────────╮
                │***********│
                │***********│
                │****foo****│
                │**bar baz**│
                │***********│
                │***********│
                ╰───────────╯
            """.trimIndent())
        }

        @Test
        fun `should center text and draw border with padding but no margin`() {
            expectThat(multiLine.wrapWithBorder(border, padding = 0, margin = 2)).isEqualTo("""
                *************
                **╭───────╮**
                **│*******│**
                **│**foo**│**
                **│bar baz│**
                **│*******│**
                **╰───────╯**
                *************
            """.trimIndent())
        }

        @Test
        fun `should center text and draw border with padding and margin`() {
            expectThat(multiLine.wrapWithBorder(border, padding = 2, margin = 2)).isEqualTo("""
                *****************
                **╭───────────╮**
                **│***********│**
                **│***********│**
                **│****foo****│**
                **│**bar baz**│**
                **│***********│**
                **│***********│**
                **╰───────────╯**
                *****************
            """.trimIndent())
        }
    }
}
