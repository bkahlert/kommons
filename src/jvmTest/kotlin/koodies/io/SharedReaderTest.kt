package koodies.io

import koodies.exception.dump
import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.io.path.notContainsLineSeparator
import koodies.io.path.prefixes
import koodies.logging.BlockRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.test.HtmlFile
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.LF
import koodies.text.fuzzyLevenshteinDistance
import koodies.times
import koodies.unit.bytes
import koodies.unit.kilo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import java.io.InputStream
import java.io.Reader
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Disabled
abstract class SharedReaderTest(val readerFactory: BlockRenderingLogger.(InputStream, Duration) -> Reader) {

    @Slow
    @RepeatedTest(3)
    fun InMemoryLogger.`should not block`() {
        val slowInputStream = slowInputStream(seconds(1), "Hel", "lo$LF", "World!$LF")
        val reader = readerFactory(slowInputStream, seconds(5))

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(seconds(100).toJavaDuration()) {
            while (read.lastOrNull() != "World!") {
                val readLine = (reader as? NonBlockingReader)?.readLine() ?: return@assertTimeoutPreemptively
                read.add(readLine)
            }
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

    @Slow
    @RepeatedTest(3)
    fun InMemoryLogger.`should read characters that are represented by two chars`() {
        val slowInputStream = slowInputStream(seconds(1), "𝌪𝌫𝌬𝌭𝌮", "𝌯𝌰$LF", "𝌱𝌲𝌳𝌴𝌵$LF")
        val reader = readerFactory(slowInputStream, seconds(.5))

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(seconds(100).toJavaDuration()) {
            while (read.lastOrNull() != "𝌱𝌲𝌳𝌴𝌵") {
                val readLine = (reader as? NonBlockingReader)?.readLine() ?: return@assertTimeoutPreemptively
                read.add(readLine)
            }
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
    fun InMemoryLogger.`should never have trailing line separators`() {
        val slowInputStream = slowInputStream(seconds(1), "Hel", "lo$LF$LF$LF$LF$LF", "World!$LF")
        val reader = readerFactory(slowInputStream, seconds(5))

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(seconds(100).toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).all { notContainsLineSeparator() }
    }

    @Test
    fun InMemoryLogger.`should not repeat line on split CRLF`() {
        val slowInputStream = slowInputStream(seconds(1), "Hello$CR", "${LF}World")
        val reader = readerFactory(slowInputStream, seconds(5))

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(seconds(100).toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).containsExactly("Hello", "World")
    }

    @Suppress("unused")
    @Isolated
    @Nested
    inner class Benchmark {
        private val size = 10.kilo.bytes
        private val input = HtmlFile.text
        private val expected: String = StringBuilder().apply { (size / input.length).wholeBytes.toInt() * { append(input);append(LF) } }.toString()

        @Slow @Test
        fun InMemoryLogger.`should quickly read boot sequence using custom forEachLine`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val reader = readerFactory(expected.byteInputStream(), seconds(1))

            val read = mutableListOf<String>()
            kotlin.runCatching {
                assertTimeoutPreemptively(seconds(8).toJavaDuration()) {
                    reader.forEachLine {
                        read.add(it)
                    }
                }
            }.onFailure {
                dump("Test failed.") { read.joinToString(LF) }
            }

            expectThat(read.joinToString(LF)).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
        }

        @Slow @Test
        fun InMemoryLogger.`should quickly read boot sequence using foreign forEachLine`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val read = ByteArrayOutputStream()
            val reader = readerFactory(TeeInputStream(expected.byteInputStream(), read), seconds(1))

            kotlin.runCatching {
                assertTimeoutPreemptively(seconds(8).toJavaDuration()) {
                    val readLines = reader.readLines()
                    expectThat(readLines.joinToString(LF)).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
                }
            }.onFailure { dump("Test failed.") { read.toString(Charsets.UTF_8) } }
        }
    }
}
