package koodies.math

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class BigIntegerKtTest {

    @TestFactory
    fun LowestBit() = test {
        expect("${BigInteger.ZERO}") { BigInteger.ZERO.takeLowestOneBit() }.that { isEqualTo(-1) }
        expect("${BigInteger.ONE}") { BigInteger.ONE.takeLowestOneBit() }.that { isEqualTo(0) }
        expect("${BigInteger.TWO}") { BigInteger.TWO.takeLowestOneBit() }.that { isEqualTo(1) }
        expect("${BigInteger.TEN}") { BigInteger.TEN.takeLowestOneBit() }.that { isEqualTo(1) }
    }

    @TestFactory
    fun HighestBit() = test {
        expect("${BigInteger.ZERO}}") { BigInteger.ZERO.takeHighestOneBit() }.that { isEqualTo(0) }
        expect("${BigInteger.ONE}}") { BigInteger.ONE.takeHighestOneBit() }.that { isEqualTo(1) }
        expect("${BigInteger.TWO}}") { BigInteger.TWO.takeHighestOneBit() }.that { isEqualTo(2) }
        expect("${BigInteger.TEN}}") { BigInteger.TEN.takeHighestOneBit() }.that { isEqualTo(4) }
    }
}
