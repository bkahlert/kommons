package koodies.nio

import koodies.exception.dump
import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.io.ByteArrayOutputStream
import koodies.io.TeeInputStream
import koodies.junit.UniqueId
import koodies.test.HtmlFixture
import koodies.test.Smoke
import koodies.test.notContainsLineSeparator
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.fuzzyLevenshteinDistance
import koodies.text.joinLinesToString
import koodies.time.seconds
import koodies.times
import koodies.unit.Mebi
import koodies.unit.bytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import java.io.Reader
import kotlin.time.Duration

class InputStreamReaderTest {

    @Test
    fun `should produce same byte sequence as ByteArrayInputStreamReader`() {
        val input = "A𝌪𝌫𝌬𝌭𝌮Z"
        val linesExpected = input.byteInputStream().reader(Charsets.UTF_8).readLines()
        val linesActual = InputStreamReader(input.byteInputStream()).readLines()
        expectThat(linesExpected).isEqualTo(linesActual)
    }

    @Test
    fun `should read no non-BEM unicode extremely slow input streams`() {
        val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
        val inputStream = slowInputStream(1.seconds, Duration.ZERO to input)
        val reader = InputStreamReader(inputStream)
        val readLines = reader.readLines()
        expectThat(readLines).isEqualTo(listOf("A\uD834\uDF2A", "\uD834\uDF2B", "\uD834\uDF2C\uD834\uDF2D\uD834\uDF2E", "Z"))
    }

    @Test
    fun `should read no non-BEM unicode extremely slow input streams if buffered`() {
        val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
        val inputStream = slowInputStream(1.seconds, Duration.ZERO to input)
        val reader = InputStreamReader(inputStream)
        val readLines = reader.readLines()
        expectThat(readLines).isEqualTo(listOf("A\uD834\uDF2A", "\uD834\uDF2B", "\uD834\uDF2C\uD834\uDF2D\uD834\uDF2E", "Z"))
    }

    @Smoke @Test
    fun `should be equally readable like any other byte input stream`() {
        val input = "A𝌪\n𝌫\n𝌬𝌭𝌮\nZ"
        val inputStream = slowInputStream(1.seconds, Duration.ZERO to input)

        val readFromSlowInput = InputStreamReader(inputStream).readAll()
        val readFromAnyInput = input.byteInputStream().readAllBytes().decodeToString()
        expectThat(readFromSlowInput).isEqualTo(readFromAnyInput).isEqualTo(input)
    }

    @Test
    fun `should never have trailing line separators`() {
        val slowInputStream = slowInputStream(1.seconds,
            "Hel",
            "lo${LineSeparators.LF}${LineSeparators.LF}${LineSeparators.LF}${LineSeparators.LF}${LineSeparators.LF}",
            "World!${LineSeparators.LF}")
        val reader = InputStreamReader(slowInputStream)

        val read = reader.readLines()

        expectThat(read).all { notContainsLineSeparator() }
    }

    @Test
    fun `should not repeat line on split CRLF`() {
        val slowInputStream = slowInputStream(1.seconds, "Hello${LineSeparators.CR}", "${LineSeparators.LF}World")
        val reader = InputStreamReader(slowInputStream)

        val read = reader.readLines()

        expectThat(read).containsExactly("Hello", "World")
    }

    @Suppress("unused")
    @Isolated
    @Nested
    inner class Benchmark {
        private val size = 1.Mebi.bytes
        private val input = HtmlFixture.text
        private val expected: String =
            StringBuilder().apply { (size / input.length).wholeBytes.toInt() * { append(input);append(LineSeparators.LF) } }.toString()

        @Test
        fun `should quickly read`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val read = ByteArrayOutputStream()
            val reader = InputStreamReader(TeeInputStream(expected.byteInputStream(), read))

            kotlin.runCatching {
                val readLines = reader.readLines()
                expectThat(readLines.joinLinesToString()).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
            }.onFailure { dump("Test failed.") { read.toString(Charsets.UTF_8) } }
        }
    }

    private fun Reader.readAll(): String {
        val buffer = StringBuilder()
        var read: Int
        while (read().also { read = it } > -1) {
            buffer.append(read.toChar())
        }
        buffer.toString()
        return buffer.toString()
    }
}
