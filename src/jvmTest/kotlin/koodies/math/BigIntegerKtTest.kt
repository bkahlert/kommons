package koodies.math

import koodies.test.tests
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class BigIntegerKtTest {

    @TestFactory
    fun LowestBit() = tests {
        expecting("${BigInteger.ZERO}") { BigInteger.ZERO.takeLowestOneBit() } that { isEqualTo(-1) }
        expecting("${BigInteger.ONE}") { BigInteger.ONE.takeLowestOneBit() } that { isEqualTo(0) }
        expecting("${BigInteger.TWO}") { BigInteger.TWO.takeLowestOneBit() } that { isEqualTo(1) }
        expecting("${BigInteger.TEN}") { BigInteger.TEN.takeLowestOneBit() } that { isEqualTo(1) }
    }

    @TestFactory
    fun HighestBit() = tests {
        expecting("${BigInteger.ZERO}}") { BigInteger.ZERO.takeHighestOneBit() } that { isEqualTo(0) }
        expecting("${BigInteger.ONE}}") { BigInteger.ONE.takeHighestOneBit() } that { isEqualTo(1) }
        expecting("${BigInteger.TWO}}") { BigInteger.TWO.takeHighestOneBit() } that { isEqualTo(2) }
        expecting("${BigInteger.TEN}}") { BigInteger.TEN.takeHighestOneBit() } that { isEqualTo(4) }
    }
}
