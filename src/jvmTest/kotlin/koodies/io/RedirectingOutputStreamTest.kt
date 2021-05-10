package koodies.io

import koodies.test.TextFile
import koodies.test.expecting
import koodies.text.toStringMatchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class RedirectingOutputStreamTest {

    @Test
    fun `should redirect`() {
        val text = TextFile.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString()).isEqualTo(text)
    }

    @Test
    fun `should redirect non latin`() {
        val text = TextFile.text
        val captured = mutableListOf<ByteArray>()
        text.byteInputStream().copyTo(RedirectingOutputStream { captured.add(it) })
        expectThat(captured.joinToString()).isEqualTo(text)
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
