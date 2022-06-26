package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.Compressor.compress
import com.bkahlert.kommons.io.compress.Compressor.decompress
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.hasEqualContent
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.renameTo
import com.bkahlert.kommons.io.path.requireNotEmpty
import com.bkahlert.kommons.io.path.touch
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.test.Fixtures.archiveWithSingleFile
import com.bkahlert.kommons.test.Fixtures.singleFile
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.withRandomSuffix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.div

class CompressorTest {

    @TestFactory
    fun `should throw on missing source`(@TempDir tempDir: Path) = testEachOld<(Path) -> Path>(
        { (it / "missing").compress() },
        { (it / "missing.bzip2").decompress() },
    ) { call ->
        expectThrows<NoSuchFileException> { call(tempDir) }
    }

    @TestFactory
    fun `should throw on non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2") },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress() },
    ) { call ->
        withTempDir(simpleId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(simpleId: SimpleId) = testEachOld<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2", overwrite = true) },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress(overwrite = true) },
    ) { call ->
        withTempDir(simpleId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should compress and decompress`(simpleId: SimpleId) = withTempDir(simpleId) {
        val file: Path = singleFile()
        file.requireNotEmpty()

        val compressedFile = file.compress()
        expectThat(compressedFile.getSize()).isLessThan(file.getSize() * 1.2)

        val renamedFile = file.renameTo("example".withRandomSuffix() + ".html")

        val decompressedFile = compressedFile.decompress()
        decompressedFile.requireNotEmpty()
        expectThat(decompressedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}
