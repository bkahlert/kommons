package koodies.io

import koodies.test.HtmlFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class JoinToStringKtTest {
    @Test
    fun `should join byte arrays to string`() {
        val buffer = ByteArray(1024)
        var read: Int
        val byteArrays = mutableListOf<ByteArray>()
        val inputStream = HtmlFile.data.inputStream()
        while (inputStream.read(buffer).also { read = it } > 0) byteArrays.add(buffer.copyOfRange(0, read))

        expectThat(byteArrays.joinToString()).isEqualTo(HtmlFile.text)
    }
}
