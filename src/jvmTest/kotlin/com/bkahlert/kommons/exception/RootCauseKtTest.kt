package com.bkahlert.kommons.exception

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.message

class RootCauseKtTest {

    @TestFactory
    fun `should find root cause`() = listOf(
        IllegalArgumentException("error message"),
        IllegalStateException(IllegalArgumentException("error message")),
        RuntimeException(IllegalStateException(IllegalArgumentException("error message"))),
        Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message")))),
        RuntimeException(Error(RuntimeException(IllegalStateException(IllegalArgumentException("error message"))))),
    ).flatMap { ex ->
        listOf(
            dynamicTest("$ex") {
                expectThat(ex.rootCause)
                    .isA<IllegalArgumentException>()
                    .message.isEqualTo("error message")
            },
            dynamicTest("$ex (rootCause assertion)") {
                expectThat(ex)
                    .rootCause
                    .isA<IllegalArgumentException>()
                    .message.isEqualTo("error message")
            },
            dynamicTest("$ex (rootCauseMessage assertion)") {
                expectThat(ex)
                    .rootCauseMessage
                    .isEqualTo("error message")
            },
        )
    }
}


/**
 * Maps an assertion on a [Throwable] to an assertion on its
 * [Throwable.rootCause].
 */
val <T : Throwable> Assertion.Builder<T>.rootCause: Assertion.Builder<Throwable>
    get() = get("root cause") { rootCause }


/**
 * Maps an assertion on a [Throwable] to an assertion on its
 * [Throwable.rootCause]'s [Throwable.message].
 */
val <T : Throwable> Assertion.Builder<T>.rootCauseMessage: Assertion.Builder<String?>
    get() = get("root cause message") { rootCause.message }
