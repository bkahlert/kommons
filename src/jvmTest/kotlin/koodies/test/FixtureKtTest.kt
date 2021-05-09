package koodies.test

import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import java.io.IOException

class FixtureKtTest {

    @TestFactory
    fun textFixture() = test(TextFixture("text-fixture", "line 1\nline 2$LF")) {
        expecting { name } that { isEqualTo("text-fixture") }
        expecting { text } that { isEqualTo("line 1\nline 2$LF") }
        expecting { data } that { isEqualTo("line 1\nline 2$LF".toByteArray()) }
    }

    @TestFactory
    fun binaryFixture() = test(BinaryFixture("binary-fixture", "binary".toByteArray())) {
        expecting { name } that { isEqualTo("binary-fixture") }
        expecting { text } that { isEqualTo("binary") }
        expecting { data } that { isEqualTo("binary".toByteArray()) }
    }


    @TestFactory
    fun classPathFixture() = test(ClassPathDirectoryFixture("META-INF")) {
        expecting { name } that { isEqualTo("META-INF") }
        expectThrows<IOException> { text }
        expectThrows<IOException> { data }
        with("dynamic paths") { dir("services").file("org.junit.jupiter.api.extension.Extension") } then {
            expecting { name } that { isEqualTo("org.junit.jupiter.api.extension.Extension") }
            expecting { text } that { contains("koodies.debug.DebugCondition") }
            expecting { data.size } that { isGreaterThan(10) }
        }
        expectThrows<Throwable> { dir("I dont exist") }
    }

    @TestFactory
    fun staticClassPathFixture() = test(META_INF) {
        expecting { name } that { isEqualTo("META-INF") }
        expectThrows<IOException> { text }
        expectThrows<IOException> { data }
        with("static paths") { META_INF.Services.JUnitExtensions } then {
            expecting { name } that { isEqualTo("org.junit.jupiter.api.extension.Extension") }
            expecting { text } that { contains("koodies.debug.DebugCondition") }
            expecting { data.size } that { isGreaterThan(10) }
        }
        expectThrows<Throwable> { Dir("I dont exist") }
    }

    object META_INF : ClassPathDirectoryFixture("META-INF") {
        object Services : Dir("services") {
            object JUnitExtensions : File("org.junit.jupiter.api.extension.Extension")
        }
    }
}

val <T : Fixture> Assertion.Builder<T>.name get() = get("name %s") { name }
val <T : Fixture> Assertion.Builder<T>.text get() = get("text %s") { text }
val <T : Fixture> Assertion.Builder<T>.data get() = get("data %s") { data }
