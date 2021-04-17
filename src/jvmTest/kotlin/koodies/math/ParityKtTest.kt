package koodies.math

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class ParityKtTest {

    @TestFactory
    fun IsOdd() = test {
        expect("-1 byte") { ((-1).toByte().isOdd) }.that { isEqualTo(true) }
        expect(" 0 byte") { (0.toByte().isOdd) }.that { isEqualTo(false) }
        expect(" 5 byte") { (5.toByte().isOdd) }.that { isEqualTo(true) }
        expect("-1 short") { ((-1).toShort().isOdd) }.that { isEqualTo(true) }
        expect(" 0 short") { (0.toShort().isOdd) }.that { isEqualTo(false) }
        expect(" 5 short") { (5.toShort().isOdd) }.that { isEqualTo(true)}
        expect("-1 int") { (-1).isOdd }.that { isEqualTo(true) }
        expect(" 0 int") { 0.isOdd }.that { isEqualTo(false) }
        expect(" 5 int") { 5.isOdd }.that { isEqualTo(true) }
        expect("-1 big int") { (BigInteger.ONE*-1).isOdd }.that { isEqualTo(true) }
        expect(" 0 big int") { BigInteger.ZERO.isOdd }.that { isEqualTo(false) }
        expect(" 5 big int") { 5.toBigInteger().isOdd }.that { isEqualTo(true)}
    }

    @TestFactory
    fun IsEven() = test {
        expect("-1 byte") { ((-1).toByte().isEven) }.that { isEqualTo(false) }
        expect(" 0 byte") { (0.toByte().isEven) }.that { isEqualTo(true) }
        expect(" 5 byte") { (5.toByte().isEven) }.that { isEqualTo(false) }
        expect("-1 short") { ((-1).toShort().isEven) }.that { isEqualTo(false) }
        expect(" 0 short") { (0.toShort().isEven) }.that { isEqualTo(true) }
        expect(" 5 short") { (5.toShort().isEven) }.that { isEqualTo(false)}
        expect("-1 int") { (-1).isEven }.that { isEqualTo(false) }
        expect(" 0 int") { 0.isEven }.that { isEqualTo(true) }
        expect(" 5 int") { 5.isEven }.that { isEqualTo(false) }
        expect("-1 big int") { (BigInteger.ONE*-1).isEven }.that { isEqualTo(false) }
        expect(" 0 big int") { BigInteger.ZERO.isEven }.that { isEqualTo(true) }
        expect(" 5 big int") { 5.toBigInteger().isEven }.that { isEqualTo(false)}
    }

}
