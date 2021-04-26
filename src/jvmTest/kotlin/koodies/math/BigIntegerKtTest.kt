package koodies.math

import koodies.test.tests
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
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
