package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.Compressor.compress
import com.bkahlert.kommons.io.compress.Compressor.decompress
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.hasEqualContent
import com.bkahlert.kommons.io.path.randomPath
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.renameTo
import com.bkahlert.kommons.io.path.requireNotEmpty
import com.bkahlert.kommons.io.path.touch
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.test.Fixtures.archiveWithSingleFile
import com.bkahlert.kommons.test.Fixtures.singleFile
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.withRandomSuffix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class CompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { randomPath().compress() },
        { randomPath(extension = ".bzip2").decompress() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<NoSuchFileException> { call() }
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2") },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("bzip2").touch().writeText("content") }.compress("bzip2", overwrite = true) },
        { archiveWithSingleFile("bzip2").apply { copyTo(removeExtensions("bzip2")) }.decompress(overwrite = true) },
    ) { call ->
        withTempDir(uniqueId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should compress and decompress`(uniqueId: UniqueId) = withTempDir(uniqueId) {
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