package com.bkahlert.kommons.text

import com.bkahlert.kommons.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

class IndentKtTest {

    @Test
    fun `should return indent`() {
        val whitespaces = Unicode.whitespaces.filter { it.isWhitespace() }.joinToString("")
        expectThat("${whitespaces}X".indent).isEqualTo(whitespaces)
    }

    @Test
    fun `should return 0 on no indention`() {
        expectThat("X".indent).isEmpty()
    }

    @Test
    fun `should return 0 on blank string`() {
        expectThat(" ".indent).isEqualTo(" ")
    }

    @Test
    fun `should return 0 on empty string`() {
        expectThat("".indent).isEmpty()
    }

    @Test
    fun `should return indentation of first line`() {
        expectThat("   3\n  2\n    4$LF".indent).isEqualTo("   ")
    }
}
