package koodies.logging

import koodies.test.testEach
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
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
        RuntimeException("exception") to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:23)",
        kotlin.runCatching { returnValue } to "return value",
        kotlin.runCatching { throw exception } to "RuntimeException: exception at.(${ReturnValueKtTest::class.simpleName}.kt:16)",
    ).testEach { (subject, expected) ->
        expect { subject.toReturnValue().format() }.that { isEqualTo(expected) }
    }
}
