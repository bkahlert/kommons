package koodies.io.compress

import koodies.io.compress.GzCompressor.gunzip
import koodies.io.compress.GzCompressor.gzip
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
class GzCompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { randomPath().gzip() },
        { randomPath(extension = ".gz").gunzip() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<NoSuchFileException>()
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { singleFile().apply { addExtensions("gz").touch().writeText("content") }.gzip() },
        { archiveWithSingleFile("gz").apply { copyTo(removeExtensions("gz")) }.gunzip() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { singleFile().apply { addExtensions("gz").touch().writeText("content") }.gzip(overwrite = true) },
        { archiveWithSingleFile("gz").apply { copyTo(removeExtensions("gz")) }.gunzip(overwrite = true) },
    ).testWithTempDir(uniqueId) { call ->
        expectThat(call()).exists()
    }

    @Test
    fun `should gzip and gunzip`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file: Path = singleFile()
        file.requireNotEmpty()

        val gzippedFile = file.gzip()
        expectThat(gzippedFile.size).isLessThan(file.size)

        val renamedFile = file.renameTo("example".withRandomSuffix() + ".html")

        val gunzippedFile = gzippedFile.gunzip()
        gunzippedFile.requireNotEmpty()
        expectThat(gunzippedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}
