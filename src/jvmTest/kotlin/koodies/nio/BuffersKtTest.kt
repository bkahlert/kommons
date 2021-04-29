package koodies.nio

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer

class BuffersKtTest {

    @Nested
    inner class Bytes {

        @Test
        fun `should read payload`() {
            val buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
            buffer.put("42".toByteArray())
            expectThat(buffer.bytes).isEqualTo("42".toByteArray())
        }

        @Test
        fun `should throw on non-ByteArray backed buffer`() {
            val buffer = MappedByteBuffer.allocateDirect(DEFAULT_BUFFER_SIZE)
            buffer.put("42".toByteArray())
            expectCatching { buffer.bytes }.isFailure().isA<IllegalStateException>()
        }
    }
}
