package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.createTempDirectory
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.delete
import com.bkahlert.kommons.io.copyToDirectory
import com.bkahlert.kommons.test.HtmlFixture
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class ConditionsKtTest {

    @Nested
    inner class IsEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should return true on empty`(@TempDir tempDir: Path) {
                expectThat(tempDir.createTempFile()).isEmpty()
            }

            @Test
            fun `should return false on non-empty`(@TempDir tempDir: Path) {
                expectThat(HtmlFixture.copyToDirectory(tempDir)).not { isEmpty() }
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should return true on empty`(@TempDir tempDir: Path) {
                expectThat(tempDir.createTempDirectory()).isEmpty()
            }

            @Test
            fun `should return false on non-empty`(@TempDir tempDir: Path) {
                expectThat(tempDir.createTempDirectory().parent).not { isEmpty() }
            }
        }

        @Test
        fun `should throw on missing`(@TempDir tempDir: Path) {
            expectCatching {
                tempDir.createTempFile().apply { delete() }.isEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`(@TempDir tempDir: Path) {
            @Suppress("BlockingMethodInNonBlockingContext")
            (expectCatching {
                Files.createSymbolicLink(tempDir.createTempFile().apply { delete() }, tempDir.createTempFile()).isEmpty()
            })
        }
    }


    @Nested
    inner class IsNotEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should return true on non-empty`(@TempDir tempDir: Path) {
                expectThat(HtmlFixture.copyToDirectory(tempDir)).isNotEmpty()
            }

            @Test
            fun `should return false on empty`(@TempDir tempDir: Path) {
                expectThat(tempDir).not { isNotEmpty() }
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should return true on non-empty`(@TempDir tempDir: Path) {
                expectThat(tempDir.createTempDirectory().parent).isNotEmpty()
            }

            @Test
            fun `should return false on empty`(@TempDir tempDir: Path) {
                expectThat(tempDir.createTempDirectory()).not { isNotEmpty() }
            }
        }

        @Test
        fun `should throw on missing`(@TempDir tempDir: Path) {
            expectCatching {
                tempDir.createTempFile().apply { delete() }.isNotEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw on different type`(@TempDir tempDir: Path) {
            @Suppress("BlockingMethodInNonBlockingContext")
            expectCatching {
                Files.createSymbolicLink(tempDir.createTempFile().apply { delete() }, tempDir.createTempFile()).isNotEmpty()
            }
        }
    }
}


fun <T : Path> Assertion.Builder<T>.isEmpty() =
    assert("is empty") {
        when (it.isEmpty()) {
            true -> pass()
            else -> fail("was not empty")
        }
    }


fun <T : Path> Assertion.Builder<T>.isNotEmpty() =
    assert("is not empty") {
        when (it.isNotEmpty()) {
            true -> pass()
            else -> fail("was empty")
        }
    }
