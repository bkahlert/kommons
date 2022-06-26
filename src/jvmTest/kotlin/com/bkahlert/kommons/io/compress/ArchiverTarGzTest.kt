package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.createTempDirectory
import com.bkahlert.kommons.io.compress.Archiver.archive
import com.bkahlert.kommons.io.compress.Archiver.listArchive
import com.bkahlert.kommons.io.compress.Archiver.unarchive
import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.tarGunzip
import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.tarGzip
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.getSize
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
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.div

class ArchiverTarGzTest {

    @TestFactory
    fun `should throw on missing source`(@TempDir tempDir: Path) = testEachOld<(Path) -> Path>(
        { (it / "missing").archive() },
        { (it / "missing.tar.gz").unarchive() },
        { (it / "missing.tar.gz").apply { listArchive() } },
    ) { call ->
        expectThrows<NoSuchFileException> { call(tempDir) }
    }

    @TestFactory
    fun `should throw on non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar").addExtensions("gz").touch().writeText("content") }.archive("tar.gz") },
        { archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("gz").removeExtensions("tar")) }.unarchive() },
    ) { call ->
        withTempDir(simpleId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        {
            createTempDirectory().directoryWithTwoFiles().apply { addExtensions("tar").addExtensions("gz").touch().writeText("content") }
                .archive("tar.gz", overwrite = true)
        },
        { createTempDirectory().archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("gz").removeExtensions("tar")) }.unarchive(overwrite = true) },
    ) { call ->
        withTempDir(simpleId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar-gzip and untar-gunzip`(simpleId: SimpleId) = withTempDir(simpleId) {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.tarGzip()
        expectThat(archivedDir.getSize()).isLessThan(dir.getSize().coerceAtLeast(550.bytes))

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.tarGunzip()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
