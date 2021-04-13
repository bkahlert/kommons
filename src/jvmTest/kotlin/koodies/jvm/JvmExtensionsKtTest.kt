package koodies.jvm

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.util.Optional

@Execution(SAME_THREAD)
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
}
