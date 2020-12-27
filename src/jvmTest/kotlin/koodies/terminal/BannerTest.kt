package koodies.terminal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class BannerTest {
    
    @Test
    fun `should format ImgCstmzr`() {
        expectThat(Banner.banner("ImgCstmzr")).isEqualTo("\u001B[40;90m░\u001B[49;39m\u001B[46;96m░\u001B[49;39m\u001B[44;94m░\u001B[49;39m\u001B[42;92m░\u001B[49;39m\u001B[43;93m░\u001B[49;39m\u001B[45;95m░\u001B[49;39m\u001B[41;91m░\u001B[49;39m \u001B[96mIMG\u001B[39m \u001B[36mCSTMZR\u001B[39m")
    }

    @Test
    fun `should format camelCase`() {
        expectThat(Banner.banner("camelCase")).isEqualTo("\u001B[40;90m░\u001B[49;39m\u001B[46;96m░\u001B[49;39m\u001B[44;94m░\u001B[49;39m\u001B[42;92m░\u001B[49;39m\u001B[43;93m░\u001B[49;39m\u001B[45;95m░\u001B[49;39m\u001B[41;91m░\u001B[49;39m \u001B[96mCAMEL\u001B[39m \u001B[36mCASE\u001B[39m")
    }

    @Test
    fun `should format PascalCase`() {
        expectThat(Banner.banner("PascalCase")).isEqualTo("\u001B[40;90m░\u001B[49;39m\u001B[46;96m░\u001B[49;39m\u001B[44;94m░\u001B[49;39m\u001B[42;92m░\u001B[49;39m\u001B[43;93m░\u001B[49;39m\u001B[45;95m░\u001B[49;39m\u001B[41;91m░\u001B[49;39m \u001B[96mPASCAL\u001B[39m \u001B[36mCASE\u001B[39m")
    }

    @Test
    fun `should format any CaSe`() {
        expectThat(Banner.banner("any CaSe")).isEqualTo("\u001B[40;90m░\u001B[49;39m\u001B[46;96m░\u001B[49;39m\u001B[44;94m░\u001B[49;39m\u001B[42;92m░\u001B[49;39m\u001B[43;93m░\u001B[49;39m\u001B[45;95m░\u001B[49;39m\u001B[41;91m░\u001B[49;39m \u001B[96mANY\u001B[39m \u001B[95mCASE\u001B[39m")
    }

    @Test
    fun `should format camelCamelCase`() {
        expectThat(Banner.banner("camelCamelCase")).isEqualTo("\u001B[40;90m░\u001B[49;39m\u001B[46;96m░\u001B[49;39m\u001B[44;94m░\u001B[49;39m\u001B[42;92m░\u001B[49;39m\u001B[43;93m░\u001B[49;39m\u001B[45;95m░\u001B[49;39m\u001B[41;91m░\u001B[49;39m \u001B[96mCAMEL\u001B[39m \u001B[36mCAMELCASE\u001B[39m")
    }
}
