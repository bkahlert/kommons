package com.bkahlert.kommons

import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.Constants.TWO_POW_128_BIN_STRING
import com.bkahlert.kommons.math.Constants.TWO_POW_128_BYTES
import com.bkahlert.kommons.math.Constants.TWO_POW_128_DEC_STRING
import com.bkahlert.kommons.math.Constants.TWO_POW_128_HEX_STRING
import com.bkahlert.kommons.math.Constants.TWO_POW_128_PLUS_1_BIN_STRING
import com.bkahlert.kommons.math.Constants.TWO_POW_128_PLUS_1_BYTES
import com.bkahlert.kommons.math.Constants.TWO_POW_128_PLUS_1_DEC_STRING
import com.bkahlert.kommons.math.Constants.TWO_POW_128_PLUS_1_HEX_STRING
import com.bkahlert.kommons.math.Constants.TWO_POW_128_PLUS_1_UBYTES
import com.bkahlert.kommons.math.Constants.TWO_POW_128_UBYTES
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.testOld
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.assertions.isEqualTo
import kotlin.Byte.Companion.MAX_VALUE
import kotlin.Byte.Companion.MIN_VALUE

// TODO delete
class BytesExtensionsKtTest {

    @TestFactory
    fun `should convert to positive int`() =
        listOf(
            0.toByte() to 0,
            MAX_VALUE to 127,
            MIN_VALUE to 128,
            (-1).toByte() to 255,
        ).testEachOld { (input, expected) ->
            expecting { input.toInt() and 0xFF } that { isEqualTo(expected) }
        }

    @Nested
    inner class Bytes {

        private val bigNumbers = mapOf(
            TWO_POW_128_PLUS_1_BYTES to Triple(TWO_POW_128_PLUS_1_BIN_STRING, TWO_POW_128_PLUS_1_DEC_STRING, TWO_POW_128_PLUS_1_HEX_STRING),
            TWO_POW_128_BYTES to Triple(TWO_POW_128_BIN_STRING, TWO_POW_128_DEC_STRING, TWO_POW_128_HEX_STRING)
        )

        @TestFactory
        fun `should convert large numbers`() = bigNumbers.toList().testEachOld { (bytes: ByteArray, strings: Triple<String, String, String>) ->
            with { bigIntegerOf(bytes) } then {
                asserting { isEqualTo(bigIntegerOfBinaryString(strings.first)) }
                asserting { isEqualTo(bigIntegerOfDecimalString(strings.second)) }
                asserting { isEqualTo(bigIntegerOfHexadecimalString(strings.third)) }
            }
        }
    }

    @Nested
    inner class UBytes {

        private val bigNumbers = mapOf(
            TWO_POW_128_PLUS_1_UBYTES to Triple(TWO_POW_128_PLUS_1_BIN_STRING, TWO_POW_128_PLUS_1_DEC_STRING, TWO_POW_128_PLUS_1_HEX_STRING),
            TWO_POW_128_UBYTES to Triple(TWO_POW_128_BIN_STRING, TWO_POW_128_DEC_STRING, TWO_POW_128_HEX_STRING)
        )


        @TestFactory
        fun `should convert large numbers`() = bigNumbers.toList().testEachOld { (ubytes: UByteArray, strings: Triple<String, String, String>) ->
            with { bigIntegerOf(ubytes) } then {
                asserting { isEqualTo(bigIntegerOfBinaryString(strings.first)) }
                asserting { isEqualTo(bigIntegerOfDecimalString(strings.second)) }
                asserting { isEqualTo(bigIntegerOfHexadecimalString(strings.third)) }
            }
        }
    }


    @Nested
    inner class BinaryRepresentation {

        private val binString128 = TWO_POW_128_BIN_STRING
        private val binString129 = TWO_POW_128_PLUS_1_BIN_STRING

        private val bytes128 = TWO_POW_128_BYTES
        private val bytes129 = TWO_POW_128_PLUS_1_BYTES

        private val ubytes128 = TWO_POW_128_UBYTES
        private val ubytes129 = TWO_POW_128_PLUS_1_UBYTES

        private val bigInt128 = BigInteger.TWO.pow(128).dec()
        private val bigInt129 = BigInteger.TWO.pow(128)

        @TestFactory
        fun `from 128 Bits binary string`() = testOld(binString128) {
            expecting { this } that { isEqualTo(binString128) }
            expecting { bigIntegerOfBinaryString(this) } that { isEqualTo(bigInt128) }
        }

        @TestFactory
        fun `from 129 Bits binary string`() = testOld(binString129) {
            expecting { this } that { isEqualTo(binString129) }
            expecting { bigIntegerOfBinaryString(this) } that { isEqualTo(bigInt129) }
        }
    }

    @TestFactory
    fun `should convert to specified hexadecimal string`() = listOf(
        0 to "00",
        10 to "0a",
        15 to "0f",
        16 to "10",
        65535 to "ffff",
        65536 to "010000",
    ).testEachOld { (dec, hex) ->
        expecting { dec.toHexadecimalString() } that { isEqualTo(hex) }
    }


    @Nested
    inner class UnsignedInt {

        @TestFactory
        fun `should convert UInt to UByteArray`() =
            listOf(
                255u to ubyteArrayOf(0xFFu),
                65535u to ubyteArrayOf(0xFFu, 0xFFu),
                16777215u to ubyteArrayOf(0xFFu, 0xFFu, 0xFFu),
                4294967295u to ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEachOld { (input, expected) ->
                expecting { input.toUBytes() } that { isEqualToUnsigned(expected) }
            }

        @TestFactory
        fun `should convert UInt to untrimmed UByteArray`() =
            listOf(
                255u to ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xFFu),
                65535u to ubyteArrayOf(0x00u, 0x00u, 0xFFu, 0xFFu),
                16777215u to ubyteArrayOf(0x00u, 0xFFu, 0xFFu, 0xFFu),
                4294967295u to ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEachOld { (input, expected) ->
                expecting { input.toUBytes(trim = false) } that { isEqualToUnsigned(expected) }
            }
    }
}


/**
 * Asserts that the subject is equal to [expected] according to the standard
 * Kotlin `==` operator.
 *
 * @param expected the expected value.
 */
infix fun Assertion.Builder<UByteArray>.isEqualToUnsigned(expected: UByteArray): Assertion.Builder<UByteArray> =
    assert("is equal to %s", expected) {
        when (it) {
            expected -> pass()
            else -> if (it.contentEquals(expected)) pass() else fail(actual = it)
        }
    }
