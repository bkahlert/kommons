package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.Archiver.archive
import com.bkahlert.kommons.io.compress.Archiver.listArchive
import com.bkahlert.kommons.io.compress.Archiver.unarchive
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.hasSameFiles
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.renameTo
import com.bkahlert.kommons.io.path.touch
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.test.Fixtures.archiveWithTwoFiles
import com.bkahlert.kommons.test.Fixtures.directoryWithTwoFiles
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.withTempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.div

class ArchiverTest {

    @TestFactory
    fun `should throw on missing source`(@TempDir tempDir: Path) = testEachOld<(Path) -> Path>(
        { (it / "missing").archive() },
        { (it / "missing.zip").unarchive() },
        { (it / "missing.tar.gz").apply { listArchive() } },
    ) { call ->
        expectThrows<NoSuchFileException> { call(tempDir) }
    }

    @TestFactory
    fun `should throw on non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("zip").touch().writeText("content") }.archive("zip") },
        { archiveWithTwoFiles("zip").apply { copyTo(removeExtensions("zip")) }.unarchive() },
    ) { call ->
        withTempDir(simpleId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("zip").touch().writeText("content") }.archive("zip", overwrite = true) },
        { archiveWithTwoFiles("zip").apply { copyTo(removeExtensions("zip")) }.unarchive(overwrite = true) },
    ) { call ->
        withTempDir(simpleId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should archive and unarchive`(simpleId: SimpleId) = withTempDir(simpleId) {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.archive()

        expectThat(archivedDir.listArchive().map { it.name }).containsExactlyInAnyOrder("hello-world.html", "sub-dir/", "sub-dir/config.txt")

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.unarchive()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
