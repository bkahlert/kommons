package koodies.math

import koodies.test.tests
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(SAME_THREAD)
class NumbersKtTest {

    @Nested
    inner class ZeroEquality {

        @TestFactory
        fun `should be zero`() = tests {
            expecting("byte") { 0.toByte().isZero } that { isTrue() }
            expecting("unsigned byte") { 0.toUByte().isZero } that { isTrue() }
            expecting("short") { 0.toShort().isZero } that { isTrue() }
            expecting("unsigned short") { 0.toUShort().isZero } that { isTrue() }
            expecting("int") { 0.isZero } that { isTrue() }
            expecting("unsigned int ") { 0u.isZero } that { isTrue() }
            expecting("long") { 0L.isZero } that { isTrue() }
            expecting("unsigned long") { 0uL.isZero } that { isTrue() }
            expecting("float") { 0F.isZero } that { isTrue() }
            expecting("big integer constant") { BigIntegerConstants.ZERO.isZero } that { isTrue() }
            expecting("big integer") { 0.toBigInteger().isZero } that { isTrue() }
            expecting("big integer calculated") { (BigIntegerConstants.ONE - BigIntegerConstants.ONE).isZero } that { isTrue() }
            expecting("big decimal constant") { 0.toBigDecimal().isZero } that { isTrue() }
            expecting("big decimal") { 0.0.toBigDecimal().isZero } that { isTrue() }
            expecting("big decimal calculated") { (BigDecimalConstants.ONE - BigDecimalConstants.ONE).isZero } that { isTrue() }
        }

        @TestFactory
        fun `should not be zero`() = tests {
            expecting("byte") { 2.toByte().isZero } that { isFalse() }
            expecting("unsigned byte") { 2.toUByte().isZero } that { isFalse() }
            expecting("short") { 2.toShort().isZero } that { isFalse() }
            expecting("unsigned short") { 2.toUShort().isZero } that { isFalse() }
            expecting("int") { 2.isZero } that { isFalse() }
            expecting("unsigned int ") { 2u.isZero } that { isFalse() }
            expecting("long") { 2L.isZero } that { isFalse() }
            expecting("unsigned long") { 2uL.isZero } that { isFalse() }
            expecting("float") { 2F.isZero } that { isFalse() }
            expecting("big integer constant") { BigIntegerConstants.TWO.isZero } that { isFalse() }
            expecting("big integer") { 2.toBigInteger().isZero } that { isFalse() }
            expecting("big integer calculated") { (BigIntegerConstants.ONE + BigIntegerConstants.ONE).isZero } that { isFalse() }
            expecting("big decimal constant") { BigDecimalConstants.TWO.isZero } that { isFalse() }
            expecting("big decimal") { 0.2.toBigDecimal().isZero } that { isFalse() }
            expecting("big decimal calculated") { (BigDecimalConstants.ONE + BigDecimalConstants.ONE).isZero } that { isFalse() }
        }
    }
}
