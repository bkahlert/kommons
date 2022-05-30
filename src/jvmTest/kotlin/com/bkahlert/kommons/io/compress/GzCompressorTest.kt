package com.bkahlert.kommons.io.compress

import com.bkahlert.kommons.io.compress.GzCompressor.gunzip
import com.bkahlert.kommons.io.compress.GzCompressor.gzip
import com.bkahlert.kommons.io.path.addExtensions
import com.bkahlert.kommons.io.path.copyTo
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.hasEqualContent
import com.bkahlert.kommons.io.path.removeExtensions
import com.bkahlert.kommons.io.path.renameTo
import com.bkahlert.kommons.io.path.requireNotEmpty
import com.bkahlert.kommons.io.path.touch
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.randomPath
import com.bkahlert.kommons.test.Fixtures.archiveWithSingleFile
import com.bkahlert.kommons.test.Fixtures.singleFile
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.withRandomSuffix
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThan
import strikt.java.exists
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class GzCompressorTest {

    @TestFactory
    fun `should throw on missing source`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { randomPath().gzip() },
        { randomPath(extension = ".gz").gunzip() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<NoSuchFileException> { call() }
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("gz").touch().writeText("content") }.gzip() },
        { archiveWithSingleFile("gz").apply { copyTo(removeExtensions("gz")) }.gunzip() },
    ) { call ->
        withTempDir(uniqueId) {
            expectThrows<FileAlreadyExistsException> { call() }
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`(uniqueId: UniqueId) = testEach<Path.() -> Path>(
        { singleFile().apply { addExtensions("gz").touch().writeText("content") }.gzip(overwrite = true) },
        { archiveWithSingleFile("gz").apply { copyTo(removeExtensions("gz")) }.gunzip(overwrite = true) },
    ) { call ->
        withTempDir(uniqueId) {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should gzip and gunzip`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file: Path = singleFile()
        file.requireNotEmpty()

        val gzippedFile = file.gzip()
        expectThat(gzippedFile.getSize()).isLessThan(file.getSize())

        val renamedFile = file.renameTo("example".withRandomSuffix() + ".html")

        val gunzippedFile = gzippedFile.gunzip()
        gunzippedFile.requireNotEmpty()
        expectThat(gunzippedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}
