package com.bkahlert.kommons.debug

import com.bkahlert.kommons.CodePoint
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class NonPrintableKtTest {

    @Test
    fun `should not replace common characters`() {
        val string = "A to z 0 to 9 Α to ω 𝌀 to 𝍖"
        expectThat(string.replaceNonPrintableCharacters()).isEqualTo(string)
    }

    @Test
    fun `should replace control characters`() {
        expectThat((0 until 32).map { i -> CodePoint(i) }.joinToString(" ").replaceNonPrintableCharacters())
            .isEqualTo("␀ ␁ ␂ ␃ ␄ ␅ ␆ ␇ ␈ ␉ ⏎␊ ␋ ␌ ⏎␍ ␎ ␏ ␐ ␑ ␒ ␓ ␔ ␕ ␖ ␗ ␘ ␙ ␚ ␛ ␜ ␝ ␞ ␟")
    }

    @Test
    fun `should replace symbol characters`() {
        expectThat("␈".replaceNonPrintableCharacters())
            .isEqualTo("␈ꜝ")
    }

    @Test
    fun `should replace surrogates`() {
        expectThat("\ud800 \udc00".replaceNonPrintableCharacters())
            .isEqualTo("d800▌﹍ ﹍▐dc00")
    }
}
