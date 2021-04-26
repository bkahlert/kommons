package koodies.io.path

import koodies.test.testEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.nio.file.Path

@Execution(CONCURRENT)
class ExtensionsTest {

    @Nested
    inner class HasExtensions {

        private val path = "path/file.something.ext".asPath()

        @Test
        fun `should match extension with ignoring case and leading period`() {
            expectThat(path)
                .hasExtension(".EXT")
                .hasExtension(".ext")
                .hasExtension("EXT")
                .hasExtension("ext")
        }

        @Test
        fun `should match multiple extensions`() {
            expectThat(path)
                .hasExtension("something", ".EXT")
                .hasExtension(".something", "ext")
                .hasExtension("something.EXT")
                .hasExtension(".something.ext")
        }

        @Test
        fun `should not match non extension`() {
            expectThat(path)
                .not { hasExtension("something") }
                .not { hasExtension(".something") }
        }
    }


    @Nested
    inner class AddExtensions {

        @TestFactory
        fun `should append single extension`() = listOf(
            "filename" to "filename.test",
            "my/path/filename" to "my/path/filename.test",
            "/my/path/filename" to "/my/path/filename.test",
            "filename.foo" to "filename.foo.test",
            "my/path/filename.foo" to "my/path/filename.foo.test",
            "/my/path/filename.foo" to "/my/path/filename.foo.test",
        ).flatMap { (path, expected) ->
            listOf(
                DynamicTest.dynamicTest("$path with appended extension \"test\" should be $expected") {
                    expectThat(Path.of(path).addExtensions("test")).isEqualTo(Path.of(expected))
                },
            )
        }

        @TestFactory
        fun `should append multiple extensions`() = listOf(
            "filename" to "filename.test.ext",
            "my/path/filename" to "my/path/filename.test.ext",
            "/my/path/filename" to "/my/path/filename.test.ext",
            "filename.foo" to "filename.foo.test.ext",
            "my/path/filename.foo" to "my/path/filename.foo.test.ext",
            "/my/path/filename.foo" to "/my/path/filename.foo.test.ext",
        ).flatMap { (path, expected) ->
            listOf(
                DynamicTest.dynamicTest("$path with appended extension \"test.ext\" should be $expected") {
                    expectThat(Path.of(path).addExtensions("test.ext")).isEqualTo(Path.of(expected))
                },
            )
        }
    }


    @Nested
    inner class ExtensionIndex {
        @TestFactory
        fun `should return index of extension`() = listOf(
            Path.of("a/b/c.2") to 5,
            Path.of("a/b.1/c.2") to 7
        ).testEach { (path, expected) ->
            expecting { path.extensionIndex } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return -1 if not in file name`() = listOf(
            Path.of("a/b/c"),
            Path.of("a/b.1/c")
        ).testEach {
            expecting { it.extensionIndex } that { isEqualTo(-1) }
        }
    }

    @Nested
    inner class ExtensionOrNullKtTest {
        @TestFactory
        fun `should return extension`() = listOf(
            Path.of("a/b/c.2") to "2",
            Path.of("a/b.1/c.2-1") to "2-1"
        ).testEach { (path, expected) ->
            expecting { path.extensionOrNull } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return null if not in file name`() = listOf(
            Path.of("a/b/c"),
            Path.of("a/b.1/c")
        ).testEach {
            expecting { it.extensionOrNull } that { isNull() }
        }
    }

    @Nested
    inner class WithExtensions {

        @TestFactory
        fun `should add extension if none exists`() = listOf(
            "filename" to "filename.test",
            "my/path/filename" to "my/path/filename.test",
            "/my/path/filename" to "/my/path/filename.test",
        ).testEach("{} should be {}") { (path, expected) ->
            expecting { Path.of(path).withExtension("test") } that { isEqualTo(Path.of(expected)) }
        }

        @TestFactory
        fun `should replace extension if one exists`() = listOf(
            "filename.pdf" to "filename.test",
            "my/path/filename.pdf" to "my/path/filename.test",
            "/my/path/filename.pdf" to "/my/path/filename.test",
        ).testEach("{} should be {}") { (path, expected) ->
            expecting { Path.of(path).withExtension("test") } that { isEqualTo(Path.of(expected)) }
        }
    }

    @Nested
    inner class FileNameWithExtension {

        @TestFactory
        fun `should add extension if none exists`() = listOf(
            "filename", "filename.test",
            "my/path/filename", "my/path/filename.test",
            "/my/path/filename", "/my/path/filename.test",
            "/my/path/filename.pdf", "/my/path/filename.test",
        ).testEach("{} should be filename.test") {
            expecting { Path.of(it).fileNameWithExtension("test") } that { isEqualTo("filename.test") }
        }

        @TestFactory
        fun `should replace extension if one exists`() = listOf(
            "filename.pdf", "filename.test",
            "my/path/filename.pdf", "my/path/filename.test",
            "/my/path/filename.pdf", "/my/path/filename.test",
        ).testEach("{} should be filename.test") {
            expecting { Path.of(it).fileNameWithExtension("test") } that { isEqualTo("filename.test") }
        }
    }


    @Nested
    inner class RemoveExtensions {

        @TestFactory
        fun `should remove single extension`() = listOf(
            "filename" to "filename.test",
            "my/path/filename" to "my/path/filename.test",
            "/my/path/filename" to "/my/path/filename.test",
            "filename.foo" to "filename.foo.test",
            "my/path/filename.foo" to "my/path/filename.foo.test",
            "/my/path/filename.foo" to "/my/path/filename.foo.test",
        ).flatMap { (path, expected) ->
            listOf(
                DynamicTest.dynamicTest("$expected with extension \"test\" removed should be $path") {
                    expectThat(Path.of(expected).removeExtensions("test")).isEqualTo(Path.of(path))
                },

                DynamicTest.dynamicTest("removing extension \"baz\" from $expected should throw") {
                    expectCatching { Path.of(expected).removeExtensions("baz") }.isFailure().isA<IllegalArgumentException>()
                },
            )
        }

        @TestFactory
        fun `should remove multiple extensions`() = listOf(
            "filename" to "filename.test.ext",
            "my/path/filename" to "my/path/filename.test.ext",
            "/my/path/filename" to "/my/path/filename.test.ext",
            "filename.foo" to "filename.foo.test.ext",
            "my/path/filename.foo" to "my/path/filename.foo.test.ext",
            "/my/path/filename.foo" to "/my/path/filename.foo.test.ext",
        ).flatMap { (path, expected) ->
            listOf(
                DynamicTest.dynamicTest("$expected with extension \"test.ext\" removed should be $path") {
                    expectThat(Path.of(expected).removeExtensions("test.ext")).isEqualTo(Path.of(path))
                },

                DynamicTest.dynamicTest("removing extension \"baz.ext\" from $expected should throw") {
                    expectCatching { Path.of(expected).removeExtensions("baz.ext") }.isFailure().isA<IllegalArgumentException>()
                },
            )
        }
    }

    @Nested
    inner class BaseName {
        @TestFactory
        fun `should return only the file name without extension`() = listOf(
            Path.of("a/b/c.2"),
            Path.of("a/b.1/c.2"),
        ).testEach {
            expecting { it.baseName } that { isEqualTo(it.fileSystem.getPath("c")) }
        }

        @TestFactory
        fun `should return only the file name even if its already missing`() = listOf(
            Path.of("a/b/c"),
            Path.of("a/b.1/c"),
        ).testEach {
            expecting { it.baseName } that { isEqualTo(it.fileName) }
        }
    }

    @Nested
    inner class BasePath {
        @TestFactory
        fun `should return all but the extension`() = listOf(
            Path.of("a/b/c.2") to Path.of("a/b/c"),
            Path.of("a/b.1/c.2") to Path.of("a/b.1/c")
        ).testEach { (path, expected) ->
            expecting { path.basePath } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return same path if no extension`() = listOf(
            Path.of("a/b/c"),
            Path.of("a/b.1/c")
        ).testEach {
            expecting { it.basePath } that { isSameInstanceAs(it) }
        }
    }
}


fun <T : Path> Assertion.Builder<T>.hasExtension(extension: String, vararg more: String) =
    assert("has extension(s) %s") {
        when (it.extensions.hasExtension(extension, *more)) {
            true -> pass()
            else -> fail("has extension ${it.extensionOrNull}")
        }
    }
