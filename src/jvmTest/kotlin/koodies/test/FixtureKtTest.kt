package koodies.test

import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import java.io.IOException

@Execution(SAME_THREAD)
class FixtureKtTest {

    @TestFactory
    fun textFixture() = TextFixture("text-fixture", "line 1\nline 2\n").test {
        expect { name }.that { isEqualTo("text-fixture") }
        expect { text }.that { isEqualTo("line 1\nline 2\n") }
        expect { data }.that { isEqualTo("line 1\nline 2\n".toByteArray()) }
    }

    @TestFactory
    fun binaryFixture() = BinaryFixture("binary-fixture", "binary".toByteArray()).test {
        expect { name }.that { isEqualTo("binary-fixture") }
        expect { text }.that { isEqualTo("binary") }
        expect { data }.that { isEqualTo("binary".toByteArray()) }
    }


    @TestFactory
    fun classPathFixture() = ClassPathDirectoryFixture("META-INF").test {
        expect { name }.that { isEqualTo("META-INF") }
        expectThrowing { text }.that { isFailure().isA<IOException>() }
        expectThrowing { data }.that { isFailure().isA<IOException>() }
        with("dynamic paths") { dir("services").file("org.junit.jupiter.api.extension.Extension") }.then {
            expect { name }.that { isEqualTo("org.junit.jupiter.api.extension.Extension") }
            expect { text }.that { contains("koodies.test.debug.DebugCondition") }
            expect { data.size }.that { isGreaterThan(10) }
        }
        expectThrowing { dir("I dont exist") }.that { isFailure().isA<Throwable>() }
    }

    @TestFactory
    fun staticClassPathFixture() = META_INF.test {
        expect { name }.that { isEqualTo("META-INF") }
        expectThrowing { text }.that { isFailure().isA<IOException>() }
        expectThrowing { data }.that { isFailure().isA<IOException>() }
        with("static paths") { META_INF.Services.JUnitExtensions }.then {
            expect { name }.that { isEqualTo("org.junit.jupiter.api.extension.Extension") }
            expect { text }.that { contains("koodies.test.debug.DebugCondition") }
            expect { data.size }.that { isGreaterThan(10) }
        }
        expectThrowing { Dir("I dont exist") }.that { isFailure().isA<Throwable>() }
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
