package com.bkahlert.kommons.io.compress

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
import com.bkahlert.kommons.randomDirectory
import com.bkahlert.kommons.randomPath
import com.bkahlert.kommons.test.Fixtures.archiveWithTwoFiles
import com.bkahlert.kommons.test.Fixtures.directoryWithTwoFiles
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class ArchiverTarGzTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { randomPath().archive() },
        { randomPath(extension = ".tar.gz").unarchive() },
        { randomPath(extension = ".tar.gz").apply { listArchive() } },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<NoSuchFileException> { call() }
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar").addExtensions("gz").touch().writeText("content") }.archive("tar.gz") },
        { archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("gz").removeExtensions("tar")) }.unarchive() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        {
            randomDirectory().directoryWithTwoFiles().apply { addExtensions("tar").addExtensions("gz").touch().writeText("content") }
                .archive("tar.gz", overwrite = true)
        },
        { randomDirectory().archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("gz").removeExtensions("tar")) }.unarchive(overwrite = true) },
    ) { call ->
        withTempDir(uniqueId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar-gzip and untar-gunzip`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.tarGzip()
        expectThat(archivedDir.getSize()).isLessThan(dir.getSize().coerceAtLeast(500.bytes))

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.tarGunzip()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
