package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class BannerTest {

    // @formatter:off
    @Test fun `should format ImgCstmzr`() { expectThat(Banner.banner("ImgCstmzr")).isEqualTo("\u001B[90;40m░\u001B[39;49m\u001B[96;46m░\u001B[39;49m\u001B[94;44m░\u001B[39;49m\u001B[92;42m░\u001B[39;49m\u001B[93;43m░\u001B[39;49m\u001B[95;45m░\u001B[39;49m\u001B[91;41m░\u001B[39;49m \u001B[96mIMG\u001B[39m \u001B[36mCSTMZR\u001B[39m") }
    @Test fun `should format camelCase`() { expectThat(Banner.banner("camelCase")).isEqualTo("\u001B[90;40m░\u001B[39;49m\u001B[96;46m░\u001B[39;49m\u001B[94;44m░\u001B[39;49m\u001B[92;42m░\u001B[39;49m\u001B[93;43m░\u001B[39;49m\u001B[95;45m░\u001B[39;49m\u001B[91;41m░\u001B[39;49m \u001B[96mCAMEL\u001B[39m \u001B[36mCASE\u001B[39m") }
    @Test fun `should format PascalCase`() { expectThat(Banner.banner("PascalCase")).isEqualTo("\u001B[90;40m░\u001B[39;49m\u001B[96;46m░\u001B[39;49m\u001B[94;44m░\u001B[39;49m\u001B[92;42m░\u001B[39;49m\u001B[93;43m░\u001B[39;49m\u001B[95;45m░\u001B[39;49m\u001B[91;41m░\u001B[39;49m \u001B[96mPASCAL\u001B[39m \u001B[36mCASE\u001B[39m") }
    @Test fun `should format any CaSe`() { expectThat(Banner.banner("any CaSe")).isEqualTo("\u001B[90;40m░\u001B[39;49m\u001B[96;46m░\u001B[39;49m\u001B[94;44m░\u001B[39;49m\u001B[92;42m░\u001B[39;49m\u001B[93;43m░\u001B[39;49m\u001B[95;45m░\u001B[39;49m\u001B[91;41m░\u001B[39;49m \u001B[96mANY\u001B[39m \u001B[95mCASE\u001B[39m") }
    @Test fun `should format camelCamelCase`() { expectThat(Banner.banner("camelCamelCase")).isEqualTo("\u001B[90;40m░\u001B[39;49m\u001B[96;46m░\u001B[39;49m\u001B[94;44m░\u001B[39;49m\u001B[92;42m░\u001B[39;49m\u001B[93;43m░\u001B[39;49m\u001B[95;45m░\u001B[39;49m\u001B[91;41m░\u001B[39;49m \u001B[96mCAMEL\u001B[39m \u001B[36mCAMELCASE\u001B[39m") }
    // @formatter:on

}
