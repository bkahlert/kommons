package koodies.io.compress

import koodies.io.compress.TarArchiver.tar
import koodies.io.compress.TarArchiver.untar
import koodies.io.path.addExtensions
import koodies.io.path.hasSameFiles
import koodies.io.path.randomPath
import koodies.io.path.removeExtensions
import koodies.io.path.renameTo
import koodies.io.path.touch
import koodies.io.path.writeText
import koodies.test.Fixtures.archiveWithTwoFiles
import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.UniqueId
import koodies.test.testWithTempDir
import koodies.test.withTempDir
import koodies.unit.size
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.copyTo

@Execution(CONCURRENT)
class TarArchiverTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { randomPath().tar() },
        { randomPath(extension = ".tar").untar() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<NoSuchFileException>()
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar").touch().writeText("content") }.tar() },
        { archiveWithTwoFiles("tar").apply { copyTo(removeExtensions("tar")) }.untar() },
    ).testWithTempDir(uniqueId) { call ->
        expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = listOf<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar").touch().writeText("content") }.tar(overwrite = true) },
        { archiveWithTwoFiles("tar").apply { copyTo(removeExtensions("tar")) }.untar(overwrite = true) },
    ).testWithTempDir(uniqueId) { call ->
        expectThat(call()).exists()
    }

    @Test
    fun `should tar and untar`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.tar()
        expectThat(archivedDir.size).isGreaterThan(dir.size)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.untar()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
