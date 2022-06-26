package com.bkahlert.kommons.text

import com.bkahlert.kommons.Unicode
import com.bkahlert.kommons.test.AnsiRequiring
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@AnsiRequiring
class BannerTest {

    private val rainbow =
        "${Unicode.ESCAPE}[90;40m▒${Unicode.ESCAPE}[39;49m${Unicode.ESCAPE}[96;46m▒${Unicode.ESCAPE}[39;49m${Unicode.ESCAPE}[94;44m▒${Unicode.ESCAPE}[39;49m${Unicode.ESCAPE}[92;42m▒${Unicode.ESCAPE}[39;49m${Unicode.ESCAPE}[93;43m▒${Unicode.ESCAPE}[39;49m${Unicode.ESCAPE}[95;45m▒${Unicode.ESCAPE}[39;49m${Unicode.ESCAPE}[91;41m▒${Unicode.ESCAPE}[39;49m"

    @Test
    fun `should format HandyKommons`() {
        expectThat(Banner.banner("HandyKommons")).isEqualToJoinedWords(
            rainbow, "${Unicode.ESCAPE}[96mHANDY${Unicode.ESCAPE}[39m",
            "${Unicode.ESCAPE}[36mKOMMONS${Unicode.ESCAPE}[39m"
        )
    }

    @Test
    fun `should format camelCase`() {
        expectThat(Banner.banner("camelCase")).isEqualToJoinedWords(
            rainbow, "${Unicode.ESCAPE}[96mCAMEL${Unicode.ESCAPE}[39m",
            "${Unicode.ESCAPE}[36mCASE${Unicode.ESCAPE}[39m"
        )
    }

    @Test
    fun `should format PascalCase`() {
        expectThat(Banner.banner("PascalCase")).isEqualToJoinedWords(
            rainbow, "${Unicode.ESCAPE}[96mPASCAL${Unicode.ESCAPE}[39m",
            "${Unicode.ESCAPE}[36mCASE${Unicode.ESCAPE}[39m"
        )
    }

    @Test
    fun `should format any CaSe`() {
        expectThat(Banner.banner("any CaSe")).isEqualToJoinedWords(
            rainbow, "${Unicode.ESCAPE}[96mANY${Unicode.ESCAPE}[39m",
            "${Unicode.ESCAPE}[95mCASE${Unicode.ESCAPE}[39m"
        )
    }

    @Test
    fun `should format camelCamelCase`() {
        expectThat(Banner.banner("camelCamelCase")).isEqualToJoinedWords(
            rainbow, "${Unicode.ESCAPE}[96mCAMEL${Unicode.ESCAPE}[39m",
            "${Unicode.ESCAPE}[36mCAMELCASE${Unicode.ESCAPE}[39m"
        )
    }

    @Test
    fun `should format with custom prefix`() {
        expectThat(Banner.banner("camelCamelCase", "custom prefix")).isEqualToJoinedWords(
            "custom prefix",
            "${Unicode.ESCAPE}[96mCAMEL${Unicode.ESCAPE}[39m", "${Unicode.ESCAPE}[36mCAMELCASE${Unicode.ESCAPE}[39m"
        )
    }

    @Test
    fun `should format with no prefix`() {
        expectThat(Banner.banner("camelCamelCase", "")).isEqualToJoinedWords(
            "${Unicode.ESCAPE}[96mCAMEL${Unicode.ESCAPE}[39m",
            "${Unicode.ESCAPE}[36mCAMELCASE${Unicode.ESCAPE}[39m"
        )
    }
}

private fun Assertion.Builder<String>.isEqualToJoinedWords(vararg words: String) =
    isEqualTo(words.joinToString(" "))
