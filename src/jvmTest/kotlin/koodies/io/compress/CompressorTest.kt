package koodies.io.compress

import koodies.io.compress.Compressor.compress
import koodies.io.compress.Compressor.decompress
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
import koodies.test.junit.UniqueId
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

class CompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { randomPath().compress() },
        { randomPath(extension = ".bzip2").decompress() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<NoSuchFileException> { call() }
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2") },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2", overwrite = true) },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress(overwrite = true) },
    ) { call ->
        withTempDir(uniqueId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should compress and decompress`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file: Path = singleFile()
        file.requireNotEmpty()

        val compressedFile = file.compress()
        expectThat(compressedFile.getSize()).isLessThan(file.getSize() * 1.2)

        val renamedFile = file.renameTo("example".withRandomSuffix() + ".html")

        val decompressedFile = compressedFile.decompress()
        decompressedFile.requireNotEmpty()
        expectThat(decompressedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}
