package koodies.nio

import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isTrue
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException

class DynamicReadableByteChannelTest {

    private fun buffer(capacity: Int) = ByteBuffer.allocate(capacity)

    @Test
    fun `should be open by default`() {
        val channel = DynamicReadableByteChannel()
        expectThat(channel.isOpen).isTrue()
    }

    @Test
    fun `should read zero bytes by default`() {
        val channel = DynamicReadableByteChannel()
        val buffer = buffer(10)
        val read = channel.read(buffer)
        expect {
            that(read).isEqualTo(0)
            that(buffer.position()).isEqualTo(0)
        }
    }

    @Test
    fun `should fill buffer if enough data`() {
        val channel = DynamicReadableByteChannel()
        channel.yield("Hello World!".toByteArray())

        val buffer = buffer(10)
        val read = channel.read(buffer)
        expect {
            that(read).isEqualTo(10)
            that(buffer.position()).isEqualTo(10)
            that(buffer.bytes).isEqualTo("Hello Worl".toByteArray())
            that(channel.available).isEqualTo(2)
            that(channel.bytes).isEqualTo("d!".toByteArray())
        }
    }

    @Test
    fun `should deplete if buffer big enough`() {
        val channel = DynamicReadableByteChannel()
        channel.yield("Hello World!".toByteArray())

        val buffer = buffer(20)
        val read = channel.read(buffer)
        expect {
            that(read).isEqualTo(12)
            that(buffer.position()).isEqualTo(12)
            that(buffer.bytes).isEqualTo("Hello World!".toByteArray())
            that(channel.available).isEqualTo(0)
            that(channel.bytes).isEmpty()
        }
    }

    @Test
    fun `should yield new data`() {
        val channel = DynamicReadableByteChannel()
        channel.yield("Hello World!".toByteArray())

        val buffer = buffer(20)
        channel.read(buffer)

        expectThat(channel.isOpen)
        expectThat(channel.read(buffer)).isEqualTo(0)

        channel.yield(" delayed".toByteArray())
        expectThat(channel.read(buffer)).isEqualTo(8)
        expectThat(buffer.bytes).isEqualTo("Hello World! delayed".toByteArray())
    }

    @Test
    fun `should throw on yielding if empty channel closed`() {
        val channel = DynamicReadableByteChannel()
        channel.close()
        expectCatching { channel.yield(ByteArray(0)) }.isFailure().isA<ClosedChannelException>()
    }

    @Test
    fun `should throw on yielding if non-empty channel closed`() {
        val channel = DynamicReadableByteChannel()
        channel.yield("Hello World!".toByteArray())
        channel.close()
        expectCatching { channel.yield(ByteArray(0)) }.isFailure().isA<ClosedChannelException>()
    }

    @Test
    fun `should allow reading all data`() {
        val channel = DynamicReadableByteChannel()
        channel.yield("Hello World!".toByteArray())
        channel.close()
        expectThat(channel.read(buffer(20))).isEqualTo(12)
    }

    @Test
    fun `should return -1 on reading effectively closed channel`() {
        val channel = DynamicReadableByteChannel()
        channel.yield("Hello World!".toByteArray())
        channel.close()
        channel.read(buffer(20))
        expectThat(channel.read(buffer(20))).isEqualTo(-1)
    }
}
