package koodies.io

import koodies.exception.dump
import koodies.io.path.notContainsLineSeparator
import koodies.io.path.prefixes
import koodies.logging.BlockRenderingLogger
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingReader
import koodies.process.SlowInputStream.Companion.slowInputStream
import koodies.test.HtmlFile
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.fuzzyLevenshteinDistance
import koodies.text.joinLinesToString
import koodies.text.repeat
import org.apache.commons.io.input.TeeInputStream
import org.apache.commons.io.output.ByteArrayOutputStream
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import java.io.InputStream
import java.io.Reader
import kotlin.time.Duration
import kotlin.time.seconds
import kotlin.time.toJavaDuration

@Disabled
abstract class SharedReaderTest(val readerFactory: BlockRenderingLogger.(InputStream, Duration) -> Reader) {

    @Slow
    @RepeatedTest(3)
    fun InMemoryLogger.`should not block`() {
        val slowInputStream = slowInputStream(1.seconds, "Hel", "lo\n", "World!\n")
        val reader = readerFactory(slowInputStream, 5.seconds)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
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
        val slowInputStream = slowInputStream(1.seconds, "𝌪𝌫𝌬𝌭𝌮", "𝌯𝌰\n", "𝌱𝌲𝌳𝌴𝌵\n")
        val reader = readerFactory(slowInputStream, .5.seconds)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
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
        val slowInputStream = slowInputStream(1.seconds, "Hel", "lo\n\n\n\n\n", "World!\n")
        val reader = readerFactory(slowInputStream, 5.seconds)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).all { notContainsLineSeparator() }
    }

    @Test
    fun InMemoryLogger.`should not repeat line on split CRLF`() {
        val slowInputStream = slowInputStream(1.seconds, "Hello\r", "\nWorld")
        val reader = readerFactory(slowInputStream, 5.seconds)

        val read: MutableList<String> = mutableListOf()
        assertTimeoutPreemptively(100.seconds.toJavaDuration()) {
            read.addAll(reader.readLines())
        }

        expectThat(read).containsExactly("Hello", "World")
    }

    @Suppress("unused")
    @Nested
    inner class Benchmark {
        private val expected = HtmlFile.text.repeat(50)

        @Test
        fun InMemoryLogger.`should quickly read boot sequence using custom forEachLine`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val reader = readerFactory(expected.byteInputStream(), 1.seconds)

            val read = mutableListOf<String>()
            kotlin.runCatching {
                assertTimeoutPreemptively(8.seconds.toJavaDuration()) {
                    reader.forEachLine {
                        read.add(it)
                    }
                }
            }.onFailure { dump("Test failed.") { read.joinLinesToString() } }

            expectThat(read.joinLinesToString()).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
        }

        @Test
        fun InMemoryLogger.`should quickly read boot sequence using foreign forEachLine`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val read = ByteArrayOutputStream()
            val reader = readerFactory(TeeInputStream(expected.byteInputStream(), read), 1.seconds)

            kotlin.runCatching {
                assertTimeoutPreemptively(8.seconds.toJavaDuration()) {
                    val readLines = reader.readLines()
                    expectThat(readLines.joinLinesToString()).fuzzyLevenshteinDistance(expected).isLessThanOrEqualTo(0.05)
                }
            }.onFailure { dump("Test failed.") { read.toString(Charsets.UTF_8) } }
        }
    }
}
