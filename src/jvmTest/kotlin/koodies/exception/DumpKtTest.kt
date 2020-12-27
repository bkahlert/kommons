package koodies.exception

import koodies.io.path.asString
import koodies.io.path.hasContent
import koodies.io.path.randomPath
import koodies.io.path.writeText
import koodies.regex.RegularExpressions
import koodies.regex.findAllValues
import koodies.terminal.AnsiFormats.bold
import koodies.test.TextFile
import koodies.test.withTempDir
import koodies.text.LineSeparators
import koodies.text.randomString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.filter
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.single
import strikt.assertions.startsWith
import java.io.IOException
import java.net.URL
import kotlin.io.path.readText

@Execution(CONCURRENT)
class DumpKtTest {

    @Nested
    inner class DumpKtTest {

        @Test
        fun `should contain explanation`() = withTempDir {
            expectThat(dump(null, data = TextFile.text)).contains("A dump has been written")
        }

        @Test
        fun `should contain capitalized custom explanation next to default one`() = withTempDir {
            expectThat(dump("custom explanation", data = TextFile.text))
                .contains("Custom explanation")
                .contains("A dump has been written")
        }

        @Test
        fun `should contain url pointing to dumps`() = withTempDir {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.LF)
            expectThat(dump("", data = data)).get {
                RegularExpressions.urlRegex.findAllValues(this)
                    .map { url -> URL(url) }
                    .map { url -> url.openStream().reader().readText() }
                    .toList()
            }.hasSize(2).all {
                isEqualTo(data)
            }
        }

        @Test
        fun `should contains last lines of dump`() = withTempDir {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.LF)
            expectThat(dump("", data = data)).get { lines().takeLast(11).map { it.trim() } }
                .containsExactly(data.lines().takeLast(10) + "")
        }

        @Test
        fun `should log all lines if problem saving the log`() = withTempDir {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.LF)
            val path = randomPath(extension = ".log").apply { writeText("already exists") }
            path.toFile().setReadOnly()
            expectThat(dump("error message", path = path, data = data)) {
                get { lines().take(2) }.containsExactly("Error message", "In the attempt to persist the corresponding dump the following error occurred:")
                get { lines().drop(2).first() }.startsWith("AccessDeniedException: ")
                get { lines() }
                    .any { contains("The not successfully persisted dump is as follows:") }
                    .contains(data.lines())
            }
            path.toFile().setWritable(true)
        }
    }

    @Nested
    class PersistDump {

        @Test
        fun `should dump data`() = withTempDir {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.LF)
            val dumps = persistDump(path = randomPath(extension = ".log"), data = { data })
            expectThat(dumps.values.map { it.readText() }).hasSize(2).all {
                isEqualTo(data)
            }
        }

        @Test
        fun `should throw if data could not be dumped`() = withTempDir {
            val path = randomPath(extension = ".log").apply { writeText("already exists") }
            path.toFile().setReadOnly()
            expectCatching { persistDump(path = path, data = { TextFile.text }) }.isFailure().isA<IOException>()
            path.toFile().setWritable(true)
        }

        @Test
        fun `should dump IO to file with ansi formatting`() = withTempDir {
            val dumps = persistDump(path = randomPath(extension = ".log"), data = { "ansi".bold() + LineSeparators.LF + "no ansi" }).values
            expectThat(dumps).filter { !it.asString().endsWith("no-ansi.log") }.single().hasContent("""
                ${"ansi".bold()}
                no ansi
            """.trimIndent())
        }

        @Test
        fun `should dump IO to file without ansi formatting`() = withTempDir {
            val dumps = persistDump(path = randomPath(extension = ".log"), data = { "ansi".bold() + LineSeparators.LF + "no ansi" }).values
            expectThat(dumps).filter { it.asString().endsWith("no-ansi.log") }.single().hasContent("""
                ansi
                no ansi
            """.trimIndent())
        }
    }

}
