package koodies.math

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo

class DivisionKtTest {

    @Nested
    inner class CeilDivide {

        @Test
        fun `should not ceil if integer`() {
            expectThat(12 ceilDiv 6).isEqualTo(2)
        }

        @Test
        fun `should ceil if exact half`() {
            expectThat(12 ceilDiv 8).isEqualTo(2)
        }

        @Test
        fun `should ceil if not integer`() {
            expectThat(12 ceilDiv 7).isEqualTo(2)
        }

        @Test
        fun `should throw on division by 0`() {
            expectThrows<ArithmeticException> { 12 ceilDiv 0 }
        }
    }

    @Nested
    inner class FloorDivide {

        @Test
        fun `should not floor if integer`() {
            expectThat(12 floorDiv 6).isEqualTo(2)
        }

        @Test
        fun `should floor if exact half`() {
            expectThat(12 floorDiv 8).isEqualTo(1)
        }

        @Test
        fun `should floor if not integer`() {
            expectThat(12 floorDiv 7).isEqualTo(1)
        }

        @Test
        fun `should throw on division by 0`() {
            expectThrows<ArithmeticException> { 12 floorDiv 0 }
        }
    }
}
