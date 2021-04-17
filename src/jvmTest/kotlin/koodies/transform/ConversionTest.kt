package koodies.transform

import koodies.math.Constants
import koodies.math.dec
import koodies.math.isEqualToUnsigned
import koodies.math.toDecimalString
import koodies.test.tests
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import java.math.BigInteger

class ConversionTest {

    @Test
    fun `should provide entry point with convert property`() {
        expectCatching { "abc".convert }.isSuccess()
    }

    @Test
    fun `should provide apply changes`() {
        expectThat("abc".convert.asHexadecimalString.toDecimalString()).isEqualTo("2748")
    }

    @Nested
    inner class NumericConversions {

        private val bigInt128 = BigInteger.TWO.pow(128).dec()
        private val bigInt129 = BigInteger.TWO.pow(128)

        @Nested
        inner class ToBytes {

            @TestFactory
            fun `should convert from string representation`() = tests {
                Constants.TWO_POW_128_BIN_STRING test { expecting { convert.asBinaryString.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }
                Constants.TWO_POW_128_DEC_STRING test { expecting { convert.asDecimalString.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }
                Constants.TWO_POW_128_HEX_STRING test { expecting { convert.asHexadecimalString.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }

                Constants.TWO_POW_128_PLUS_1_BIN_STRING test { expecting { convert.asBinaryString.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
                Constants.TWO_POW_128_PLUS_1_DEC_STRING test { expecting { convert.asDecimalString.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
                Constants.TWO_POW_128_PLUS_1_HEX_STRING test { expecting { convert.asHexadecimalString.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
            }

            @TestFactory
            fun `should convert from bytes`() = tests {
                Constants.TWO_POW_128_BYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }
                Constants.TWO_POW_128_UBYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }

                Constants.TWO_POW_128_PLUS_1_BYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
                Constants.TWO_POW_128_PLUS_1_UBYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
            }

            @TestFactory
            fun `should convert from big integer`() = tests {
                bigInt128 test { expecting { convert.asUnsigned.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }
                bigInt129 test { expecting { convert.asUnsigned.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
            }
        }

        @Nested
        inner class FromBytes {

            @TestFactory
            fun `should convert to string representation`() = tests {
                Constants.TWO_POW_128_BYTES testAll {
                    test("bin 2^128") { expecting { convert.toBinaryString() } that { isEqualTo(Constants.TWO_POW_128_BIN_STRING) } }
                    test("dec 2^128") { expecting { convert.toDecimalString() } that { isEqualTo(Constants.TWO_POW_128_DEC_STRING) } }
                    test("dec 2^128") { expecting { convert.toUByteArray().toDecimalString() } that { isEqualTo(Constants.TWO_POW_128_DEC_STRING) } }
                    test("hex 2^128") { expecting { convert.toHexadecimalString() } that { isEqualTo(Constants.TWO_POW_128_HEX_STRING) } }
                }
                Constants.TWO_POW_128_PLUS_1_BYTES testAll {
                    test("bin 2^128+1") { expecting { convert.toBinaryString() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BIN_STRING) } }
                    test("dec 2^128+1") { expecting { convert.toDecimalString() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_DEC_STRING) } }
                    test("hex 2^128+1") { expecting { convert.toHexadecimalString() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_HEX_STRING) } }
                }
            }

            @TestFactory
            fun `should convert to bytes`() = tests {
                Constants.TWO_POW_128_BYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }
                Constants.TWO_POW_128_BYTES test { expecting { convert.toUByteArray() } that { isEqualToUnsigned(Constants.TWO_POW_128_UBYTES) } }
                Constants.TWO_POW_128_PLUS_1_BYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
                Constants.TWO_POW_128_PLUS_1_BYTES test { expecting { convert.toUByteArray() } that { isEqualToUnsigned(Constants.TWO_POW_128_PLUS_1_UBYTES) } }

                Constants.TWO_POW_128_UBYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_BYTES) } }
                Constants.TWO_POW_128_UBYTES test { expecting { convert.toUByteArray() } that { isEqualToUnsigned(Constants.TWO_POW_128_UBYTES) } }
                Constants.TWO_POW_128_PLUS_1_UBYTES test { expecting { convert.toByteArray() } that { isEqualTo(Constants.TWO_POW_128_PLUS_1_BYTES) } }
                Constants.TWO_POW_128_PLUS_1_UBYTES test { expecting { convert.toUByteArray() } that { isEqualToUnsigned(Constants.TWO_POW_128_PLUS_1_UBYTES) } }
            }

            @TestFactory
            fun `should produce same big integer`() = tests {
                Constants.TWO_POW_128_BYTES test { expecting { convert.toBigInteger() } that { isEqualTo(bigInt128) } }
                Constants.TWO_POW_128_PLUS_1_BYTES test { expecting { convert.toBigInteger() } that { isEqualTo(bigInt129) } }

                Constants.TWO_POW_128_UBYTES test { expecting { convert.toBigInteger() } that { isEqualTo(bigInt128) } }
                Constants.TWO_POW_128_PLUS_1_UBYTES test { expecting { convert.toBigInteger() } that { isEqualTo(bigInt129) } }
            }
        }
    }
}
