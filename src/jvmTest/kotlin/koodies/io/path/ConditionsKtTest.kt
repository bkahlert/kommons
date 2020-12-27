package koodies.io.path

import koodies.test.Fixtures.copyToDirectory
import koodies.test.HtmlFile
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

@Execution(CONCURRENT)
class ConditionsKtTest {

    @Nested
    inner class IsEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should return true on empty`() = withTempDir {
                expectThat(randomFile()).isEmpty()
            }

            @Test
            fun `should return false on non-empty`() = withTempDir {
                expectThat(HtmlFile.copyToDirectory(this)).not { isEmpty() }
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should return true on empty`() = withTempDir {
                expectThat(randomDirectory()).isEmpty()
            }

            @Test
            fun `should return false on non-empty`() = withTempDir {
                expectThat(randomDirectory().parent).not { isEmpty() }
            }
        }

        @Test
        fun `should throw on missing`() = withTempDir {
            expectCatching {
                randomPath().isEmpty
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw in different type`() = withTempDir {
            @Suppress("BlockingMethodInNonBlockingContext")
            (expectCatching {
                Files.createSymbolicLink(randomPath(), randomFile()).isEmpty
            })
        }
    }


    @Nested
    inner class IsNotEmptyKtTest {

        @Nested
        inner class WithFile {
            @Test
            fun `should return true on non-empty`() = withTempDir {
                expectThat(HtmlFile.copyToDirectory(this)).isNotEmpty()
            }

            @Test
            fun `should return false on empty`() = withTempDir {
                expectThat(this).not { isNotEmpty() }
            }
        }

        @Nested
        inner class WithDirectory {
            @Test
            fun `should return true on non-empty`() = withTempDir {
                expectThat(randomDirectory().parent).isNotEmpty()
            }

            @Test
            fun `should return false on empty`() = withTempDir {
                expectThat(randomDirectory()).not { isNotEmpty() }
            }
        }

        @Test
        fun `should throw on missing`() = withTempDir {
            expectCatching {
                randomPath().isNotEmpty
            }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should throw on different type`() = withTempDir {
            @Suppress("BlockingMethodInNonBlockingContext")
            expectCatching {
                Files.createSymbolicLink(randomPath(), randomFile()).isNotEmpty
            }
        }
    }
}


fun <T : Path> Assertion.Builder<T>.isEmpty() =
    assert("is empty") {
        when (it.isEmpty) {
            true -> pass()
            else -> fail("was not empty")
        }
    }


fun <T : Path> Assertion.Builder<T>.isNotEmpty() =
    assert("is not empty") {
        when (it.isNotEmpty) {
            true -> pass()
            else -> fail("was empty")
        }
    }

