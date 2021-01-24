package koodies.logging

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ReturnValueKtTest {
    private val returnValue: ReturnValue = object : ReturnValue {
        override val successful: Boolean get() = false
        override fun format(): String = "return value"
    }

    private val exception = RuntimeException("exception")

    @TestFactory
    fun `should convert to ReturnValue`() = listOf(
        null to "␀",
        "string" to "string",
        returnValue to "return value",
        RuntimeException("exception") to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:21)",
        kotlin.runCatching { returnValue } to "return value",
        kotlin.runCatching { throw exception } to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:14)",
    ).test { (subject, expected) ->
        expectThat(subject.toReturnValue().format()).isEqualTo(expected)
    }
}
