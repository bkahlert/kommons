package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.Archiver.archive
import com.bkahlert.kommons.io.compress.Archiver.listArchive
import com.bkahlert.kommons.io.compress.Archiver.unarchive
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.hasSameFiles
import com.bkahlert.kommons.io.path.randomPath
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.renameTo
import com.bkahlert.kommons.io.path.touch
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.test.Fixtures.archiveWithTwoFiles
import com.bkahlert.kommons.test.Fixtures.directoryWithTwoFiles
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.withTempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class ArchiverTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { randomPath().archive() },
        { randomPath(extension = ".zip").unarchive() },
        { randomPath(extension = ".tar.gz").apply { listArchive() } },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<NoSuchFileException> { call() }
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("zip").touch().writeText("content") }.archive("zip") },
        { archiveWithTwoFiles("zip").apply { copyTo(removeExtensions("zip")) }.unarchive() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("zip").touch().writeText("content") }.archive("zip", overwrite = true) },
        { archiveWithTwoFiles("zip").apply { copyTo(removeExtensions("zip")) }.unarchive(overwrite = true) },
    ) { call ->
        withTempDir(uniqueId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should archive and unarchive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.archive()

        expectThat(archivedDir.listArchive().map { it.name }).containsExactlyInAnyOrder("example.html", "sub-dir/", "sub-dir/config.txt")

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.unarchive()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
