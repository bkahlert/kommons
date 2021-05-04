package koodies.text

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class AsStringKtTest {

    @Test
    fun `should produce same byte string as toString`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat("${StringBuilder(string)}").isEqualTo(string)
    }
}
