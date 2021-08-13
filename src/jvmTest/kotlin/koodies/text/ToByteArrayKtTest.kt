package koodies.text

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ToByteArrayKtTest {

    @Test
    fun `should produce same byte array as string with default encoding`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat(StringBuilder(string).toByteArray()).isEqualTo(string.toByteArray())
    }

    @Test
    fun `should produce same byte array as string with explicit encoding`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat(StringBuilder(string).toByteArray(Charsets.UTF_16LE)).isEqualTo(string.toByteArray(Charsets.UTF_16LE))
    }
}
