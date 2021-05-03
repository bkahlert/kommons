package koodies.io

import koodies.test.TextFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
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
}
