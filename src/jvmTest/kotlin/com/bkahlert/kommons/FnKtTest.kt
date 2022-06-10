package com.bkahlert.kommons

import com.bkahlert.kommons.math.BigIntegerConstants
import com.bkahlert.kommons.math.toBigInteger
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    inner class ComposeKtTest {

        fun a(arg: String): String = "${arg.length}"
        fun b(arg: String): String = "$arg$arg"
        fun c(arg: String): String = "${arg}c"

        @Test
        fun `a + b should call b with result of a`() {
            val ab = ::a.compose(::b)
            expectThat(ab("abc")).isEqualTo("33")
        }

        @Test
        fun `b + a should call a with result of b`() {
            val ba = ::b.compose(::a)
            expectThat(ba("abc")).isEqualTo("6")
        }

        @Test
        fun `compose should have + infix function+`() {
            val ab = ::a.plus(::b)
            expectThat(ab("abc")).isEqualTo("33")
        }

        @Test
        fun `should with three functions`() {
            val ab = ::a.compose(::b, ::c)
            expectThat(ab("abc")).isEqualTo("33c")
        }

        @Test
        fun `should allow composition without receiver`() {
            val ab = compositionOf(::a, ::b, ::c)
            expectThat(ab("abc")).isEqualTo("33c")
        }

        @Test
        fun `should allow conditional composition without receiver`() {
            val ab = compositionOf(true to ::a, false to ::b, true to ::c)
            expectThat(ab("abc")).isEqualTo("3c")
        }
    }

    @Nested
    inner class RunWrapping {

        @Nested
        inner class WithoutReceiver {

            @Test
            fun `should run in correct order and return`() {
                val events = mutableListOf<String>()
                val returnValue = com.bkahlert.kommons.runWrapping(
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
                    com.bkahlert.kommons.runWrapping(
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
