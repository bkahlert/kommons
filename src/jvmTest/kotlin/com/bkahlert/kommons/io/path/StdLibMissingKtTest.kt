package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.io.copyToDirectory
import com.bkahlert.kommons.test.Fixtures.directoryWithTwoFiles
import com.bkahlert.kommons.test.Fixtures.singleFile
import com.bkahlert.kommons.test.Fixtures.symbolicLink
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.hasElements
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.time.Now
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotEqualTo
import strikt.assertions.isNull
import strikt.java.exists
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
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

    @Nested
    inner class ListDirectoryEntriesRecursively {

        @Test
        fun `should list all entries recursively`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = directoryWithTwoFiles()
            val subject = dir.listDirectoryEntriesRecursively()
            expectThat(subject).containsExactly(
                dir.resolve("sub-dir"),
                dir.resolve("sub-dir/config.txt"),
                dir.resolve("example.html"))
        }

        @Test
        fun `should apply glob expression`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = directoryWithTwoFiles()
            val subject = dir.listDirectoryEntriesRecursively("**/*.*")
            expectThat(subject).containsExactly(
                dir.resolve("sub-dir/config.txt"),
                dir.resolve("example.html"))
        }

        @Test
        fun `should throw on listing file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomFile().listDirectoryEntriesRecursively() }.isFailure().isA<NotDirectoryException>()
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `should delete file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = singleFile()
            expect {
                that(file.delete()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Test
        fun `should delete empty directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir").createDirectory()
            expect {
                that(dir.delete()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Test
        fun `should throw on non-empty directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir").createDirectory().apply { singleFile() }
            expectCatching { dir.delete() }.isFailure().isA<DirectoryNotEmptyException>()
        }

        @Test
        fun `should delete non-existing file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file")
            expect {
                that(file.delete()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Nested
        inner class WithNoFollowLinks {

            @Test
            fun `should delete symbolic link itself`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val symbolicLink = symbolicLink()
                symbolicLink.delete()
                expect {
                    that(symbolicLink.delete(NOFOLLOW_LINKS)).not { exists(NOFOLLOW_LINKS) }
                    that(this@withTempDir).isEmpty()
                }
            }
        }

        @Nested
        inner class WithoutNoFollowLinks {

            @Test
            fun `should not delete symbolic link itself`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val symbolicLink = symbolicLink()
                expect {
                    that(symbolicLink.isSymbolicLink())
                    that(this@withTempDir).isNotEmpty()
                }
            }
        }
    }

    @Nested
    inner class DeleteRecursively {

        @Test
        fun `should delete file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = singleFile()
            expect {
                that(file.deleteRecursively()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Test
        fun `should delete empty directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir").createDirectory()
            expect {
                that(dir.deleteRecursively()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Test
        fun `should delete non-empty directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir").createDirectory().apply { singleFile() }
            expect {
                that(dir.deleteRecursively()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Test
        fun `should delete non-existing file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file")
            expect {
                that(file.deleteRecursively()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Test
        fun `should delete complex file tree`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir")
            dir.directoryWithTwoFiles().symbolicLink()
            expect {
                that(dir.deleteRecursively()).not { exists() }
                that(this@withTempDir).isEmpty()
            }
        }

        @Test
        fun `should delete filtered files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir")
            val exception = dir.directoryWithTwoFiles().listDirectoryEntriesRecursively().first()
            expectThat(dir.deleteRecursively { it != exception && !it.isDirectory() }) {
                exists()
                hasElements({
                    isNotEqualTo(exception)
                })
            }
        }
    }

    @Nested
    inner class ListDirectoryEntriesRecursivelyOperation {

        @Test
        fun `should delete directory contents`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir")
            dir.directoryWithTwoFiles()
            expectThat(dir.deleteDirectoryEntriesRecursively()) {
                exists()
                isEmpty()
            }
        }

        @Test
        fun `should delete filtered directory contents`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir")
            val exception = dir.directoryWithTwoFiles().listDirectoryEntriesRecursively().first()
            expectThat(dir.deleteDirectoryEntriesRecursively { it != exception && !it.isDirectory() }) {
                exists()
                hasElements({
                    isNotEqualTo(exception)
                })
            }
        }

        @Test
        fun `should throws on non-directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file").createFile()
            expectThrows<NotDirectoryException> { file.deleteDirectoryEntriesRecursively() }
        }
    }

    @Isolated
    @Nested
    inner class DeleteOnExit {

        private val name = "kommons.onexit.does-not-work.txt"
        private val markerFile: Path = Locations.temp / name

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
            markerFile.writeText("""
            This file was created $Now.
            It used to be cleaned up by the Kommons library
            the moment the application in question shut down.
            
            The application was started by ${System.getProperty("sun.java.command")}.
        """.trimIndent())
        }
    }
}
