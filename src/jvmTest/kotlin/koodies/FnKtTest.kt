package koodies

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message

@Execution(SAME_THREAD)
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
}
