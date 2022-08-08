package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.test.copyToDirectory
import com.bkahlert.kommons.test.fixtures.HtmlDocumentFixture
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import java.io.IOException
import java.nio.file.NoSuchFileException

class StdLibMissingKtTest {

    @Nested
    inner class ReadLine {

        @Test
        fun `should read line`(simpleId: SimpleId) = withTempDir(simpleId) {
            val file = HtmlDocumentFixture.copyToDirectory(this)
            expectThat(file.readLine(6)).isEqualTo("    <p>Hello World!</p>")
        }

        @Test
        fun `should return null if file has less lines`(simpleId: SimpleId) = withTempDir(simpleId) {
            val file = HtmlDocumentFixture.copyToDirectory(this)
            expectThat(file.readLine(16)).isNull()
        }

        @Test
        fun `should throw on illegal line`(simpleId: SimpleId) = withTempDir(simpleId) {
            val file = HtmlDocumentFixture.copyToDirectory(this)
            expectCatching { file.readLine(0) }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on missing file`(simpleId: SimpleId) = withTempDir(simpleId) {
            val file = resolve("missing.txt")
            expectCatching { file.readLine(6) }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw on non-file`(simpleId: SimpleId) = withTempDir(simpleId) {
            val file = this
            expectCatching { file.readLine(6) }.isFailure().isA<IOException>()
        }
    }
}
