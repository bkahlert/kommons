package com.bkahlert.kommons.test

import com.bkahlert.kommons.deleteOnExit
import com.bkahlert.kommons.listDirectoryEntriesRecursively
import com.bkahlert.kommons.test.fixtures.EmojiTextDocumentFixture
import com.bkahlert.kommons.test.fixtures.GifImageFixture
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.fixtures.UnicodeTextDocumentFixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAny
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.readText

class JvmFixturesKtTest {

    @Test fun file_name() = testAll {
        SvgImageFixture.fileName shouldBe Paths.get("kommons.svg")
    }

    @Test fun input_stream() = testAll {
        GifImageFixture.inputStream().readBytes() shouldBe GifImageFixture.bytes
        SvgImageFixture.inputStream().readBytes() shouldBe SvgImageFixture.bytes
    }

    // TODO move to common
    @Test fun url() = testAll(
        EmojiTextDocumentFixture,
        GifImageFixture,
        HtmlDocumentFixture,
        SvgImageFixture,
        UnicodeTextDocumentFixture,
    ) { fixture ->
        fixture.url should {
            it.toString() shouldEndWith fixture.name
            it.readBytes() shouldBe fixture.bytes
        }
    }

    @Test fun reader() = testAll {
        SvgImageFixture.reader().readText() shouldBe SvgImageFixture.contents
    }

    @Test fun copy_to(@TempDir tempDir: Path) = testAll {
        SvgImageFixture.copyTo(tempDir / "file") should {
            it.exists() shouldBe true
            it.readText() shouldBe SvgImageFixture.contents
        }

        shouldThrow<FileAlreadyExistsException> { GifImageFixture.copyTo(tempDir / "file") }

        GifImageFixture.copyTo(tempDir / "file", overwrite = true) should {
            it.exists() shouldBe true
            it.readBytes() shouldBe GifImageFixture.bytes
        }
    }

    @Test fun copy_to_directory(@TempDir tempDir: Path) = testAll {
        SvgImageFixture.copyToDirectory(tempDir) should {
            it.exists() shouldBe true
            it.readText() shouldBe SvgImageFixture.contents
            it.fileName.pathString shouldBe "kommons.svg"
        }

        tempDir.resolve("pixels.gif").createFile()
        shouldThrow<FileAlreadyExistsException> { GifImageFixture.copyToDirectory(tempDir) }

        GifImageFixture.copyToDirectory(tempDir, overwrite = true) should {
            it.exists() shouldBe true
            it.readBytes() shouldBe GifImageFixture.bytes
        }
    }

    @Test fun copy_to_temp_file() = testAll {
        GifImageFixture.copyToTempFile().deleteOnExit() should {
            it.exists() shouldBe true
            it.readBytes() shouldBe GifImageFixture.bytes
            it.fileName.pathString shouldStartWith "pixels."
            it.fileName.pathString shouldEndWith ".gif"
        }
    }

    @Test fun copy_to_temp_text_file() = testAll {
        SvgImageFixture.copyToTempTextFile().deleteOnExit() should {
            it.exists() shouldBe true
            it.readText() shouldBe SvgImageFixture.contents
            it.fileName.pathString shouldStartWith "kommons."
            it.fileName.pathString shouldEndWith ".svg"
        }

        SvgImageFixture.copyToTempTextFile(Charsets.ISO_8859_1) should {
            it.exists() shouldBe true
            it.readText(Charsets.ISO_8859_1) shouldBe SvgImageFixture.contents
            it.fileName.pathString shouldStartWith "kommons."
            it.fileName.pathString shouldEndWith ".svg"
        }
    }


    @Test fun create_any_file(@TempDir tempDir: Path) {
        tempDir.createAnyFile("my.file") should {
            it.exists() shouldBe true
            it.readText() shouldBe SvgImageFixture.contents
            it.fileName.pathString shouldBe "my.file"
        }
        tempDir.createAnyFile() should {
            it.exists() shouldBe true
            it.readText() shouldBe SvgImageFixture.contents
            it.fileName.pathString shouldBe "kommons.svg"
        }
    }

    @Test fun create_random_file(@TempDir tempDir: Path) {
        tempDir.createRandomFile("my.file") should {
            it.exists() shouldBe true
            listOf(GifImageFixture, HtmlDocumentFixture, SvgImageFixture, UnicodeTextDocumentFixture).forAny { fixture ->
                it.readBytes() shouldBe fixture.bytes
            }
            it.fileName.pathString shouldBe "my.file"
        }

        tempDir.createRandomFile(GifImageFixture, HtmlDocumentFixture) should {
            it.exists() shouldBe true
            listOf(GifImageFixture, HtmlDocumentFixture).forAny { fixture ->
                it.readBytes() shouldBe fixture.bytes
                it.fileName.pathString shouldBe fixture.name
            }
            listOf(SvgImageFixture, UnicodeTextDocumentFixture).forNone { fixture ->
                it.readBytes() shouldBe fixture.bytes
                it.fileName.pathString shouldBe fixture.name
            }
        }
    }

    @Test fun create_directory_with_files(@TempDir tempDir: Path) {
        tempDir.createDirectoryWithFiles() should {
            it.exists() shouldBe true
            it.resolve("pixels.gif").readBytes() shouldBe GifImageFixture.contents
            it.resolve("kommons.svg").readText() shouldBe SvgImageFixture.contents
            it.resolve("docs") should { docs ->
                docs.shouldBeADirectory()
                docs.resolve("hello-world.html").readText() shouldBe HtmlDocumentFixture.contents
                docs.resolve("unicode.txt").readText() shouldBe UnicodeTextDocumentFixture.contents
            }
        }

        tempDir.createDirectoryWithFiles()

        tempDir.listDirectoryEntriesRecursively() shouldHaveSize 12
    }
}
