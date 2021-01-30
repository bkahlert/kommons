package koodies.concurrent

import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue

@Execution(SAME_THREAD)
class StatusTest {
    private val status0 = Status(0)
    private val status127 = Status(127)
    private val status128 = Status(128)
    private val status255 = Status(255)

    @TestFactory
    fun `should be instantiatable from byte`() = listOf(
        0.toByte() to status0,
        Byte.MAX_VALUE to status127,
        Byte.MIN_VALUE to status128,
        (-1).toByte() to status255,
    ).testEach { (byte: Byte, expected) ->
        expect { Status(byte) }.that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should be considered successful`() = listOf(
        status0,
        Status.SUCCESS,
    ).testEach { status ->
        expect { status }.that { isSuccessful() }
    }

    @TestFactory
    fun `should be considered failed`() = listOf(
        status127,
        status128,
        status255,
        Status.FAILURE
    ).testEach { status ->
        expect { status }.that { isFailed() }
    }

    @TestFactory
    fun `should format as int plus arrow with hook`() = listOf(
        status0 to "𝟶↩",
        status127 to "𝟷𝟸𝟽↩",
        status128 to "𝟷𝟸𝟾↩",
        status255 to "𝟸𝟻𝟻↩",
    ).testEach { (status, expected) ->
        test("using format()") { expect { status.format() }.that { isEqualTo(expected) } }
        test("using toString()") { expect { status.toString() }.that { isEqualTo(expected) } }
    }
}

fun Assertion.Builder<Status>.isSuccessful() =
    assert("is successful") { get { successful }.isTrue() }

fun Assertion.Builder<Status>.isFailed() =
    assert("is successful") { get { failed }.isTrue() }
