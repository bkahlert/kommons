package koodies.text

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isLessThanOrEqualTo

class DecodeToValidStringKtTest {

    @Nested
    inner class EmptyString {
        @Test
        fun `should decode`() {
            expectThat(ByteArray(0).decodeToValidString()).isEmpty()
        }
    }

    @Nested
    inner class ValidString {
        val validString = "aβç".also { check(it.toByteArray().size == 5) }

        @Test
        fun `should decode`() {
            expectThat(validString.toByteArray().decodeToValidString()).isEqualTo(validString)
        }

        @Test
        fun `should not 'loose' any bytes`() {
            val byteArray = validString.toByteArray()
            expectThat(byteArray.decodeToValidString().toByteArray().size).isEqualTo(byteArray.size)
        }
    }

    @Nested
    inner class InvalidString {
        val invalidString = "aβ𝌔".toByteArray().dropLast(1).toByteArray()

        @Test
        fun `should convert array with invalid string`() {

            expectThat(invalidString.decodeToValidString()).isEqualTo("aβ")
        }

        @Test
        fun `should not 'loose' more than 3 bytes (𝕓 has 4 bytes)`() {
            expectThat("ab𝕓".toByteArray().dropLast(1).size - "aβ".toByteArray().size).isLessThanOrEqualTo(3)
        }
    }
}
