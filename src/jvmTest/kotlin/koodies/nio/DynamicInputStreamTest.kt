package koodies.nio

import koodies.concurrent.thread
import koodies.time.sleep
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import kotlin.time.measureTime
import kotlin.time.seconds

@Execution(SAME_THREAD)
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
        thread { 2.seconds.sleep { inputStream.close() } }

        var bytes: ByteArray
        val duration = measureTime {
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
