package koodies

import koodies.math.BigIntegerConstants
import koodies.math.toBigInteger
import koodies.test.tests
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isSuccess
import strikt.assertions.message

class FnKtTest {

    @Nested
    inner class Otherwise {

        @Test
        fun `should run on null value`() {
            val value: String? = null
            expectThat(value otherwise { "fallback" }).isEqualTo("fallback")
        }

        @Test
        fun `should return value if not null`() {
            val value = "value"
            expectThat(value otherwise { "fallback" }).isEqualTo("value")
        }
    }

    @Nested
    inner class RunWrapping {

        @Nested
        inner class WithoutReceiver {

            @Test
            fun `should run in correct order and return`() {
                val events = mutableListOf<String>()
                val returnValue = koodies.runWrapping(
                    { events.add("before") },
                    { events.add("after") })
                { events.add("block"); "value" }

                expectThat(returnValue).isEqualTo("value")
                expectThat(events).containsExactly("before", "block", "after")
            }

            @Test
            fun `should run in correct order on fail`() {
                val events = mutableListOf<String>()
                expectCatching {
                    koodies.runWrapping(
                        { events.add("before") },
                        { events.add("after") })
                    { throw RuntimeException("test") }
                }
                    .isFailure().message.isEqualTo("test")
                expectThat(events).containsExactly("before", "after")
            }
        }

        @Nested
        inner class WithReceiver {

            @Test
            fun `should run in correct order and return`() {
                val events = mutableListOf<Pair<Any?, String>>()
                val returnValue = "receiver".runWrapping(
                    { events.add(this to "before") },
                    { events.add(this to "after") })
                { events.add(this to "block"); "value" }

                expectThat(returnValue).isEqualTo("value")
                expectThat(events).containsExactly("receiver" to "before", "receiver" to "block", "receiver" to "after")
            }

            @Test
            fun `should run in correct order on fail`() {
                val events = mutableListOf<Pair<Any?, String>>()
                expectCatching {
                    "receiver".runWrapping(
                        { events.add(this to "before") },
                        { events.add(this to "after") })
                    { throw RuntimeException("test") }
                }
                    .isFailure().message.isEqualTo("test")
                expectThat(events).containsExactly("receiver" to "before", "receiver" to "after")
            }
        }
    }

    @Nested
    inner class Times {

        @Test
        fun `should run block 0 times`() {
            expectCatching { 0 * { throw IllegalStateException("test failed") } }.isSuccess()
        }

        @Test
        fun `should run block n times`() {
            var n = 3
            n * { n-- }
            expectThat(n == 0)
        }

        @Test
        fun `should return n results`() {
            val n = 3
            val results = run { n * { it } }
            expectThat(results).containsExactly(0, 1, 2)
        }

        @Test
        fun `should throw negative index`() {
            expectCatching { -1 * { println("test failed") } }.isFailure().isA<IllegalArgumentException>()
        }
    }


    @Nested
    inner class BigIntegerTimes {

        @Test
        fun `should run block 0 times`() {
            expectCatching { BigIntegerConstants.ZERO * { throw IllegalStateException("test failed") } }.isSuccess()
        }

        @Test
        fun `should run block n times`() {
            var n = 3.toBigInteger()
            n * { n-- }
            expectThat(n == BigIntegerConstants.ZERO)
        }

        @Test
        fun `should return n results`() {
            val n = 3.toBigInteger()
            val results = run { n * { it } }
            expectThat(results).containsExactly(0, 1, 2)
        }

        @Test
        fun `should throw negative index`() {
            expectCatching { (-1).toBigInteger() * { println("test failed") } }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on too large index`() {
            expectCatching { (Int.MAX_VALUE.toBigInteger() + BigIntegerConstants.ONE) * { println("test failed") } }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @TestFactory
    fun `should require not empty`() = tests {
        expecting { "abc".requireNotEmpty() } that { isEqualTo("abc") }
        expecting { "abc".requireNotEmpty { "error" } } that { isEqualTo("abc") }
        expectThrows<IllegalArgumentException> { "".requireNotEmpty() }
        expectThrows<IllegalArgumentException> { "".requireNotEmpty { "error" } } that { message.isEqualTo("error") }
    }

    @TestFactory
    fun `should require not blank`() = tests {
        expecting { "abc".requireNotBlank() } that { isEqualTo("abc") }
        expecting { "abc".requireNotBlank { "error" } } that { isEqualTo("abc") }
        expectThrows<IllegalArgumentException> { "   ".requireNotBlank() }
        expectThrows<IllegalArgumentException> { "   ".requireNotBlank { "error" } } that { message.isEqualTo("error") }
    }

    @TestFactory
    fun `should check not empty`() = tests {
        expecting { "abc".checkNotEmpty() } that { isEqualTo("abc") }
        expecting { "abc".checkNotEmpty { "error" } } that { isEqualTo("abc") }
        expectThrows<IllegalStateException> { "".checkNotEmpty() }
        expectThrows<IllegalStateException> { "".checkNotEmpty { "error" } } that { message.isEqualTo("error") }
    }

    @TestFactory
    fun `should check not blank`() = tests {
        expecting { "abc".checkNotBlank() } that { isEqualTo("abc") }
        expecting { "abc".checkNotBlank { "error" } } that { isEqualTo("abc") }
        expectThrows<IllegalStateException> { "   ".checkNotBlank() }
        expectThrows<IllegalStateException> { "   ".checkNotBlank { "error" } } that { message.isEqualTo("error") }
    }

    @Nested
    inner class NullableInvoke {

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
}
