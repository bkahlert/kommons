package koodies.io.path

import koodies.io.compress.TarArchiver.tar
import koodies.io.file.isSiblingOf
import koodies.io.file.lastModified
import koodies.runtime.deleteOnExit
import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.Fixtures.singleFile
import koodies.test.UniqueId
import koodies.test.hasSameFileName
import koodies.test.withTempDir
import koodies.time.minus
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.time.days
import kotlin.time.seconds

@Execution(CONCURRENT)
class CopyKtTest {

    @Nested
    inner class CopyToKtTest {

        private fun Path.getTestFile() = randomFile(extension = ".txt").writeText("test file").apply { lastModified -= 7.days }
        private fun Path.getTestDir() = directoryWithTwoFiles().apply { listDirectoryEntriesRecursively().forEach { it.lastModified -= 7.days } }


        @Test
        fun `should throw on missing src`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val src = randomPath(extension = ".txt")
            expectCatching { src.copyTo(getTestFile()) }.isFailure().isA<NoSuchFileException>()
        }

        @Nested
        inner class CopyFile {

            @Test
            fun `should copy file if destination not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath(extension = ".txt")
                expectThat(getTestFile().copyTo(dest))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should create missing directories on missing destination parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath("missing").randomPath()
                expectThat(getTestFile().copyTo(dest))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should throw on existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomFile(extension = ".txt").writeText("old")
                expect {
                    catching { getTestFile().copyTo(dest) }.isFailure().isA<FileAlreadyExistsException>()
                    that(dest).hasContent("old")
                }
            }

            @Test
            fun `should throw on existing directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomDirectory().apply { randomFile().writeText("old") }
                expect {
                    catching { getTestFile().copyTo(dest) }.isFailure().isA<FileAlreadyExistsException>()
                    that(dest.listDirectoryEntries().single()).hasContent("old")
                }
            }

            @Test
            fun `should override existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomFile(extension = ".txt").writeText("old")
                expectThat(getTestFile().copyTo(dest, overwrite = true))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should override existing directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomDirectory().apply { tempFile().writeText("old") }
                expectThat(getTestFile().copyTo(dest, overwrite = true))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should not copy attributes by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath(extension = ".txt")
                expectThat(getTestFile().copyTo(dest)).get { age }.isLessThan(10.seconds)
            }

            @Test
            fun `should copy attributes if specified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath(extension = ".txt")
                expectThat(getTestFile().copyTo(dest, preserve = true)).get { age }.isGreaterThan(6.9.days).isLessThan(7.1.days)
            }
        }

        @Nested
        inner class CopyDirectory {

            @Test
            fun `should copy file if destination not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath(extension = ".txt")
                expectThat(getTestDir().copyTo(dest))
                    .isCopyOf(getTestDir())
                    .isEqualTo(dest)
            }

            @Test
            fun `should create missing directories on missing destination parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath().randomPath()
                expectThat(getTestDir().copyTo(dest))
                    .isCopyOf(getTestDir())
                    .isEqualTo(dest)
            }

            @Test
            fun `should throw on existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomFile(extension = ".txt").writeText("old")
                expect {
                    catching { getTestDir().copyTo(dest) }.isFailure().isA<FileAlreadyExistsException>()
                    that(dest).hasContent("old")
                }
            }

            @Test
            fun `should merge on existing directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomDirectory().apply { randomFile().writeText("old") }
                val alreadyExistingFile = dest.listDirectoryEntries().single()
                expectThat(getTestDir().copyTo(dest))
                    .containsAllFiles(getTestDir())
                    .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                    .isEqualTo(dest)
            }

            @Test
            fun `should override existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomFile(extension = ".txt").writeText("old")
                expectThat(getTestDir().copyTo(dest, overwrite = true))
                    .isCopyOf(getTestDir())
                    .isEqualTo(dest)
            }

            @Test
            fun `should merge on overwriting non-empty directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomDirectory().apply { randomFile().writeText("old") }
                val alreadyExistingFile = dest.listDirectoryEntries().single()
                expectThat(getTestDir().copyTo(dest, overwrite = true))
                    .containsAllFiles(getTestDir())
                    .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                    .isEqualTo(dest)
            }

            @Test
            fun `should not copy attributes by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath(extension = ".txt")
                expectThat(getTestDir().copyTo(dest)) {
                    get { listDirectoryEntriesRecursively() }.all {
                        get { age }.isLessThan(10.seconds)
                    }
                }
            }

            @Test
            fun `should copy attributes if specified for all but not-empty directories`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val dest = randomPath(extension = ".txt")
                expectThat(getTestDir().copyTo(dest, preserve = true)) {
                    get { listDirectoryEntriesRecursively().filter { !it.isDirectory() || it.isEmpty } }.all {
                        get { age }.isGreaterThan(6.9.days).isLessThan(7.1.days)
                    }
                }
            }
        }
    }


    @Nested
    inner class CopyToDirectoryKtTest {

        private fun Path.getTestFile() = randomFile(extension = ".txt").writeText("test file").apply { lastModified -= 7.days }
        private fun Path.getTestDir() = directoryWithTwoFiles().apply { listDirectoryEntriesRecursively().forEach { it.lastModified -= 7.days } }

        @Test
        fun `should throw on missing src`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val src = randomPath(extension = ".txt")
            expectCatching { src.copyToDirectory(getTestFile()) }.isFailure().isA<NoSuchFileException>()
        }

        @Nested
        inner class CopyFile {

            @Test
            fun `should copy file if destination not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomDirectory().resolve(srcFile.fileName)
                expectThat(srcFile.copyToDirectory(dest.parent))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should create missing directories on missing destination parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomPath("missing").resolve(srcFile.fileName)
                expectThat(srcFile.copyToDirectory(dest.parent))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should throw on existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomDirectory().resolve(srcFile.fileName).writeText("old")
                expect {
                    catching { srcFile.copyToDirectory(dest.parent) }.isFailure().isA<FileAlreadyExistsException>()
                    that(dest).hasContent("old")
                }
            }

            @Test
            fun `should throw on existing directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomDirectory().resolve(srcFile.fileName).createDirectories().apply { randomFile().writeText("old") }
                expect {
                    catching { srcFile.copyToDirectory(dest.parent) }.isFailure().isA<FileAlreadyExistsException>()
                    that(dest.listDirectoryEntries().single()).hasContent("old")
                }
            }

            @Test
            fun `should override existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomDirectory().resolve(srcFile.fileName).writeText("old")
                expectThat(srcFile.copyToDirectory(dest.parent, overwrite = true))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should override existing directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomDirectory().resolve(srcFile.fileName).createDirectories().apply { randomFile().writeText("old") }
                expectThat(srcFile.copyToDirectory(dest.parent, overwrite = true))
                    .hasContent("test file")
                    .isEqualTo(dest)
            }

            @Test
            fun `should not copy attributes by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomDirectory().resolve(srcFile.fileName)
                expectThat(srcFile.copyToDirectory(dest.parent))
                    .get { age }.isLessThan(10.seconds)
            }

            @Test
            fun `should copy attributes if specified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcFile = getTestFile()
                val dest = randomDirectory().resolve(srcFile.fileName)
                expectThat(srcFile.copyToDirectory(dest.parent, preserve = true))
                    .get { age }.isGreaterThan(6.9.days).isLessThan(7.1.days)
            }
        }

        @Nested
        inner class CopyDirectory {

            @Test
            fun `should copy file if destination not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomDirectory().resolve(srcDir.fileName)
                expectThat(srcDir.copyToDirectory(dest.parent))
                    .isCopyOf(srcDir)
                    .isEqualTo(dest)
            }

            @Test
            fun `should create missing directories on missing destination parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomPath().resolve(srcDir.fileName)
                expectThat(srcDir.copyToDirectory(dest.parent))
                    .isCopyOf(srcDir)
                    .isEqualTo(dest)
            }

            @Test
            fun `should throw on existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomDirectory().resolve(srcDir.fileName).writeText("old")
                expect {
                    catching { srcDir.copyToDirectory(dest.parent) }.isFailure().isA<FileAlreadyExistsException>()
                    that(dest).hasContent("old")
                }
            }

            @Test
            fun `should merge on existing directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomDirectory().resolve(srcDir.fileName).apply { randomFile().writeText("old") }
                val alreadyExistingFile = dest.listDirectoryEntries().single()
                expectThat(srcDir.copyToDirectory(dest.parent))
                    .containsAllFiles(srcDir)
                    .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                    .isEqualTo(dest)
            }

            @Test
            fun `should override existing file destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomDirectory().resolve(srcDir.fileName).writeText("old")
                expectThat(srcDir.copyToDirectory(dest.parent, overwrite = true))
                    .isCopyOf(srcDir)
                    .isEqualTo(dest)
            }

            @Test
            fun `should merge on overwriting non-empty directory destination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomDirectory().resolve(srcDir.fileName).apply { randomFile().writeText("old") }
                val alreadyExistingFile = dest.listDirectoryEntries().single()
                expectThat(srcDir.copyToDirectory(dest.parent, overwrite = true))
                    .containsAllFiles(srcDir)
                    .and { get { resolve(alreadyExistingFile.fileName) }.hasContent("old") }
                    .isEqualTo(dest)
            }

            @Test
            fun `should not copy attributes by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomDirectory().resolve(fileName)
                expectThat(srcDir.copyToDirectory(dest.parent)) {
                    get { listDirectoryEntriesRecursively() }.all {
                        get { age }.isLessThan(10.seconds)
                    }
                }
            }

            @Test
            fun `should copy attributes if specified for all but not-empty directories`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val srcDir = getTestDir()
                val dest = randomDirectory().resolve(fileName)
                expectThat(srcDir.copyToDirectory(dest.parent, preserve = true)) {
                    get { listDirectoryEntriesRecursively().filter { !it.isDirectory() || it.isEmpty } }.all {
                        get { age }.isGreaterThan(6.9.days).isLessThan(7.1.days)
                    }
                }
            }
        }
    }

    @Nested
    inner class Duplicate {

        @Test
        fun `should duplicate file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file-dir").singleFile().renameTo("file.ext")
            val copy = file.duplicate()
            expectThat(copy) {
                isCopyOf(file)
                hasSameFileName(file)
                isSiblingOf(file)
            }
        }

        @Test
        fun `should duplicate directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir-dir").directoryWithTwoFiles().renameTo("dir")
            val copy = dir.duplicate()
            expectThat(copy) {
                isCopyOf(dir)
                hasSameFileName(dir)
                isSiblingOf(dir)
            }
        }
    }

}


fun <T : Path> Assertion.Builder<T>.createsEqualTar(other: Path) =
    assert("is copy of $other") { self ->
        val selfTar = self.tar(tempFile()).deleteOnExit()
        val otherTar = other.tar(tempFile()).deleteOnExit()

        val selfBytes = selfTar.readBytes()
        val otherBytes = otherTar.readBytes()
        if (selfBytes.contentEquals(otherBytes)) pass()
        else fail("The resulting tarballs do not match. Expected size ${selfBytes.size} but was ${otherBytes.size}")
    }

fun <T : Path> Assertion.Builder<T>.isCopyOf(other: Path) =
    assert("is copy of $other") { self ->
        if (self.isRegularFile() && !other.isRegularFile()) fail("$self is a file and can only be compared to another file")
        else if (self.isDirectory() && !other.isDirectory()) fail("$self is a directory and can only be compared to another directory")
        else if (self.isDirectory()) {
            kotlin.runCatching {
                expectThat(self).hasSameFiles(other)
            }.exceptionOrNull()?.let { fail("Directories contained different files.") } ?: pass()
        } else {
            val selfBytes = self.readBytes()
            val otherBytes = other.readBytes()
            if (selfBytes.contentEquals(otherBytes)) pass()
            else fail("The resulting tarballs do not match. Expected size ${selfBytes.size} but was ${otherBytes.size}")
        }
    }

@Suppress("unused")
fun <T : Path> Assertion.Builder<T>.isDuplicateOf(expected: Path, order: Int = 1) {
    isCopyOf(expected)
    hasSameFileName(expected)
    isSiblingOf(expected, order)
}
