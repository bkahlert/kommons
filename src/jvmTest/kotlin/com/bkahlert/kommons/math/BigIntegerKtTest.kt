package com.bkahlert.kommons.math

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class BigIntegerKtTest {

    @Nested
    inner class BigIntegerZERO {
        @Test
        fun `should have lowest bit`() {
            expectThat(BigInteger.ZERO.takeLowestOneBit()).isEqualTo(-1)
        }

        @Test
        fun `should have highest bit`() {
            expectThat(BigInteger.ZERO.takeHighestOneBit()).isEqualTo(0)
        }
    }

    @Nested
    inner class BigIntegerONE {
        @Test
        fun `should have lowest bit`() {
            expectThat(BigInteger.ONE.takeLowestOneBit()).isEqualTo(0)
        }

        @Test
        fun `should have highest bit`() {
            expectThat(BigInteger.ONE.takeHighestOneBit()).isEqualTo(1)
        }
    }

    @Nested
    inner class BigIntegerTWO {
        @Test
        fun `should have lowest bit`() {
            expectThat(BigInteger.TWO.takeLowestOneBit()).isEqualTo(1)
        }

        @Test
        fun `should have highest bit`() {
            expectThat(BigInteger.TWO.takeHighestOneBit()).isEqualTo(2)
        }
    }

    @Nested
    inner class BigIntegerTEN {
        @Test
        fun `should have lowest bit`() {
            expectThat(BigInteger.TEN.takeLowestOneBit()).isEqualTo(1)
        }

        @Test
        fun `should have highest bit`() {
            expectThat(BigInteger.TEN.takeHighestOneBit()).isEqualTo(4)
        }
    }
}
