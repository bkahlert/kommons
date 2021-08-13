package koodies.tracing.rendering

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SLF4JTest {

    @Nested
    inner class Replacement {

        @Test
        fun `should fill placeholders in SLF4J style format`() {
            val slf4jStyleFormat = "A {} C {} E"
            val actual = SLF4J.format(slf4jStyleFormat, "B", "D")
            expectThat(actual).isEqualTo("A B C D E")
        }

        @Test
        fun `should fill placeholders in SLF4J style format if too many args`() {
            val slf4jStyleFormat = "A {} C {} E"
            val actual = SLF4J.format(slf4jStyleFormat, "B", "D", "Z")
            expectThat(actual).isEqualTo("A B C D E")
        }

        @Test
        fun `should fill placeholders in SLF4J style format if too few args`() {
            val slf4jStyleFormat = "A {} C {} E"
            val actual = SLF4J.format(slf4jStyleFormat, "B")
            expectThat(actual).isEqualTo("A B C {1} E")
        }
    }
}
