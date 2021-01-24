package koodies.concurrent

import koodies.test.test
import koodies.test.tests
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
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
    ).test { (byte: Byte, expected) ->
        expectThat(Status(byte)).isEqualTo(expected)
    }

    @TestFactory
    fun `should be considered successful`() = listOf(
        status0,
        Status.SUCCESS,
    ).test { status ->
        expectThat(status).isSuccessful()
    }

    @TestFactory
    fun `should be considered failed`() = listOf(
        status127,
        status128,
        status255,
        Status.FAILURE
    ).test { status ->
        expectThat(status).isFailed()
    }

    @TestFactory
    fun `should format as int plus arrow with hook`() = listOf(
        status0 to "𝟶↩",
        status127 to "𝟷𝟸𝟽↩",
        status128 to "𝟷𝟸𝟾↩",
        status255 to "𝟸𝟻𝟻↩",
    ).tests { (status, expected) ->
        test("using format()") { expectThat(status.format()).isEqualTo(expected) }
        test("using toString()") { expectThat(status.toString()).isEqualTo(expected) }
    }
}

fun Assertion.Builder<Status>.isSuccessful() =
    assert("is successful") { get { successful }.isTrue() }

fun Assertion.Builder<Status>.isFailed() =
    assert("is successful") { get { failed }.isTrue() }
