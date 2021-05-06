package koodies.io

import koodies.exec.mock.SlowInputStream.Companion.slowInputStream
import koodies.logging.InMemoryLogger
import koodies.nio.NonBlockingCharReader
import koodies.number.times
import koodies.test.Slow
import koodies.text.ANSI
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.styling.Borders
import koodies.text.styling.wrapWithBorder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.opentest4j.AssertionFailedError
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo
import java.io.InputStreamReader
import kotlin.time.Duration

@Execution(CONCURRENT)
class NonBlockingCharReaderTest {

    @Test
    fun InMemoryLogger.`should read null if empty`() {
        val reader = NonBlockingCharReader("".byteInputStream(), Duration.milliseconds(100))
        5 times { expectThat(reader.read(CharArray(1), 0, this)).isLessThanOrEqualTo(0) }
        10 times { expectThat(reader.read(CharArray(1), 0, this)).isEqualTo(-1) }
    }

    @Test
    fun InMemoryLogger.`should return null if source is closed`() {
        val reader = NonBlockingCharReader("123".byteInputStream(), Duration.milliseconds(100))
        expectThat(reader.readText()).isEqualTo("123")
        5 times { expectThat(reader.read(CharArray(1), 0, this)).isLessThanOrEqualTo(0) }
        10 times { expectThat(reader.read(CharArray(1), 0, this)).isEqualTo(-1) }
    }

    @Test
    fun `should read content`() {
        val line = "line #壹\nline #❷"
        val reader = NonBlockingCharReader(line.byteInputStream(), Duration.milliseconds(100))
        expectThat(reader.readLines()).containsExactly("line #壹", "line #❷")
    }

    @Slow @Test
    fun InMemoryLogger.`should read in a non-greedy fashion resp just as much as needed to avoid blocking`() {
        val inputStream = slowInputStream(
            Duration.seconds(0),
            Duration.seconds(1) to "123",
            Duration.seconds(2) to "abc",
            Duration.seconds(3) to "!§\"",
        )
        val reader = NonBlockingCharReader(inputStream, Duration.seconds(2))

        kotlin.runCatching {
            expectThat(reader.readLines()).containsExactly("123abc!\"")
                .get { this[0] }.not { contains("$") } // needs a wrapper like NonBlockingReader for characters of length > 1 byte
        }.recover {
            val color = ANSI.Colors.green
            if (it is AssertionFailedError) throw it
            fail(listOf("An exception has occurred while reading the input stream.",
                "Please make sure you don't use a greedy implementation like",
                InputStreamReader::class.qualifiedName?.let { it.ansi.magenta.toString() } + ".",
                "\nTheir reading strategy blocks the execution leaving you with nothing but timeouts and exceptions.",
                color.invoke(org.jline.utils.InputStreamReader::class.qualifiedName.toString()).toString() + "",
                " is known to be a working non-greedy implementation.")
                .wrapWithBorder(Borders.SpikedOutward), it)
        }
    }
}
