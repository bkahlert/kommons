package koodies.io

import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.test.Slow
import koodies.test.Smoke
import koodies.text.randomString
import koodies.time.seconds
import org.jline.utils.InputStreamReader
import org.jline.utils.NonBlocking
import org.jline.utils.NonBlockingReader
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.Reader
import kotlin.time.Duration

/**
 * Tests mainly JLine functionality / findings / working patterns.
 */
@Execution(CONCURRENT)
class NonBlockingTest {

    @Nested
    inner class NonBlockingInputStream {

        @Test
        fun `should produce same byte sequence as ByteArrayInputStream`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val nonBlockingInputStream =
                NonBlocking.nonBlocking(::`should produce same byte sequence as ByteArrayInputStream`.toString(), input.byteInputStream())
            expectThat(input.byteInputStream().readAllBytes()).isEqualTo(nonBlockingInputStream.readAllBytes())
        }
    }

    @Nested
    inner class NonBlockingInputStreamReader {

        @Test
        fun `should produce same byte sequence as ByteArrayInputStreamReader`() {
            val input = "A𝌪𝌫𝌬𝌭𝌮Z"
            val linesExpected = input.byteInputStream().reader(Charsets.UTF_8).readLines()
            val linesActual = NonBlocking.nonBlocking(::`should produce same byte sequence as ByteArrayInputStreamReader`.toString(),
                input.byteInputStream(),
                Charsets.UTF_8).readLines()
            expectThat(linesExpected).isEqualTo(linesActual)
        }

        @Slow @Test
        fun `should read no non-BEM unicode extremely slow input streams`() {
            val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
            val inputStream = slowInputStream(1.seconds, Duration.ZERO to input)
            val reader = NonBlocking.nonBlocking(randomString(), NonBlocking.nonBlocking(randomString(), BufferedInputStream(inputStream)), Charsets.UTF_8)
            val readLines = reader.readLines()
            expectThat(readLines).isEqualTo(listOf("A\uD834\uDF2A", "\uD834\uDF2B", "\uD834\uDF2C\uD834\uDF2D\uD834\uDF2E", "Z"))
        }

        @Test
        fun `should read no non-BEM unicode extremely slow input streams if buffered`() {
            val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
            val inputStream = slowInputStream(1.seconds, Duration.ZERO to input)
            val reader =
                NonBlocking.nonBlocking("should read no non-BEM unicode extremely slow input streams if buffered",
                    BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)))
            val readLines = reader.readLines()
            expectThat(readLines).isEqualTo(listOf("A\uD834\uDF2A", "\uD834\uDF2B", "\uD834\uDF2C\uD834\uDF2D\uD834\uDF2E", "Z"))
        }

        @Smoke @Test
        fun `should be equally readable like any other byte input stream`() {
            val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
            val inputStream = slowInputStream(1.seconds, Duration.ZERO to input)

            val readFromSlowInput =
                NonBlocking.nonBlocking(randomString(), BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))).readAll(8.seconds)
            val readFromAnyInput = input.byteInputStream().readAllBytes().decodeToString()
            expectThat(readFromSlowInput).isEqualTo(readFromAnyInput).isEqualTo(input)
        }
    }

    fun Reader.read(timeout: Duration = 6.seconds): Int = if (this is NonBlockingReader) this.read(timeout.inWholeMilliseconds) else this.read()

    fun Reader.readAll(timeout: Duration = 6.seconds): String {
        val buffer = StringBuilder()
        kotlin.runCatching {
            var read: Int
            while (read(timeout).also { read = it } > -1) {
                buffer.append(read.toChar())
            }
            buffer.toString()
        }.getOrThrow()
        return buffer.toString()
    }
}
