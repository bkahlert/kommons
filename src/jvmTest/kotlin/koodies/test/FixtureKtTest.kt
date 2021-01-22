package koodies.test

import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import java.io.IOException

@Execution(SAME_THREAD)
class FixtureKtTest {

    @TestFactory
    fun textFixture() = TextFixture("text-fixture", "line 1\nline 2\n").should {
        "have name" { name.isEqualTo("text-fixture") }
        "have text" { text.isEqualTo("line 1\nline 2\n") }
        "have data" { data.isEqualTo("line 1\nline 2\n".toByteArray()) }
    }

    @TestFactory
    fun binaryFixture() = BinaryFixture("binary-fixture", "binary".toByteArray()).should {
        "have name" { name.isEqualTo("binary-fixture") }
        "have text" { text.isEqualTo("binary") }
        "have data" { data.isEqualTo("binary".toByteArray()) }
    }


    @TestFactory
    fun classPathFixture() = ClassPathDirectoryFixture("META-INF").should {
        "have name" { name.isEqualTo("META-INF") }
        throwOn({ text }) { isA<IOException>() }
        throwOn({ data }) { isA<IOException>() }
        with("file-backed class path", { dir("services").file("org.junit.jupiter.api.extension.Extension") }) {
            "have name" { name.isEqualTo("org.junit.jupiter.api.extension.Extension") }
            "have text" { text.contains("koodies.test.debug.DebugCondition") }
            "have data" { data.get { size }.isGreaterThan(10) }
        }
        throwOn({ dir("I dont exist") }) { isA<Throwable>() }
    }

    @TestFactory
    fun staticClassPathFixture() = META_INF.should {
        "have name" { name.isEqualTo("META-INF") }
        throwOn({ text }) { isA<IOException>() }
        throwOn({ data }) { isA<IOException>() }
        with("file-backed class path", { META_INF.Services.JUnitExtensions }) {
            "have name" { name.isEqualTo("org.junit.jupiter.api.extension.Extension") }
            "have text" { text.contains("koodies.test.debug.DebugCondition") }
            "have data" { data.get { size }.isGreaterThan(10) }
        }
        throwOn({ Dir("I dont exist") }) { isA<Throwable>() }
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
