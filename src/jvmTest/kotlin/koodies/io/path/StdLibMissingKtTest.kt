package koodies.io.path

import koodies.test.Fixtures.directoryWithTwoFiles
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.NotDirectoryException

@Execution(CONCURRENT)
class StdLibMissingKtTest {

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
