package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.listArchive
import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.tarGunzip
import com.bkahlert.kommons.io.compress.TarArchiveGzCompressor.tarGzip
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.hasSameFiles
import com.bkahlert.kommons.io.path.randomPath
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.renameTo
import com.bkahlert.kommons.io.path.touch
import com.bkahlert.kommons.test.Fixtures.archiveWithTwoFiles
import com.bkahlert.kommons.test.Fixtures.directoryWithTwoFiles
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.writeText

class TarArchiveGzCompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { randomPath().tarGzip() },
        { randomPath(extension = ".tar.gz").tarGunzip() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<NoSuchFileException> { call() }
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar.gz").touch().writeText("content") }.tarGzip() },
        { archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("tar.gz")) }.tarGunzip() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar.gz").touch().writeText("content") }.tarGzip(overwrite = true) },
        { archiveWithTwoFiles("tar.gz").apply { copyTo(removeExtensions("tar.gz")) }.tarGunzip(overwrite = true) },
    ) { call ->
        withTempDir(uniqueId) {
            expectThat(call()).exists()
        }
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
