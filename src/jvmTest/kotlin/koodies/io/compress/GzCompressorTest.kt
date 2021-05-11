package koodies.io.compress

import koodies.io.compress.GzCompressor.gunzip
import koodies.io.compress.GzCompressor.gzip
import koodies.io.path.addExtensions
import koodies.io.path.copyTo
import koodies.io.path.getSize
import koodies.io.path.hasEqualContent
import koodies.io.path.randomPath
import koodies.io.path.removeExtensions
import koodies.io.path.renameTo
import koodies.io.path.requireNotEmpty
import koodies.io.path.touch
import koodies.io.path.writeText
import koodies.test.Fixtures.archiveWithSingleFile
import koodies.test.Fixtures.singleFile
import koodies.test.UniqueId
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.withRandomSuffix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class GzCompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { randomPath().gzip() },
        { randomPath(extension = ".gz").gunzip() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<NoSuchFileException> { call() }
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("gz").touch().writeText("content") }.gzip() },
        { archiveWithSingleFile("gz").apply { copyTo(removeExtensions("gz")) }.gunzip() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("gz").touch().writeText("content") }.gzip(overwrite = true) },
        { archiveWithSingleFile("gz").apply { copyTo(removeExtensions("gz")) }.gunzip(overwrite = true) },
    ) { call ->
        withTempDir(uniqueId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should gzip and gunzip`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file: Path = singleFile()
        file.requireNotEmpty()

        val gzippedFile = file.gzip()
        expectThat(gzippedFile.getSize()).isLessThan(file.getSize())

        val renamedFile = file.renameTo("example".withRandomSuffix() + ".html")

        val gunzippedFile = gzippedFile.gunzip()
        gunzippedFile.requireNotEmpty()
        expectThat(gunzippedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}
