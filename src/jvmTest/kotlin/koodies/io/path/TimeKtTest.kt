package koodies.io.path

import koodies.test.expectThrows
import koodies.test.junit.UniqueId
import koodies.test.withTempDir
import koodies.time.Now
import koodies.time.days
import koodies.time.minus
import koodies.time.minutes
import koodies.time.seconds
import koodies.time.toFileTime
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.java.exists
import java.nio.file.NoSuchFileException
import java.nio.file.attribute.FileTime
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile

class TimeKtTest {

    @Nested
    inner class Touch {

        @Test
        fun `should update last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file").apply {
                createFile()
                lastModified -= 1.days
            }

            file.touch()

            expectThat(Now.fileTime.toMillis() - file.lastModified.toMillis()).isLessThan(5000)
        }

        @Test
        fun `should create file if missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file")
            file.touch()
            expectThat(file).exists()
        }
    }

    @Nested
    inner class Age {

        @Test
        fun `should return age of file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file").apply {
                createFile()
                lastModified -= 1.days
            }

            expectThat(file.age) {
                isGreaterThanOrEqualTo(1.days - 5.seconds)
                isLessThanOrEqualTo(1.days + 5.seconds)
            }
        }

        @Test
        fun `should return age of directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dir = resolve("dir").apply {
                createDirectory()
                lastModified -= 1.days
            }

            expectThat(dir.age) {
                isGreaterThanOrEqualTo(1.days - 5.seconds)
                isLessThanOrEqualTo(1.days + 5.seconds)
            }
        }

        @Test
        fun `should throw if missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("file")

            expectThrows<NoSuchFileException> { file.age }
        }
    }

    @Nested
    inner class CreatedKtTest {

        @Test
        fun `should read created`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomFile().created.toInstant())
                .isLessThan(Now + 1.minutes)
                .isGreaterThan(Now - 1.minutes)
        }

        @Test
        fun `should write created`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = randomFile()
            file.created = (Now - 20.minutes).toFileTime()
            expectThat(file.created)
                .isLessThan((Now + 21.minutes).toFileTime())
                .isGreaterThan((Now - 21.minutes).toFileTime())
        }
    }

    @Nested
    inner class LastAccessedKtTest {

        @Test
        fun `should read last accessed`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomFile().lastAccessed.toInstant())
                .isLessThan(Now + 1.minutes)
                .isGreaterThan(Now - 1.minutes)
        }

        @Test
        fun `should write last accessed`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = randomFile()
            file.lastAccessed = FileTime.from(Now - 20.minutes)
            expectThat(file.lastAccessed.toInstant())
                .isLessThan(Now + 21.minutes)
                .isGreaterThan(Now - 21.minutes)
        }
    }

    @Nested
    inner class LastModifiedKtTest {

        @Test
        fun `should read last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomFile().lastModified.toInstant())
                .isLessThan(Now + 1.minutes)
                .isGreaterThan(Now - 1.minutes)
        }

        @Test
        fun `should write last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = randomFile()
            file.lastModified = FileTime.from(Now - 20.minutes)
            expectThat(file.lastModified.toInstant())
                .isLessThan(Now + 21.minutes)
                .isGreaterThan(Now - 21.minutes)
        }
    }
}
