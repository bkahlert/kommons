package koodies.io.path

import koodies.io.copyToDirectory
import koodies.io.randomDirectory
import koodies.io.randomFile
import koodies.io.randomPath
import koodies.junit.UniqueId
import koodies.test.HtmlFixture
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isSuccess
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException

class RequireKtTest {

    @Nested
    inner class RequireDirectoryKtTest {

        @Test
        fun `should throw on file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomFile().requireDirectory() }.isFailure().isA<NotDirectoryException>()
        }

        @Test
        fun `should not throw on directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            randomDirectory().requireDirectory()
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomPath().requireDirectory() }.isFailure().isA<NotDirectoryException>()
        }
    }

    @Nested
    inner class RequireEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should not throw on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                randomFile().requireEmpty()
            }

            @Test
            fun `should throw on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { HtmlFixture.copyToDirectory(this).requireEmpty() }.isFailure()
                    .isA<FileAlreadyExistsException>()
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should not throw on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                randomDirectory().requireEmpty()
            }

            @Test
            fun `should throw on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { randomDirectory().parent.requireEmpty() }.isFailure().isA<DirectoryNotEmptyException>()
            }
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomPath().requireEmpty() }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            @Suppress("BlockingMethodInNonBlockingContext")
            (expectCatching {
                Files.createSymbolicLink(randomPath(), randomFile()).requireEmpty()
            })
        }
    }

    @Nested
    inner class RequireExistsKtTest {

        @Test
        fun `should not throw if exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            randomDirectory().requireExists()
        }

        @Test
        fun `should throw if not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomPath().requireExists() }
                .isFailure().isA<NoSuchFileException>()
        }
    }

    @Nested
    inner class RequireExistsNotKtTest {

        @Test
        fun `should throw if exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomDirectory().requireExistsNot() }.isFailure().isA<FileAlreadyExistsException>()
        }

        @Test
        fun `should not throw if not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            randomPath().requireExistsNot()
        }
    }

    @Nested
    inner class RequireFileKtTest {

        @Test
        fun `should throw on directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomDirectory().requireFile() }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            randomFile().requireFile()
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { randomPath().requireFile() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class RequireNotEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should throw on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { randomFile().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
            }

            @Test
            fun `should not throw on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { HtmlFixture.copyToDirectory(randomDirectory()).requireNotEmpty() }.isSuccess()
            }
        }

        @Nested
        inner class WithDirectory {

            @Test
            fun `should throw on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { randomDirectory().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
            }

            @Test
            fun `should not throw on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                randomDirectory().parent.requireNotEmpty()
            }
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching {
                randomDirectory().randomPath().requireNotEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            @Suppress("BlockingMethodInNonBlockingContext")
            expectCatching { Files.createSymbolicLink(randomDirectory().randomPath(), randomFile()).requireNotEmpty() }
                .isFailure().isA<NoSuchFileException>()
        }
    }
}
