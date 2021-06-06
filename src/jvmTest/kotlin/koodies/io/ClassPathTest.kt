package koodies.io

import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan

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
            expecting { data.decodeToString() } that { contains("koodies.debug.DebugCondition") }
            expecting { data.size } that { isGreaterThan(10) }
        }

        @Test
        fun `should nest`() {
            expecting { META_INF.dir("services").file("org.junit.jupiter.api.extension.Extension") } that {
                toStringIsEqualTo("META-INF/services/org.junit.jupiter.api.extension.Extension")
            }
        }

        @Test
        fun `should throw if not exist`() {
            expectThrows<Throwable> { META_INF.file("I dont exist") }
        }
    }

    private object META_INF : ClassPathDirectory("META-INF") {
        object Services : Dir("services") {
            object JUnitExtensions : File("org.junit.jupiter.api.extension.Extension")
        }
    }
}

val <T : InMemoryFile> Assertion.Builder<T>.name get() = get("name %s") { name }
val <T : InMemoryTextFile> Assertion.Builder<T>.text get() = get("text %s") { text }
val <T : InMemoryFile> Assertion.Builder<T>.data get() = get("data %s") { data }
