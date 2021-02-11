package koodies.builder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class NoopBuilderTest {

    private class CustomNoopBuilder : NoopBuilder<String>

    private val builder = CustomNoopBuilder()

    @Test
    fun `should return compute result as build result`() {
        expectThat(builder.build { "abc" }).isEqualTo("abc")
    }

    @Test
    fun `should return value as build result`() {
        expectThat(builder.instead("abc")).isEqualTo("abc")
    }

    @Test
    fun `should build and transform`() {
        expectThat(builder.build({ "abc" }) { this + this }).isEqualTo("abcabc")
    }
}
