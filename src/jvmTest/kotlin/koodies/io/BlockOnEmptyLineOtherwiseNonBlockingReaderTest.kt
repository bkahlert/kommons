package koodies.io

import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.nio.NonBlockingReader
import koodies.test.Slow
import koodies.text.LineSeparators.LF
import koodies.time.seconds
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isBlank
import strikt.assertions.none
import java.io.InputStream
import kotlin.time.Duration

@Execution(CONCURRENT)
class BlockOnEmptyLineOtherwiseNonBlockingReaderTest :
    SharedReaderTest({ inputStream: InputStream, timeout: Duration ->
        NonBlockingReader(inputStream = inputStream, timeout = timeout, blockOnEmptyLine = true)
    }) {

    @Slow @Test
    fun `should not read empty lines due to timeout`() {
        val reader = readerFactory(object : InputStream() {
            override fun read(): Int {
                5.seconds.sleep()
                return -1
            }
        }, 2.seconds)

        val read = reader.readLines()

        expectThat(read).none { isBlank() }
    }

    @Nested
    inner class TimedOut {

        @Test
        fun `should read full line if delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "Foo$LF",
            )

            expectThat(readerFactory(slowInputStream, 1.seconds).readLines()).containsExactly("Foo")
        }

        @Test
        fun `should read full line if second half delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "F",
                0.5.seconds to "oo$LF",
            )

            expectThat(readerFactory(slowInputStream, 1.seconds).readLines()).containsExactly("Foo")
        }

        @Test
        fun `should read full line if split`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "Foo\nB",
                0.5.seconds to "ar$LF",
            )

            expectThat(readerFactory(slowInputStream, 1.seconds).readLines()).containsExactly("Foo", "Bar")
        }

        @Test
        fun `should read full line if delayed split`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "Foo\nB",
                1.5.seconds to "ar$LF",
            )

            expectThat(readerFactory(slowInputStream, 1.seconds).readLines()).containsExactly("Foo", "B", "Bar")
        }
    }
}
