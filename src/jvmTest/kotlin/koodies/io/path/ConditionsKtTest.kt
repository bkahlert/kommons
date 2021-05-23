package koodies.io.path

import koodies.io.copyToDirectory
import koodies.test.HtmlFixture
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
            fun `should return true on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(randomFile()).isEmpty()
            }

            @Test
            fun `should return false on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(HtmlFixture.copyToDirectory(this)).not { isEmpty() }
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should return true on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(randomDirectory()).isEmpty()
            }

            @Test
            fun `should return false on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(randomDirectory().parent).not { isEmpty() }
            }
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching {
                randomPath().isEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            @Suppress("BlockingMethodInNonBlockingContext")
            (expectCatching {
                Files.createSymbolicLink(randomPath(), randomFile()).isEmpty()
            })
        }
    }


    @Nested
    inner class IsNotEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should return true on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(HtmlFixture.copyToDirectory(this)).isNotEmpty()
            }

            @Test
            fun `should return false on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(this).not { isNotEmpty() }
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should return true on non-empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(randomDirectory().parent).isNotEmpty()
            }

            @Test
            fun `should return false on empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(randomDirectory()).not { isNotEmpty() }
            }
        }

        @Test
        fun `should throw on missing`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching {
                randomPath().isNotEmpty()
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw on different type`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            @Suppress("BlockingMethodInNonBlockingContext")
            expectCatching {
                Files.createSymbolicLink(randomPath(), randomFile()).isNotEmpty()
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
