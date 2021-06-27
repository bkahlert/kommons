package koodies.jvm

import koodies.debug.asEmoji
import koodies.runtime.isDebugging
import koodies.tracing.TestSpan
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.util.Optional
import kotlin.reflect.jvm.javaMethod

class JvmExtensionsKtTest {

    @Nested
    inner class OrNull {

        @Nested
        inner class AnOptional {

            @Test
            fun `should unwrap present value`() {
                val optional: Optional<String> = Optional.of("test")
                val unwrapped: String? = optional.orNull()
                expectThat(unwrapped).isEqualTo("test")
            }

            @Test
            fun `should unwrap non-present value`() {
                val optional: Optional<String> = Optional.empty()
                val unwrapped: String? = optional.orNull()
                expectThat(unwrapped).isNull()
            }
        }

        @Nested
        inner class ANullableOptional {

            @Test
            fun `should unwrap present value`() {
                @Suppress("RedundantNullableReturnType")
                val optional: Optional<String>? = Optional.of("test")
                val unwrapped: String? = optional.orNull()
                expectThat(unwrapped).isEqualTo("test")
            }

            @Test
            fun `should unwrap non-present value`() {
                @Suppress("RedundantNullableReturnType")
                val optional: Optional<String>? = Optional.empty()
                val unwrapped: String? = optional.orNull()
                expectThat(unwrapped).isNull()
            }

            @Test
            fun `should unwrap null optional`() {
                val optional: Optional<String>? = null
                val unwrapped: String? = optional.orNull()
                expectThat(unwrapped).isNull()
            }
        }
    }

    @Nested
    inner class Ancestors {

        @Test
        fun `should resolve class ancestor`() {
            class InnerTestClass
            expectThat(InnerTestClass::class.java.ancestor).isEqualTo(
                Ancestors::class.java
            )
        }

        @Test
        fun `should resolve class ancestors`() {
            class InnerTestClass
            expectThat(InnerTestClass::class.java.ancestors).containsExactly(
                InnerTestClass::class.java,
                Ancestors::class.java,
                JvmExtensionsKtTest::class.java,
            )
        }

        @Test
        fun `should resolve method ancestor`() {
            val method = ::`should resolve method ancestor`.javaMethod ?: fail("Error getting Java method.")
            expectThat(method.ancestor).isEqualTo(
                Ancestors::class.java,
            )
        }

        @Test
        fun `should resolve method ancestors`() {
            val method = ::`should resolve method ancestors`.javaMethod ?: fail("Error getting Java method.")
            expectThat(method.ancestors).containsExactly(
                method,
                Ancestors::class.java,
                JvmExtensionsKtTest::class.java,
            )
        }
    }

    @Nested
    inner class IsDebugging {

        @Test
        fun TestSpan.`should not throw`() {
            log("Debugging: ${isDebugging.asEmoji}")
        }
    }
}
