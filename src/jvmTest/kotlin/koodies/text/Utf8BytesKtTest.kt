package koodies.text

import koodies.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class Utf8BytesKtTest {
    @Test
    fun `should have same size as UTF-8 byte array`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat(string.utf8Bytes)
            .isEqualTo(string.toByteArray(Charsets.UTF_8).size.bytes)
            .isEqualTo(23.bytes)
    }
}
