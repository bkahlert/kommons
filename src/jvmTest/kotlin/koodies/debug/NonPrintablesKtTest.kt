package koodies.debug

import koodies.text.Unicode
import koodies.text.Unicode.zeroWidthSpace
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class NonPrintablesKtTest {

    @Test
    fun `should not replace common characters`() {
        val string = "A to z 0 to 9 Α to ω 𝌀 to 𝍖"
        expectThat(string.replaceNonPrintableCharacters()).isEqualTo(string)
    }

    @Test
    fun `should replace control characters`() {
        expectThat((0 until 32).map { i -> Unicode[i] }.joinToString(" ").replaceNonPrintableCharacters())
            .isEqualTo("␀ ␁ ␂ ␃ ␄ ␅ ␆ ␇ ␈ ␉ ⏎␊ ␋ ␌ ⏎␍ ␎ ␏ ␐ ␑ ␒ ␓ ␔ ␕ ␖ ␗ ␘ ␙ ␚ ␛ ␜ ␝ ␞ ␟")
    }

    @Test
    fun `should replace blank characters but simple space`() {
        expectThat("                  $zeroWidthSpace    ".replaceNonPrintableCharacters())
            .isEqualTo("❲EN SPACE❳ ❲EM SPACE❳ ❲THREE-PER-EM SPACE❳ ❲FOUR-PER-EM SPACE❳ ❲SIX-PER-EM SPACE❳ ❲FIGURE SPACE❳ ❲PUNCTUATION SPACE❳ ❲THIN SPACE❳ ❲HAIR SPACE❳ ❲ZERO WIDTH SPACE❳ ❲NARROW NO-BREAK SPACE❳ ❲MEDIUM MATHEMATICAL SPACE❳")
    }

    @Test
    fun `should replace symbol characters`() {
        expectThat("␈".replaceNonPrintableCharacters())
            .isEqualTo("␈ꜝ")
    }

    @Test
    fun `should replace surrogates`() {
        expectThat("\ud800 \udc00".replaceNonPrintableCharacters())
            .isEqualTo("D800▌﹍ ﹍▐DC00")
    }
}
