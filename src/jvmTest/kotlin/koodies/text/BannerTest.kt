package koodies.text

import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import koodies.text.Unicode.escape as e

class BannerTest {

    private val rainbow =
        "$e[90;40m░$e[39;49m$e[96;46m░$e[39;49m$e[94;44m░$e[39;49m$e[92;42m░$e[39;49m$e[93;43m░$e[39;49m$e[95;45m░$e[39;49m$e[91;41m░$e[39;49m"

    @Test
    fun `should format HandyKoodies`() {
        expectThat(Banner.banner("HandyKoodies")).isEqualToJoinedWords(rainbow, "$e[96mHANDY$e[39m", "$e[36mKOODIES$e[39m")
    }

    @Test fun `should format camelCase`() {
        expectThat(Banner.banner("camelCase")).isEqualToJoinedWords(rainbow, "$e[96mCAMEL$e[39m", "$e[36mCASE$e[39m")
    }

    @Test fun `should format PascalCase`() {
        expectThat(Banner.banner("PascalCase")).isEqualToJoinedWords(rainbow, "$e[96mPASCAL$e[39m", "$e[36mCASE$e[39m")
    }

    @Test fun `should format any CaSe`() {
        expectThat(Banner.banner("any CaSe")).isEqualToJoinedWords(rainbow, "$e[96mANY$e[39m", "$e[95mCASE$e[39m")
    }

    @Test fun `should format camelCamelCase`() {
        expectThat(Banner.banner("camelCamelCase")).isEqualToJoinedWords(rainbow, "$e[96mCAMEL$e[39m", "$e[36mCAMELCASE$e[39m")
    }
}

private fun Assertion.Builder<String>.isEqualToJoinedWords(vararg words: String) =
    isEqualTo(words.joinToString(" "))
