package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.TarArchiver.tar
import com.bkahlert.kommons.io.compress.TarArchiver.untar
import com.bkahlert.kommons.io.path.addExtensions
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.div

class TarArchiverTest {

    @TestFactory
    fun `should throw on missing source`(@TempDir tempDir: Path) = testEachOld<(Path) -> Path>(
        { (it / "missing").tar() },
        { (it / "missing.tar").untar() },
    ) { call ->
        expectThrows<NoSuchFileException> { call(tempDir) }
    }

    @TestFactory
    fun `should throw on non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar").touch().writeText("content") }.tar() },
        { archiveWithTwoFiles("tar").apply { copyTo(removeExtensions("tar")) }.untar() },
    ) { call ->
        withTempDir(simpleId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        { directoryWithTwoFiles().apply { addExtensions("tar").touch().writeText("content") }.tar(overwrite = true) },
        { archiveWithTwoFiles("tar").apply { copyTo(removeExtensions("tar")) }.untar(overwrite = true) },
    ) { call ->
        withTempDir(simpleId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar and untar`(simpleId: SimpleId) = withTempDir(simpleId) {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.tar()
        expectThat(archivedDir.getSize()).isGreaterThan(dir.getSize() * 1.2)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.untar()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
