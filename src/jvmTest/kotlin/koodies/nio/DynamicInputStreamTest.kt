package koodies.nio

import koodies.jvm.thread
import koodies.time.seconds
import koodies.time.sleep
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import kotlin.time.Duration
import kotlin.time.measureTime

class DynamicInputStreamTest {

    @Test
    fun `should be readable`() {
        val inputStream = DynamicInputStream()
        inputStream.yield("Hello World!".toByteArray())
        inputStream.close()

        val bytes = inputStream.readBytes()

        expectThat(bytes).isEqualTo("Hello World!".toByteArray())
    }

    @Test
    fun `should be readable as reader`() {
        val inputStream = DynamicInputStream()
        inputStream.yield("Hello World!\nGoodbye Trump.".toByteArray())
        inputStream.close()

        val lines = inputStream.reader().readLines()

        expectThat(lines).containsExactly("Hello World!", "Goodbye Trump.")
    }

    @Test
    fun `should read as long as stream is not closed`() {
        val inputStream = DynamicInputStream()
        inputStream.yield("Hello World!".toByteArray())
        thread { 2.1.seconds.sleep { inputStream.close() } }

        var bytes: ByteArray
        val duration: Duration = measureTime {
            bytes = inputStream.readBytes()
        }

        expect {
            that(duration).isGreaterThanOrEqualTo(2.seconds)
            that(bytes).isEqualTo("Hello World!".toByteArray())
        }
    }

    @Test
    fun `should provide available byte count`() {
        val inputStream = DynamicInputStream()
        inputStream.yield("Hello World!".toByteArray())

        expectThat(inputStream.available()).isEqualTo(12)
    }
}
