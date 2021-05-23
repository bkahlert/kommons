package koodies.io

import koodies.decodeToString
import koodies.test.TextFixture
import koodies.test.expecting
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class RedirectingOutputStreamTest {

    @Test
    fun `should redirect`() {
        val text = TextFixture.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.decodeToString()).isEqualTo(text)
    }

    @Test
    fun `should redirect non latin`() {
        val text = TextFixture.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.decodeToString()).isEqualTo(text)
    }

    @Test
    fun `should override toString`() {
        val redirection = object : (ByteArray) -> Unit {
            override operator fun invoke(byteArray: ByteArray): Unit = Unit
            override fun toString(): String = "sample"
        }
        expecting { RedirectingOutputStream(redirection) } that {
            toStringMatchesCurlyPattern("RedirectingOutputStream {} redirection = sample {}")
        }
    }
}
