package koodies.io

import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@Execution(CONCURRENT)
class NonBlockingReaderTest : SharedReaderTest({ inputStream: InputStream, timeout: Duration ->
    NonBlockingReader(inputStream = inputStream, timeout = timeout, logger = this, blockOnEmptyLine = false)
}) {

    @Nested
    inner class TimedOut {

        @Test
        fun InMemoryLogger.`should read full line if delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "Foo$LF",
            )

            expectThat(read(slowInputStream)).containsExactly("", "Foo")
        }

        @Test
        fun InMemoryLogger.`should read full line if second half delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "F",
                0.5.seconds to "oo$LF",
            )

            expectThat(read(slowInputStream)).containsExactly("", "Foo")
        }

        @Test
        fun InMemoryLogger.`should read full line if split`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "Foo\nB",
                0.5.seconds to "ar$LF",
            )

            expectThat(read(slowInputStream)).containsExactly("", "Foo", "Bar")
        }

        @Test
        fun InMemoryLogger.`should read full line if split delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                1.5.seconds to "Foo\nB",
                1.5.seconds to "ar$LF",
            )

            expectThat(read(slowInputStream)).containsExactly("", "Foo", "B", "Bar")
        }

        private fun InMemoryLogger.read(slowInputStream: InputStream): List<String> {
            val reader = readerFactory(slowInputStream, Duration.seconds(1))

            val read: MutableList<String> = mutableListOf()
            assertTimeoutPreemptively(Duration.seconds(100).toJavaDuration()) {
                read.addAll(reader.readLines())
            }
            return read
        }
    }
}
