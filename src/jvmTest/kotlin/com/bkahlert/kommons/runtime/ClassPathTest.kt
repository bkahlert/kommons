package com.bkahlert.kommons.runtime

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.io.path.deleteOnExit
import com.bkahlert.kommons.io.path.listDirectoryEntriesRecursively
import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.runtime.ClassPathTest.META_INF.Services
import com.bkahlert.kommons.runtime.ClassPathTest.META_INF.Services.JUnitExtensions
import com.bkahlert.kommons.runtime.ClassPathTest.resources.a.b.c
import com.bkahlert.kommons.runtime.ClassPathTest.resources.a.b.c.file
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.tests
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.test.withTempDir
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
        private val staticClassPathFile = JUnitExtensions

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
            expecting { data.decodeToString() } that { contains("com.bkahlert.kommons.test.junit.TestNameResolver") }
            expecting { data.size } that { isGreaterThan(10) }
        }

        @TestFactory
        fun `should nest`() = tests {
            expecting { META_INF.dir("services").file("org.junit.jupiter.api.extension.Extension") } that {
                toStringIsEqualTo("META-INF/services/org.junit.jupiter.api.extension.Extension")
            }

            expecting { c } that { toStringIsEqualTo("a/b/c") }
            expecting { file } that { toStringIsEqualTo("a/b/c/file") }
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
            expecting { JUnitExtensions.copyTo(resolve("test.txt")) } that {
                fileName.toStringIsEqualTo("test.txt")
                textContent.contains("com.bkahlert.kommons.test.junit.TestNameResolver")
            }
        }

        @Test
        fun `should copy file to directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { JUnitExtensions.copyToDirectory(this) } that {
                fileName.toStringIsEqualTo("org.junit.jupiter.api.extension.Extension")
                textContent.contains("com.bkahlert.kommons.test.junit.TestNameResolver")
            }
        }

        @Test
        fun `should copy file to temp`() {
            expecting { JUnitExtensions.copyToTemp().deleteOnExit() } that {
                parent.isEqualTo(Kommons.filesTemp)
                textContent.contains("com.bkahlert.kommons.test.junit.TestNameResolver")
            }
        }
    }

    @Nested
    inner class CopyDirectory {

        @Test
        fun `should copy directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { Services.copyTo(resolve("srv")) } that {
                fileName.toStringIsEqualTo("srv")
                get { listDirectoryEntriesRecursively() }.size.isGreaterThanOrEqualTo(2)
            }
        }

        @Test
        fun `should copy directory to directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { Services.copyToDirectory(this) } that {
                fileName.toStringIsEqualTo("services")
                get { listDirectoryEntriesRecursively() }.size.isGreaterThanOrEqualTo(2)
            }
        }

        @Test
        fun `should copy directory to temp`() {
            expecting { Services.copyToTemp().deleteOnExit(recursively = true) } that {
                parent.isEqualTo(Kommons.filesTemp)
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
