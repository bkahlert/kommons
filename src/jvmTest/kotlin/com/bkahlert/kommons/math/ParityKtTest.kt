package com.bkahlert.kommons.math

import com.bkahlert.kommons.test.testsOld
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class ParityKtTest {

    @TestFactory
    fun isOdd() = testsOld {
        expecting("-1 byte") { ((-1).toByte().isOdd) } that { isEqualTo(true) }
        expecting(" 0 byte") { (0.toByte().isOdd) } that { isEqualTo(false) }
        expecting(" 5 byte") { (5.toByte().isOdd) } that { isEqualTo(true) }
        expecting("-1 short") { ((-1).toShort().isOdd) } that { isEqualTo(true) }
        expecting(" 0 short") { (0.toShort().isOdd) } that { isEqualTo(false) }
        expecting(" 5 short") { (5.toShort().isOdd) } that { isEqualTo(true) }
        expecting("-1 int") { (-1).isOdd } that { isEqualTo(true) }
        expecting(" 0 int") { 0.isOdd } that { isEqualTo(false) }
        expecting(" 5 int") { 5.isOdd } that { isEqualTo(true) }
        expecting("-1 big int") { (BigInteger.ONE * -1).isOdd } that { isEqualTo(true) }
        expecting(" 0 big int") { BigInteger.ZERO.isOdd } that { isEqualTo(false) }
        expecting(" 5 big int") { 5.toBigInteger().isOdd } that { isEqualTo(true) }
    }

    @TestFactory
    fun isEvening() = testsOld {
        expecting("-1 byte") { ((-1).toByte().isEven) } that { isEqualTo(false) }
        expecting(" 0 byte") { (0.toByte().isEven) } that { isEqualTo(true) }
        expecting(" 5 byte") { (5.toByte().isEven) } that { isEqualTo(false) }
        expecting("-1 short") { ((-1).toShort().isEven) } that { isEqualTo(false) }
        expecting(" 0 short") { (0.toShort().isEven) } that { isEqualTo(true) }
        expecting(" 5 short") { (5.toShort().isEven) } that { isEqualTo(false) }
        expecting("-1 int") { (-1).isEven } that { isEqualTo(false) }
        expecting(" 0 int") { 0.isEven } that { isEqualTo(true) }
        expecting(" 5 int") { 5.isEven } that { isEqualTo(false) }
        expecting("-1 big int") { (BigInteger.ONE * -1).isEven } that { isEqualTo(false) }
        expecting(" 0 big int") { BigInteger.ZERO.isEven } that { isEqualTo(true) }
        expecting(" 5 big int") { 5.toBigInteger().isEven } that { isEqualTo(false) }
    }
}
