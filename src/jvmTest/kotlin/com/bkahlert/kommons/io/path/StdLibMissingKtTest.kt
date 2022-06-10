package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.io.copyToDirectory
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.time.Now
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import strikt.java.exists
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

class StdLibMissingKtTest {

    @Nested
    inner class ReadLine {

        @Test
        fun `should read line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = HtmlFixture.copyToDirectory(this)
            expectThat(file.readLine(6)).isEqualTo("    <p>Hello World!</p>")
        }

        @Test
        fun `should return null if file has less lines`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = HtmlFixture.copyToDirectory(this)
            expectThat(file.readLine(16)).isNull()
        }

        @Test
        fun `should throw on illegal line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = HtmlFixture.copyToDirectory(this)
            expectCatching { file.readLine(0) }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on missing file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("missing.txt")
            expectCatching { file.readLine(6) }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw on non-file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = this
            expectCatching { file.readLine(6) }.isFailure().isA<IOException>()
        }
    }

    @Isolated
    @Nested
    inner class DeleteOnExit {

        private val name = "kommons.onexit.does-not-work.txt"
        private val markerFile: Path = SystemLocations.Temp / name

        @BeforeAll
        fun setUp() {
            markerFile.deleteOnExit(true)
        }

        @Test
        fun `should clean up on shutdown`() {
            expectThat(markerFile).not { exists() }
        }

        @AfterAll
        fun tearDown() {
            markerFile.writeText(
                """
            This file was created $Now.
            It used to be cleaned up by the Kommons library
            the moment the application in question shut down.
            
            The application was started by ${System.getProperty("sun.java.command")}.
        """.trimIndent()
            )
        }
    }
}
