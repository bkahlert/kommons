package koodies.io.compress

import koodies.io.compress.Archiver.archive
import koodies.io.compress.Archiver.listArchive
import koodies.io.compress.Archiver.unarchive
import koodies.io.path.addExtensions
import koodies.io.path.copyTo
import koodies.io.path.hasSameFiles
import koodies.io.path.randomPath
import koodies.io.path.removeExtensions
import koodies.io.path.renameTo
import koodies.io.path.touch
import koodies.io.path.writeText
import koodies.test.Fixtures.archiveWithTwoFiles
import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

@Execution(CONCURRENT)
class ArchiverTest {

    @TestFactory
    fun `should throw on missing source`() = listOf<Path.() -> Path>(
        { randomPath().archive() },
        { randomPath(extension = ".zip").unarchive() },
        { randomPath(extension = ".tar.gz").apply { listArchive() } },
    ).testWithTempDir { call ->
        expectCatching { call() }.isFailure().isA<NoSuchFileException>()
    }

    @TestFactory
    fun `should throw on non-empty destination`() = listOf<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("zip").touch().writeText("content") }.archive("zip") },
        { archiveWithTwoFiles("zip").apply { copyTo(removeExtensions("zip")) }.unarchive() },
    ).testWithTempDir { call ->
        expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
    }

    @TestFactory
    fun `should overwrite non-empty destination`() = listOf<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("zip").touch().writeText("content") }.archive("zip", overwrite = true) },
        { archiveWithTwoFiles("zip").apply { copyTo(removeExtensions("zip")) }.unarchive(overwrite = true) },
    ).testWithTempDir { call ->
        expectThat(call()).exists()
    }

    @Test
    fun `should archive and unarchive`() = withTempDir {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.archive()

        expectThat(archivedDir.listArchive().map { it.name }).containsExactlyInAnyOrder("example.html", "sub-dir/", "sub-dir/config.txt")

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.unarchive()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
