package koodies.io.compress

import koodies.io.compress.Compressor.compress
import koodies.io.compress.Compressor.decompress
import koodies.io.path.addExtensions
import koodies.io.path.copyTo
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
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import koodies.text.withRandomSuffix
import koodies.unit.size
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isLessThan
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path


@Execution(CONCURRENT)
class CompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { randomPath().compress() },
        { randomPath(extension = ".bzip2").decompress() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<NoSuchFileException>()
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2") },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2", overwrite = true) },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress(overwrite = true) },
    ).testWithTempDir(uniqueId) { call ->
        expectThat(call()).exists()
    }

    @Test
    fun `should compress and decompress`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file: Path = singleFile()
        file.requireNotEmpty()

        val compressedFile = file.compress()
        expectThat(compressedFile.size).isLessThan(file.size)

        val renamedFile = file.renameTo("example".withRandomSuffix() + ".html")

        val decompressedFile = compressedFile.decompress()
        decompressedFile.requireNotEmpty()
        expectThat(decompressedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}

