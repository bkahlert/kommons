package koodies.io

import koodies.exception.dump
import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.junit.UniqueId
import koodies.nio.NonBlockingReader
import koodies.test.HtmlFixture
import koodies.test.Slow
import koodies.test.notContainsLineSeparator
import koodies.test.prefixes
import koodies.test.withTempDir
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.LF
import koodies.text.fuzzyLevenshteinDistance
import koodies.time.seconds
import koodies.times
import koodies.unit.bytes
import koodies.unit.kilo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import java.io.InputStream
import java.io.Reader
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration
import kotlin.time.toJavaDuration

@Disabled
abstract class SharedReaderTest(val readerFactory: (InputStream, Duration) -> Reader) {

    @Slow @Test
    fun `should not block`() {
        val slowInputStream = slowInputStream(1.seconds, "Hel", "lo$LF", "World!$LF")
        val reader = readerFactory(slowInputStream, 5.seconds)

        val read: MutableList<String> = mutableListOf()
        while (read.lastOrNull() != "World!") {
            val readLine = (reader as? NonBlockingReader)?.readLine() ?: break
            read.add(readLine)
        }

        // e.g. [, , , , Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hello, , , , , , , , , , World, World!]
        expectThat(read
            .takeWhile { it != "Hello" }
        ).all { prefixes("Hello") }
        expectThat(read
            .dropWhile { it != "Hello" }
            .filter { it.isNotBlank() }
            .takeWhile { it == "Hello" }
        ).all { isEqualTo("Hello") }
        expectThat(read
            .dropWhile { it != "Hello" }
            .filter { it.isNotBlank() }
            .dropWhile { it == "Hello" }
            .filter { it.isNotBlank() }
        ).all { prefixes("World!") }
    }

    @Slow @Test
    fun `should read characters that are represented by two chars`() {
        val slowInputStream = slowInputStream(1.seconds, "𝌪𝌫𝌬𝌭𝌮", "𝌯𝌰$LF", "𝌱𝌲𝌳𝌴𝌵$LF")
        val reader = readerFactory(slowInputStream, 0.5.seconds)

        val read: MutableList<String> = mutableListOf()
        while (read.lastOrNull() != "𝌱𝌲𝌳𝌴𝌵") {
            val readLine = (reader as? NonBlockingReader)?.readLine() ?: break
            read.add(readLine)
        }

        // e.g. [, , , , Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hel, Hello, , , , , , , , , , World, World!]
        expectThat(read
            .takeWhile { it != "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
        ).all { prefixes("𝌯𝌰") }
        expectThat(read
            .dropWhile { it != "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
            .filter { it.isNotBlank() }
            .takeWhile { it == "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
        ).all { isEqualTo("𝌪𝌫𝌬𝌭𝌮𝌯𝌰") }
        expectThat(read
            .dropWhile { it != "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
            .filter { it.isNotBlank() }
            .dropWhile { it == "𝌪𝌫𝌬𝌭𝌮𝌯𝌰" }
            .filter { it.isNotBlank() }
        ).all { prefixes("𝌱𝌲𝌳𝌴𝌵") }
        expectThat("𝌱𝌲𝌳𝌴𝌵")
    }

    @Test
    fun `should never have trailing line separators`() {
        val slowInputStream = slowInputStream(1.seconds, "Hel", "lo$LF$LF$LF$LF$LF", "World!$LF")
        val reader = readerFactory(slowInputStream, 5.seconds)

        val read = reader.readLines()

        expectThat(read).all { notContainsLineSeparator() }
    }

    @Test
    fun `should not repeat line on split CRLF`() {
        val slowInputStream = slowInputStream(1.seconds, "Hello$CR", "${LF}World")
        val reader = readerFactory(slowInputStream, 5.seconds)

        val read = reader.readLines()

        expectThat(read).containsExactly("Hello", "World")
    }

    @Suppress("unused")
    @Isolated
    @Nested
    inner class Benchmark {
        private val size = 5.kilo.bytes
        private val input = HtmlFixture.text
        private val expected: String = StringBuilder().apply { (size / input.length).wholeBytes.toInt() * { append(input);append(LF) } }.toString()

        @Test @Timeout(15, unit = SECONDS)
        fun `should quickly read`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val read = ByteArrayOutputStream()
            val reader = readerFactory(TeeInputStream(expected.byteInputStream(), read), 1.seconds)

            kotlin.runCatching {
                assertTimeoutPreemptively(8.seconds.toJavaDuration()) {
                    val readLines = reader.readLines()
                    expectThat(readLines.joinToString(LF)).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
                }
            }.onFailure { dump("Test failed.") { read.toString(Charsets.UTF_8) } }
        }
    }
}
