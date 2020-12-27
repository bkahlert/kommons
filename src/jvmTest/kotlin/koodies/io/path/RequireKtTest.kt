package koodies.io.path

import koodies.test.Fixtures.copyToDirectory
import koodies.test.HtmlFile
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isSuccess
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException

@Execution(CONCURRENT)
class RequireKtTest {

    @Nested
    inner class RequireDirectoryKtTest {

        @Test
        fun `should throw on file`() = withTempDir {
            expectCatching { randomFile().requireDirectory() }.isFailure().isA<NotDirectoryException>()
        }

        @Test
        fun `should not throw on directory`() = withTempDir {
            randomDirectory().requireDirectory()
        }

        @Test
        fun `should throw on missing`() = withTempDir {
            expectCatching { randomPath().requireDirectory() }.isFailure().isA<NotDirectoryException>()
        }
    }

    @Nested
    inner class RequireEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should not throw on empty`() = withTempDir {
                randomFile().requireEmpty()
            }

            @Test
            fun `should throw on non-empty`() = withTempDir {
                expectCatching { HtmlFile.copyToDirectory(this).requireEmpty() }.isFailure()
                    .isA<FileAlreadyExistsException>()
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should not throw on empty`() = withTempDir {
                randomDirectory().requireEmpty()
            }

            @Test
            fun `should throw on non-empty`() = withTempDir {
                expectCatching { randomDirectory().parent.requireEmpty() }.isFailure().isA<DirectoryNotEmptyException>()
            }
        }

        @Test
        fun `should throw on missing`() = withTempDir {
            expectCatching { randomPath().requireEmpty() }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`() = withTempDir {
            @Suppress("BlockingMethodInNonBlockingContext")
            (expectCatching {
                Files.createSymbolicLink(randomPath(), randomFile()).requireEmpty()
            })
        }
    }

    @Nested
    inner class RequireExistsKtTest {

        @Test
        fun `should not throw if exists`() = withTempDir {
            randomDirectory().requireExists()
        }

        @Test
        fun `should throw if not exists`() = withTempDir {
            expectCatching { randomPath().requireExists() }
                .isFailure().isA<NoSuchFileException>()
        }
    }

    @Nested
    inner class RequireExistsNotKtTest {

        @Test
        fun `should throw if exists`() = withTempDir {
            expectCatching { randomDirectory().requireExistsNot() }.isFailure().isA<FileAlreadyExistsException>()
        }

        @Test
        fun `should not throw if not exists`() = withTempDir {
            randomPath().requireExistsNot()
        }
    }

    @Nested
    inner class RequireFileKtTest {

        @Test
        fun `should throw on directory`() = withTempDir {
            expectCatching { randomDirectory().requireFile() }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on file`() = withTempDir {
            randomFile().requireFile()
        }

        @Test
        fun `should throw on missing`() = withTempDir {
            expectCatching { randomPath().requireFile() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class RequireNotEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should throw on empty`() = withTempDir {
                expectCatching { randomFile().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
            }

            @Test
            fun `should not throw on non-empty`() = withTempDir {
                expectCatching { HtmlFile.copyToDirectory(randomDirectory()).requireNotEmpty() }.isSuccess()
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should throw on empty`() = withTempDir {
                expectCatching { randomDirectory().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
            }

            @Test
            fun `should not throw on non-empty`() = withTempDir {
                randomDirectory().parent.requireNotEmpty()
            }
        }

        @Test
        fun `should throw on missing`() = withTempDir {
            expectCatching {
                randomDirectory().randomPath().requireNotEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`() = withTempDir {
            @Suppress("BlockingMethodInNonBlockingContext")
            expectCatching {
                Files.createSymbolicLink(randomDirectory().randomPath(), randomFile()).requireNotEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }
    }
}
