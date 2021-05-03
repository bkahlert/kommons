package koodies.io

import koodies.nio.MemoryReclaimableByteArrayOutputStream
import koodies.number.times
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan


class MemoryReclaimableByteArrayIOStreamTest {
    @Test
    fun `should support accessing bytes by index`() {
        val outputStream = MemoryReclaimableByteArrayOutputStream().apply { write(2); write(1); write(0) }
        expectThat(outputStream[0]).isEqualTo(2)
        expectThat(outputStream[1]).isEqualTo(1)
        expectThat(outputStream[2]).isEqualTo(0)
    }

    @Test
    fun `should provide remaining space`() {
        val outputStream = MemoryReclaimableByteArrayOutputStream(1024).apply { write(ByteArray(1000)) }
        expectThat(outputStream.remaining).isEqualTo(24)
    }

    @Test
    fun `should grow space`() {
        val outputStream = MemoryReclaimableByteArrayOutputStream(1024)
        outputStream.grow(128)
        expectThat(outputStream.remaining).isGreaterThan(1150)
    }

    @Test
    fun `should grow automatically`() {
        val outputStream = MemoryReclaimableByteArrayOutputStream(1024)
        outputStream.write(ByteArray(1500))
        expectThat(outputStream.remaining).isGreaterThan(500)
    }

    @Test
    fun `should reclaim memory`() {
        val outputStream = MemoryReclaimableByteArrayOutputStream(1024).apply {
            write(ByteArray(1000))
            24 times { write(1) }
        }

        outputStream.drop(999)

        expectThat(outputStream[0]).isEqualTo(0)
        (1 until 24).forEach { i -> expectThat(outputStream).get { this[i] }.isEqualTo(1) }
    }
}
