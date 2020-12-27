package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class Utf8KtTest {
    @Test
    fun `should produce same byte array as UTF-8 byte array`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat(string.utf8).isEqualTo(string.toByteArray(Charsets.UTF_8))
    }
}
