package com.bkahlert.kommons_deprecated.io.path

import com.bkahlert.kommons_deprecated.test.testEachOld
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import java.nio.file.Paths

class ExtensionsTest {

    @Nested
    inner class HasExtensions {

        private val path = Paths.get("path/file.something.ext")

        @Test
        fun `should match extension with ignoring case and leading period`() {
            path should {
                it.hasExtensions(".EXT") shouldBe true
                it.hasExtensions(".ext") shouldBe true
                it.hasExtensions("EXT") shouldBe true
                it.hasExtensions("ext") shouldBe true
            }
        }

        @Test
        fun `should match multiple extensions`() {
            path should {
                it.hasExtensions("something.EXT") shouldBe true
                it.hasExtensions(".something", "ext") shouldBe true
                it.hasExtensions("something.EXT") shouldBe true
                it.hasExtensions(".something.ext") shouldBe true
            }
        }

        @Test
        fun `should not match non extension`() {
            path should {
                it.hasExtensions("something") shouldBe false
                it.hasExtensions(".something") shouldBe false
            }
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
                    expectThat(Paths.get(path).addExtensions("test")).isEqualTo(Paths.get(expected))
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
                    expectThat(Paths.get(path).addExtensions("test.ext")).isEqualTo(Paths.get(expected))
                },
            )
        }
    }


    @Nested
    inner class ExtensionIndex {
        @TestFactory
        fun `should return index of extension`() = listOf(
            Paths.get("a/b/c.2") to 5,
            Paths.get("a/b.1/c.2") to 7
        ).testEachOld { (path, expected) ->
            expecting { path.extensionIndex } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return -1 if not in file name`() = listOf(
            Paths.get("a/b/c"),
            Paths.get("a/b.1/c")
        ).testEachOld {
            expecting { it.extensionIndex } that { isEqualTo(-1) }
        }
    }

    @Nested
    inner class ExtensionOrNullKtTest {
        @TestFactory
        fun `should return extension`() = listOf(
            Paths.get("a/b/c.2") to "2",
            Paths.get("a/b.1/c.2-1") to "2-1"
        ).testEachOld { (path, expected) ->
            expecting { path.extensionOrNull } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return null if not in file name`() = listOf(
            Paths.get("a/b/c"),
            Paths.get("a/b.1/c")
        ).testEachOld {
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
        ).testEachOld("* should be *") { (path, expected) ->
            expecting { Paths.get(path).withExtension("test") } that { isEqualTo(Paths.get(expected)) }
        }

        @TestFactory
        fun `should replace extension if one exists`() = listOf(
            "filename.pdf" to "filename.test",
            "my/path/filename.pdf" to "my/path/filename.test",
            "/my/path/filename.pdf" to "/my/path/filename.test",
        ).testEachOld("* should be *") { (path, expected) ->
            expecting { Paths.get(path).withExtension("test") } that { isEqualTo(Paths.get(expected)) }
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
        ).testEachOld("* should be filename.test") {
            expecting { Paths.get(it).fileNameWithExtension("test") } that { isEqualTo("filename.test") }
        }

        @TestFactory
        fun `should replace extension if one exists`() = listOf(
            "filename.pdf", "filename.test",
            "my/path/filename.pdf", "my/path/filename.test",
            "/my/path/filename.pdf", "/my/path/filename.test",
        ).testEachOld("* should be filename.test") {
            expecting { Paths.get(it).fileNameWithExtension("test") } that { isEqualTo("filename.test") }
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
                    expectThat(Paths.get(expected).removeExtensions("test")).isEqualTo(Paths.get(path))
                },

                DynamicTest.dynamicTest("removing extension \"baz\" from $expected should throw") {
                    expectCatching { Paths.get(expected).removeExtensions("baz") }.isFailure().isA<IllegalArgumentException>()
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
                    expectThat(Paths.get(expected).removeExtensions("test.ext")).isEqualTo(Paths.get(path))
                },

                DynamicTest.dynamicTest("removing extension \"baz.ext\" from $expected should throw") {
                    expectCatching { Paths.get(expected).removeExtensions("baz.ext") }.isFailure().isA<IllegalArgumentException>()
                },
            )
        }
    }

    @Nested
    inner class BaseName {
        @TestFactory
        fun `should return only the file name without extension`() = listOf(
            Paths.get("a/b/c.2"),
            Paths.get("a/b.1/c.2"),
        ).testEachOld {
            expecting { it.baseName } that { isEqualTo(it.fileSystem.getPath("c")) }
        }

        @TestFactory
        fun `should return only the file name even if its already missing`() = listOf(
            Paths.get("a/b/c"),
            Paths.get("a/b.1/c"),
        ).testEachOld {
            expecting { it.baseName } that { isEqualTo(it.fileName) }
        }
    }

    @Nested
    inner class BasePath {
        @TestFactory
        fun `should return all but the extension`() = listOf(
            Paths.get("a/b/c.2") to Paths.get("a/b/c"),
            Paths.get("a/b.1/c.2") to Paths.get("a/b.1/c")
        ).testEachOld { (path, expected) ->
            expecting { path.basePath } that { isEqualTo(expected) }
        }

        @TestFactory
        fun `should return same path if no extension`() = listOf(
            Paths.get("a/b/c"),
            Paths.get("a/b.1/c")
        ).testEachOld {
            expecting { it.basePath } that { isSameInstanceAs(it) }
        }
    }
}
