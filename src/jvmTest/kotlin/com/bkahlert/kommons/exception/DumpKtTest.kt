package com.bkahlert.kommons.exception

import com.bkahlert.kommons.LineSeparators
import com.bkahlert.kommons.LineSeparators.LF
import com.bkahlert.kommons.UrlRegex
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.findAllValues
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.test.fixtures.UnicodeTextDocumentFixture
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.io.path.readText

class DumpKtTest {

    @Nested
    inner class DumpKtTest {

        @Test
        fun `should contain explanation`(@TempDir tempDir: Path) {
            tempDir.dump(null, data = UnicodeTextDocumentFixture.contents) shouldContain "A dump has been written"
        }

        @Test
        fun `should contain capitalized custom explanation next to default one`(@TempDir tempDir: Path) {
            tempDir.dump("custom explanation", data = UnicodeTextDocumentFixture.contents) should {
                it shouldContain "Custom explanation"
                it shouldContain "A dump has been written"
            }
        }

        @Test
        fun `should contain url pointing to dumps`(@TempDir tempDir: Path) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.Default)
            tempDir.dump("", data = data) should {
                Regex.UrlRegex.findAllValues(it, 0)
                    .map { url -> URL(url) }
                    .map { url -> url.openStream().reader().readText() }
                    .toList() should {
                    it shouldHaveSize 2
                    it.forAll { it shouldBe data }
                }
            }
        }

        @Test
        fun `should contains last lines of dump`(@TempDir tempDir: Path) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.Default)
            tempDir.dump("", data = data).lines().takeLast(11).map { it.trim() }
                .shouldContainExactly(data.lines().takeLast(10) + "")
        }

        @Test
        fun `should log all lines if problem saving the log`(@TempDir tempDir: Path) {
            val data = (0 until 15).map { randomString(20) }.joinToString(LineSeparators.Default)
            val path = tempDir.createTempFile(suffix = ".log").writeText("already exists")
            path.toFile().setReadOnly()
            tempDir.dump("error message", path = path, data = data) should {
                it.lines().take(2).shouldContainExactly("Error message", "In the attempt to persist the corresponding dump the following error occurred:")
                it.lines().drop(2).first() shouldStartWith "AccessDeniedException: "
                it.lines() should {
                    it.forAny { it.shouldContain("The not successfully persisted dump is as follows:") }
                    it.shouldContainAll(data.lines())
                }
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
            dumps.values.map { it.readText() } should {
                it shouldHaveSize 2
                it.forAll { it shouldBe data }
            }
        }

        @Test
        fun `should throw if data could not be dumped`(@TempDir tempDir: Path) {
            val path = tempDir.createTempFile(suffix = ".log").writeText("already exists")
            path.toFile().setReadOnly()
            shouldThrow<IOException> { persistDump(path = path, data = { UnicodeTextDocumentFixture.contents }) }
            path.toFile().setWritable(true)
        }

        @Test
        fun `should dump IO to file with ansi formatting`(@TempDir tempDir: Path) {
            val dumps = persistDump(path = tempDir.createTempFile(suffix = ".log"), data = { "ansi".ansi.bold.done + LF + "no ansi" }).values
            dumps.single { !it.pathString.endsWith("ansi-removed.log") }.readText() shouldBe """
                ${"ansi".ansi.bold}
                no ansi
            """.trimIndent()
        }

        @Test
        fun `should dump IO to file without ansi formatting`(@TempDir tempDir: Path) {
            val dumps = persistDump(path = tempDir.createTempFile(suffix = ".log"), data = { "ansi".ansi.bold.done + LF + "no ansi" }).values
            dumps.single { it.pathString.endsWith("ansi-removed.log") }.readText() shouldBe """
                ansi
                no ansi
            """.trimIndent()
        }
    }
}
