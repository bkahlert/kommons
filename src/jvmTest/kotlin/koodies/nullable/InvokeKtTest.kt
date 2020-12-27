package koodies.nullable

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class InvokeKtTest {

    @Nested
    inner class WithNonNullableReturnType {

        @Test
        fun `should apply f if set`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String)? = { it + it }
            expectThat(f("a")).isEqualTo("aa")
        }

        @Test
        fun `should return unchanged argument if f is unset`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String)? = null
            expectThat(f("a")).isEqualTo("a")
        }
    }

    @Nested
    inner class WithNullableReturnType {

        @Test
        fun `should return result of applied f if non-null returned`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String?)? = { it + it }
            expectThat("a".let(f)).isEqualTo("aa")
        }

        @Test
        fun `should return unchanged argument if null returned`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String?)? = { null }
            expectThat("a".let(f)).isEqualTo("a")
        }

        @Test
        fun `should return unchanged argument if f is unset`() {
            @Suppress("RedundantNullableReturnType")
            val f: ((String) -> String)? = null
            expectThat("a".let(f)).isEqualTo("a")
        }
    }
}
