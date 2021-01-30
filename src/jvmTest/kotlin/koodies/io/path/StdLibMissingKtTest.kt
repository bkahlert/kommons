package koodies.io.path

import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.HtmlFile
import koodies.test.UniqueId
import koodies.test.copyToDirectory
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import java.io.IOException
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException

@Execution(CONCURRENT)
class StdLibMissingKtTest {

    @Nested
    inner class ReadLine {

        @Test
        fun `should read line`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = HtmlFile.copyToDirectory(this)
            expectThat(file.readLine(6)).isEqualTo("<p>Hello World!</p>")
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
}
