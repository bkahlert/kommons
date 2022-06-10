package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.createTempDirectory
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.io.copyToDirectory
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.withTempDir
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
            expectCatching { createTempFile().requireDirectory() }.isFailure().isA<NotDirectoryException>()
        }

        @Test
        fun `should not throw on directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            createTempDirectory().requireDirectory()
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { resolve("path").requireDirectory() }.isFailure().isA<NotDirectoryException>()
        }
    }

    @Nested
    inner class RequireEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should not throw on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                createTempFile().requireEmpty()
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
                createTempDirectory().requireEmpty()
            }

            @Test
            fun `should throw on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { createTempDirectory().parent.requireEmpty() }.isFailure().isA<DirectoryNotEmptyException>()
            }
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { resolve("path").requireEmpty() }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            @Suppress("BlockingMethodInNonBlockingContext")
            (expectCatching {
                Files.createSymbolicLink(resolve("path"), createTempFile()).requireEmpty()
            })
        }
    }

    @Nested
    inner class RequireExistsKtTest {

        @Test
        fun `should not throw if exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            createTempDirectory().requireExists()
        }

        @Test
        fun `should throw if not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { resolve("path").requireExists() }
                .isFailure().isA<NoSuchFileException>()
        }
    }

    @Nested
    inner class RequireExistsNotKtTest {

        @Test
        fun `should throw if exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { createTempDirectory().requireExistsNot() }.isFailure().isA<FileAlreadyExistsException>()
        }

        @Test
        fun `should not throw if not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            resolve("path").requireExistsNot()
        }
    }

    @Nested
    inner class RequireFileKtTest {

        @Test
        fun `should throw on directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { createTempDirectory().requireFile() }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            createTempFile().requireFile()
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching { resolve("path").requireFile() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class RequireNotEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should throw on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { createTempFile().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
            }

            @Test
            fun `should not throw on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { HtmlFixture.copyToDirectory(createTempDirectory()).requireNotEmpty() }.isSuccess()
            }
        }

        @Nested
        inner class WithDirectory {

            @Test
            fun `should throw on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectCatching { createTempDirectory().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
            }

            @Test
            fun `should not throw on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                createTempDirectory().parent.requireNotEmpty()
            }
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching {
                createTempDirectory().resolve("path").requireNotEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            @Suppress("BlockingMethodInNonBlockingContext")
            expectCatching { Files.createSymbolicLink(createTempDirectory().resolve("path"), createTempFile()).requireNotEmpty() }
                .isFailure().isA<NoSuchFileException>()
        }
    }
}
