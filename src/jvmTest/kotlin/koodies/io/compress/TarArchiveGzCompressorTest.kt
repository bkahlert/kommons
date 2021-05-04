package koodies.io.compress

import koodies.io.compress.TarArchiveGzCompressor.listArchive
import koodies.io.compress.TarArchiveGzCompressor.tarGunzip
import koodies.io.compress.TarArchiveGzCompressor.tarGzip
import koodies.io.path.addExtensions
import koodies.io.path.copyTo
import koodies.io.path.getSize
import koodies.io.path.hasSameFiles
import koodies.io.path.randomPath
import koodies.io.path.removeExtensions
import koodies.io.path.renameTo
import koodies.io.path.touch
import koodies.test.Fixtures.archiveWithTwoFiles
import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.UniqueId
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import koodies.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.writeText

class TarArchiveGzCompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { randomPath().tarGzip() },
        { randomPath(extension = ".tar.gz").tarGunzip() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<NoSuchFileException>()
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar.gz").touch().writeText("content") }.tarGzip() },
        { archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("tar.gz")) }.tarGunzip() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar.gz").touch().writeText("content") }.tarGzip(overwrite = true) },
        { archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("tar.gz")) }.tarGunzip(overwrite = true) },
    ).testWithTempDir(uniqueId) { call ->
        expectThat(call()).exists()
    }

    @Test
    fun `should tar-gzip listArchive and untar-gunzip`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.tarGzip()
        expectThat(archivedDir.getSize()).isLessThan(dir.getSize().coerceAtLeast(500.bytes))

        expectThat(archivedDir.listArchive().map { it.name }).containsExactlyInAnyOrder("example.html", "sub-dir/", "sub-dir/config.txt")

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.tarGunzip()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
