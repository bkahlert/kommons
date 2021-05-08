package koodies.io

import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.test.Slow
import koodies.test.Smoke
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
import kotlin.time.toJavaDuration

@Execution(CONCURRENT)
class NonBlockingReaderTest : SharedReaderTest({ inputStream: InputStream, timeout: Duration ->
    NonBlockingReader(inputStream = inputStream, timeout = timeout, logger = this, blockOnEmptyLine = false)
}) {

    @Slow
    @Nested
    inner class TimedOut {

        @Test
        fun InMemoryLogger.`should read full line if delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                Duration.seconds(1.5) to "Foo$LF",
            )

            expectThat(read(slowInputStream)).containsExactly("", "Foo")
        }

        @Test
        fun InMemoryLogger.`should read full line if second half delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                Duration.seconds(1.5) to "F",
                Duration.seconds(0.5) to "oo$LF",
            )

            expectThat(read(slowInputStream)).containsExactly("", "Foo")
        }

        @Test
        fun InMemoryLogger.`should read full line if split`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                Duration.seconds(1.5) to "Foo\nB",
                Duration.seconds(0.5) to "ar$LF",
            )

            expectThat(read(slowInputStream)).containsExactly("", "Foo", "Bar")
        }

        @Smoke @Test
        fun InMemoryLogger.`should read full line if split delayed`() {
            val slowInputStream = slowInputStream(
                Duration.ZERO,
                Duration.seconds(1.5) to "Foo\nB",
                Duration.seconds(1.5) to "ar$LF",
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
