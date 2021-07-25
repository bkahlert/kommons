package koodies.io

import koodies.Koodies
import koodies.io.path.deleteOnExit
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.tests
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.size
import strikt.java.fileName
import strikt.java.parent

class ClassPathTest {

    @Nested
    inner class DynamicClassPathDirectory {

        @TestFactory
        fun `should have name`() = testEach(ClassPathDirectory("META-INF"), META_INF) {
            expecting { name } that { isEqualTo("META-INF") }
        }

        @TestFactory
        fun `should override toString`() = testEach(ClassPathDirectory("META-INF"), META_INF) {
            expecting { it.toString() } that { isEqualTo("META-INF") }
        }

        @Test
        fun `should throw if not exist`() {
            expectThrows<Throwable> { ClassPathDirectory("I dont exist") }
        }
    }


    @Nested
    inner class DynamicClassPathFile {

        private val dynamicClassPathFile = ClassPathDirectory("META-INF").dir("services").file("org.junit.jupiter.api.extension.Extension")
        private val staticClassPathFile = META_INF.Services.JUnitExtensions

        @TestFactory
        fun `should have name`() = testEach(dynamicClassPathFile, staticClassPathFile) {
            expecting { it.name } that { isEqualTo("org.junit.jupiter.api.extension.Extension") }
        }

        @TestFactory
        fun `should override toString`() = testEach(dynamicClassPathFile, staticClassPathFile) {
            expecting { it.toString() } that { isEqualTo("META-INF/services/org.junit.jupiter.api.extension.Extension") }
        }

        @TestFactory
        fun `should have data`() = testEach(dynamicClassPathFile, staticClassPathFile) {
            expecting { data.decodeToString() } that { contains("koodies.junit.TestNameResolver") }
            expecting { data.size } that { isGreaterThan(10) }
        }

        @TestFactory
        fun `should nest`() = tests {
            expecting { META_INF.dir("services").file("org.junit.jupiter.api.extension.Extension") } that {
                toStringIsEqualTo("META-INF/services/org.junit.jupiter.api.extension.Extension")
            }

            expecting { resources.a.b.c } that { toStringIsEqualTo("a/b/c") }
            expecting { resources.a.b.c.file } that { toStringIsEqualTo("a/b/c/file") }
        }

        @Test
        fun `should throw if not exist`() {
            expectThrows<Throwable> { META_INF.file("I dont exist") }
        }
    }

    @Nested
    inner class CopyFile {

        @Test
        fun `should copy file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { META_INF.Services.JUnitExtensions.copyTo(resolve("test.txt")) } that {
                fileName.toStringIsEqualTo("test.txt")
                textContent.contains("koodies.junit.TestNameResolver")
            }
        }

        @Test
        fun `should copy file to directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { META_INF.Services.JUnitExtensions.copyToDirectory(this) } that {
                fileName.toStringIsEqualTo("org.junit.jupiter.api.extension.Extension")
                textContent.contains("koodies.junit.TestNameResolver")
            }
        }

        @Test
        fun `should copy file to temp`() {
            expecting { META_INF.Services.JUnitExtensions.copyToTemp().deleteOnExit() } that {
                parent.isEqualTo(Koodies.FilesTemp)
                textContent.contains("koodies.junit.TestNameResolver")
            }
        }
    }

    @Nested
    inner class CopyDirectory {

        @Test
        fun `should copy directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { META_INF.Services.copyTo(resolve("srv")) } that {
                fileName.toStringIsEqualTo("srv")
                get { listDirectoryEntriesRecursively() }.size.isGreaterThanOrEqualTo(2)
            }
        }

        @Test
        fun `should copy directory to directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { META_INF.Services.copyToDirectory(this) } that {
                fileName.toStringIsEqualTo("services")
                get { listDirectoryEntriesRecursively() }.size.isGreaterThanOrEqualTo(2)
            }
        }

        @Test
        fun `should copy directory to temp`() {
            expecting { META_INF.Services.copyToTemp().deleteOnExit(recursively = true) } that {
                parent.isEqualTo(Koodies.FilesTemp)
                get { listDirectoryEntriesRecursively() }.size.isGreaterThanOrEqualTo(2)
            }
        }
    }

    private object resources : ClassPathDirectory("") {
        object a : Dir("a") {
            object b : Dir("b") {
                object c : Dir("c") {
                    object file : File("file")
                }
            }
        }
    }

    private object META_INF : ClassPathDirectory("META-INF") {
        object Services : Dir("services") {
            object JUnitExtensions : File("org.junit.jupiter.api.extension.Extension")
        }
    }
}
