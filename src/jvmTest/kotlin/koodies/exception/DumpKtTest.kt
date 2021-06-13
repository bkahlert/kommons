package koodies.exception

import koodies.io.path.hasContent
import koodies.io.path.pathString
import koodies.io.randomPath
import koodies.io.path.writeText
import koodies.regex.RegularExpressions
import koodies.regex.findAllValues
import koodies.test.TextFixture
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators.LF
import koodies.text.randomString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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

class DumpKtTest {

    @Nested
    inner class DumpKtTest {

        @Test
        fun `should contain explanation`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(dump(null, data = TextFixture.text)).contains("A dump has been written")
        }

        @Test
        fun `should contain capitalized custom explanation next to default one`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(dump("custom explanation", data = TextFixture.text))
                .contains("Custom explanation")
                .contains("A dump has been written")
        }

        @Test
        fun `should contain url pointing to dumps`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LF)
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
        fun `should contains last lines of dump`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LF)
            expectThat(dump("", data = data)).get { lines().takeLast(11).map { it.trim() } }
                .containsExactly(data.lines().takeLast(10) + "")
        }

        @Test
        fun `should log all lines if problem saving the log`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LF)
            val path = randomPath(extension = ".log").writeText("already exists")
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
    inner class PersistDump {

        @Test
        fun `should dump data`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LF)
            val dumps = persistDump(path = randomPath(extension = ".log"), data = { data })
            expectThat(dumps.values.map { it.readText() }).hasSize(2).all {
                isEqualTo(data)
            }
        }

        @Test
        fun `should throw if data could not be dumped`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val path = randomPath(extension = ".log").writeText("already exists")
            path.toFile().setReadOnly()
            expectCatching { persistDump(path = path, data = { TextFixture.text }) }.isFailure().isA<IOException>()
            path.toFile().setWritable(true)
        }

        @Test
        fun `should dump IO to file with ansi formatting`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dumps = persistDump(path = randomPath(extension = ".log"), data = { "ansi".ansi.bold.done + LF + "no ansi" }).values
            expectThat(dumps).filter { !it.pathString.endsWith("ansi-removed.log") }.single().hasContent("""
                ${"ansi".ansi.bold}
                no ansi
            """.trimIndent())
        }

        @Test
        fun `should dump IO to file without ansi formatting`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dumps = persistDump(path = randomPath(extension = ".log"), data = { "ansi".ansi.bold.done + LF + "no ansi" }).values
            expectThat(dumps).filter { it.pathString.endsWith("ansi-removed.log") }.single().hasContent("""
                ansi
                no ansi
            """.trimIndent())
        }
    }
}
