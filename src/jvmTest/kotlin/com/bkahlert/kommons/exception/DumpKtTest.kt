package com.bkahlert.kommons.exception

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.regex.RegularExpressions
import com.bkahlert.kommons.regex.findAllValues
import com.bkahlert.kommons.test.TextFixture
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.LineSeparators.LF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
import java.nio.file.Path
import kotlin.io.path.readText

class DumpKtTest {

    @Nested
    inner class DumpKtTest {

        @Test
        fun `should contain explanation`(@TempDir tempDir: Path) {
            expectThat(tempDir.dump(null, data = TextFixture.text)).contains("A dump has been written")
        }

        @Test
        fun `should contain capitalized custom explanation next to default one`(@TempDir tempDir: Path) {
            expectThat(tempDir.dump("custom explanation", data = TextFixture.text))
                .contains("Custom explanation")
                .contains("A dump has been written")
        }

        @Test
        fun `should contain url pointing to dumps`(@TempDir tempDir: Path) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.Default)
            expectThat(tempDir.dump("", data = data)).get {
                RegularExpressions.urlRegex.findAllValues(this)
                    .map { url -> URL(url) }
                    .map { url -> url.openStream().reader().readText() }
                    .toList()
            }.hasSize(2).all {
                isEqualTo(data)
            }
        }

        @Test
        fun `should contains last lines of dump`(@TempDir tempDir: Path) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.Default)
            expectThat(tempDir.dump("", data = data)).get { lines().takeLast(11).map { it.trim() } }
                .containsExactly(data.lines().takeLast(10) + "")
        }

        @Test
        fun `should log all lines if problem saving the log`(@TempDir tempDir: Path) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.Default)
            val path = tempDir.createTempFile(suffix = ".log").writeText("already exists")
            path.toFile().setReadOnly()
            expectThat(tempDir.dump("error message", path = path, data = data)) {
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
        fun `should dump data`(@TempDir tempDir: Path) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.Default)
            val dumps = persistDump(path = tempDir.createTempFile(suffix = ".log"), data = { data })
            expectThat(dumps.values.map { it.readText() }).hasSize(2).all {
                isEqualTo(data)
            }
        }

        @Test
        fun `should throw if data could not be dumped`(@TempDir tempDir: Path) {
            val path = tempDir.createTempFile(suffix = ".log").writeText("already exists")
            path.toFile().setReadOnly()
            expectCatching { persistDump(path = path, data = { TextFixture.text }) }.isFailure().isA<IOException>()
            path.toFile().setWritable(true)
        }

        @Test
        fun `should dump IO to file with ansi formatting`(@TempDir tempDir: Path) {
            val dumps = persistDump(path = tempDir.createTempFile(suffix = ".log"), data = { "ansi".ansi.bold.done + LF + "no ansi" }).values
            expectThat(dumps).filter { !it.pathString.endsWith("ansi-removed.log") }.single().hasContent(
                """
                ${"ansi".ansi.bold}
                no ansi
            """.trimIndent()
            )
        }

        @Test
        fun `should dump IO to file without ansi formatting`(@TempDir tempDir: Path) {
            val dumps = persistDump(path = tempDir.createTempFile(suffix = ".log"), data = { "ansi".ansi.bold.done + LF + "no ansi" }).values
            expectThat(dumps).filter { it.pathString.endsWith("ansi-removed.log") }.single().hasContent(
                """
                ansi
                no ansi
            """.trimIndent()
            )
        }
    }
}
