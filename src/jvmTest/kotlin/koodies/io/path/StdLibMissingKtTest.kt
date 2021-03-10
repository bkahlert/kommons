package koodies.io.path

import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.Fixtures.singleFile
import koodies.test.Fixtures.symbolicLink
import koodies.test.HtmlFile
import koodies.test.UniqueId
import koodies.test.copyToDirectory
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import kotlin.io.path.createDirectory
import kotlin.io.path.isSymbolicLink

@Execution(CONCURRENT)
class StdLibMissingKtTest {

    @Nested
    inner class ReadLine {

        @Test
        fun `should read line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = HtmlFile.copyToDirectory(this)
            expectThat(file.readLine(6)).isEqualTo("    <p>Hello World!</p>")
        }

        @Test
        fun `should return null if file has less lines`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = HtmlFile.copyToDirectory(this)
            expectThat(file.readLine(16)).isNull()
        }

        @Test
        fun `should throw on illegal line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = HtmlFile.copyToDirectory(this)
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
    }
}
