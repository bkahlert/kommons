package koodies.math

import koodies.math.Constants.TWO_POW_128_BIN_STRING
import koodies.math.Constants.TWO_POW_128_BYTES
import koodies.math.Constants.TWO_POW_128_DEC_STRING
import koodies.math.Constants.TWO_POW_128_HEX_STRING
import koodies.math.Constants.TWO_POW_128_PLUS_1_BIN_STRING
import koodies.math.Constants.TWO_POW_128_PLUS_1_BYTES
import koodies.math.Constants.TWO_POW_128_PLUS_1_DEC_STRING
import koodies.math.Constants.TWO_POW_128_PLUS_1_HEX_STRING
import koodies.math.Constants.TWO_POW_128_PLUS_1_UBYTES
import koodies.math.Constants.TWO_POW_128_UBYTES
import koodies.test.test
import koodies.test.testEach
import koodies.test.tests
import koodies.transform.convert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.assertions.contentEquals
import strikt.assertions.isEqualTo
import kotlin.Byte.Companion.MAX_VALUE
import kotlin.Byte.Companion.MIN_VALUE

@Execution(SAME_THREAD)
class BytesKtTest {

    private val bytes = listOf<Byte>(0x00, 0x7f, -0x80, -0x01)
    private val ubytes = listOf<UByte>(0x00u, 0x7fu, 0x80u, 0xffu)

    private val paddedBinBytes = listOf("00000000", "01111111", "10000000", "11111111")
    private val nonPaddedBinBytes = listOf("0", "1111111", "10000000", "11111111")
    private val decBytes = listOf("0", "127", "128", "255")
    private val paddedHexBytes = listOf("00", "7f", "80", "ff")
    private val nonPaddedHexBytes = listOf("0", "7f", "80", "ff")

    private val paddedBinString = "00000000011111111000000011111111"
    private val nonPaddedBinString = "11111111000000011111111"
    private val decString = "8356095"
    private val paddedHexString = "007f80ff"
    private val nonPaddedHexString = "7f80ff"

    @TestFactory
    fun `should convert to positive int`() =
        listOf(
            0.toByte() to 0,
            MAX_VALUE to 127,
            MIN_VALUE to 128,
            (-1).toByte() to 255,
        ).testEach { (input, expected) ->
            expecting { input.toPositiveInt() } that { isEqualTo(expected) }
        }

    @Nested
    inner class Bytes {

        private val bigNumbers = mapOf(
            TWO_POW_128_PLUS_1_BYTES to Triple(TWO_POW_128_PLUS_1_BIN_STRING, TWO_POW_128_PLUS_1_DEC_STRING, TWO_POW_128_PLUS_1_HEX_STRING),
            TWO_POW_128_BYTES to Triple(TWO_POW_128_BIN_STRING, TWO_POW_128_DEC_STRING, TWO_POW_128_HEX_STRING)
        )

        @TestFactory
        fun `should convert bytes`() = tests {
            bytes all {
                expecting { map { it.toBinaryString() } } that { isEqualTo(paddedBinBytes) }
                expecting { map { it.toBinaryString(pad = true) } } that { isEqualTo(paddedBinBytes) }
                expecting { map { it.toBinaryString(pad = false) } } that { isEqualTo(nonPaddedBinBytes) }

                expecting { map { it.toDecimalString() } } that { isEqualTo(decBytes) }

                expecting { map { it.toHexadecimalString() } } that { isEqualTo(paddedHexBytes) }
                expecting { map { it.toHexadecimalString(pad = true) } } that { isEqualTo(paddedHexBytes) }
                expecting { map { it.toHexadecimalString(pad = false) } } that { isEqualTo(nonPaddedHexBytes) }
            }

            bytes.toByteArray() all {
                expecting { toBinaryString() } that { isEqualTo(paddedBinString) }
                expecting { toBinaryString(pad = true) } that { isEqualTo(paddedBinString) }
                expecting { toBinaryString(pad = false) } that { isEqualTo(nonPaddedBinString) }

                expecting { toDecimalString() } that { isEqualTo(decString) }

                expecting { toHexadecimalString() } that { isEqualTo(paddedHexString) }
                expecting { toHexadecimalString(pad = true) } that { isEqualTo(paddedHexString) }
                expecting { toHexadecimalString(pad = false) } that { isEqualTo(nonPaddedHexString) }
            }
        }

        @TestFactory
        fun `should convert large numbers`() = bigNumbers.toList().testEach { (bytes: ByteArray, strings: Triple<String, String, String>) ->
            expecting { bytes.toBinaryString() } that { isEqualTo(strings.first) }
            expecting { byteArrayOfBinaryString(bytes.toBinaryString()) } that { isEqualTo(bytes) }
            expecting { bytes.toDecimalString() } that { isEqualTo(strings.second) }
            expecting { byteArrayOfDecimalString(bytes.toDecimalString()) } that { isEqualTo(bytes) }
            expecting { bytes.toHexadecimalString() } that { isEqualTo(strings.third) }
            expecting { byteArrayOfHexadecimalString(bytes.toHexadecimalString()) } that { isEqualTo(bytes) }
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
        fun `should convert ubytes`() = tests {
            ubytes all {
                expecting { map { it.toBinaryString() } } that { isEqualTo(paddedBinBytes) }
                expecting { map { it.toBinaryString(pad = true) } } that { isEqualTo(paddedBinBytes) }
                expecting { map { it.toBinaryString(pad = false) } } that { isEqualTo(nonPaddedBinBytes) }

                expecting { map { it.toDecimalString() } } that { isEqualTo(decBytes) }

                expecting { map { it.toHexadecimalString() } } that { isEqualTo(paddedHexBytes) }
                expecting { map { it.toHexadecimalString(pad = true) } } that { isEqualTo(paddedHexBytes) }
                expecting { map { it.toHexadecimalString(pad = false) } } that { isEqualTo(nonPaddedHexBytes) }
            }

            ubytes.toUByteArray() all {
                expecting { toBinaryString() } that { isEqualTo(paddedBinString) }
                expecting { toBinaryString(pad = true) } that { isEqualTo(paddedBinString) }
                expecting { toBinaryString(pad = false) } that { isEqualTo(nonPaddedBinString) }

                expecting { toDecimalString() } that { isEqualTo(decString) }

                expecting { toHexadecimalString() } that { isEqualTo(paddedHexString) }
                expecting { toHexadecimalString(pad = true) } that { isEqualTo(paddedHexString) }
                expecting { toHexadecimalString(pad = false) } that { isEqualTo(nonPaddedHexString) }
            }
        }

        @TestFactory
        fun `should convert large numbers`() = bigNumbers.toList().testEach { (ubytes: UByteArray, strings: Triple<String, String, String>) ->
            expecting { ubytes.toBinaryString() } that { isEqualTo(strings.first) }
            expecting { ubyteArrayOfBinaryString(ubytes.toBinaryString()) } that { isEqualToUnsigned(ubytes) }
            expecting { ubytes.toDecimalString() } that { isEqualTo(strings.second) }
            expecting { ubyteArrayOfDecimalString(ubytes.toDecimalString()) } that { isEqualToUnsigned(ubytes) }
            expecting { ubytes.toHexadecimalString() } that { isEqualTo(strings.third) }
            expecting { ubyteArrayOfHexadecimalString(ubytes.toHexadecimalString()) } that { isEqualToUnsigned(ubytes) }
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
        fun `from 128 Bits binary string`() = test(binString128) {
            expecting { this } that { isEqualTo(binString128) }
            expecting { convert.asBinaryString.toByteArray() } that { isEqualTo(bytes128) }
            expecting { byteArrayOfBinaryString(this) } that { isEqualTo(bytes128) }
            expecting { convert.asBinaryString.toByteArray().toUByteArray() } that { isEqualToUnsigned(ubytes128) }
            expecting { ubyteArrayOfBinaryString(this) } that { isEqualToUnsigned(ubytes128) }
            expecting { convert.asBinaryString.toBigInteger() } that { isEqualTo(bigInt128) }
            expecting { bigIntegerOfBinaryString(this) } that { isEqualTo(bigInt128) }
        }

        @TestFactory
        fun `from 129 Bits binary string`() = test(binString129) {
            expecting { this } that { isEqualTo(binString129) }
            expecting { convert.asBinaryString.toByteArray() } that { isEqualTo(bytes129) }
            expecting { byteArrayOfBinaryString(this) } that { isEqualTo(bytes129) }
            expecting { convert.asBinaryString.toByteArray().toUByteArray() } that { isEqualToUnsigned(ubytes129) }
            expecting { ubyteArrayOfBinaryString(this) } that { isEqualToUnsigned(ubytes129) }
            expecting { convert.asBinaryString.toBigInteger() } that { isEqualTo(bigInt129) }
            expecting { bigIntegerOfBinaryString(this) } that { isEqualTo(bigInt129) }
        }

        @TestFactory
        fun `from 128 Bits byte array`() = test(bytes128) {
            expecting { convert.toBinaryString(padding = false) } that { isEqualTo(binString128) }
            expecting { convert.toBinaryString(padding = true) } that { isEqualTo(binString128) }
            expecting { this } that { isEqualTo(bytes128) }
            expecting { convert.toUByteArray() } that { isEqualToUnsigned(ubytes128) }
            expecting { convert.toBigInteger() } that { isEqualTo(bigInt128) }
        }

        @TestFactory
        fun `from 129 Bits byte array`() = test(bytes129) {
            expecting { convert.toBinaryString(padding = false) } that { isEqualTo(binString129.substring(7)) }
            expecting { convert.toBinaryString(padding = true) } that { isEqualTo(binString129) }
            expecting { this } that { isEqualTo(bytes129) }
            expecting { convert.toUByteArray() } that { isEqualToUnsigned(ubytes129) }
            expecting { convert.toBigInteger() } that { isEqualTo(bigInt129) }
        }

        @TestFactory
        fun `from 128 Bits unsigned byte array`() = test(ubytes128) {
            expecting { convert.toBinaryString(padding = false) } that { isEqualTo(binString128) }
            expecting { convert.toBinaryString(padding = true) } that { isEqualTo(binString128) }
            expecting { convert.toByteArray() } that { isEqualTo(bytes128) }
            expecting { this } that { isEqualToUnsigned(ubytes128) }
            expecting { convert.toBigInteger() } that { isEqualTo(bigInt128) }
        }

        @TestFactory
        fun `from 129 Bits unsigned byte array`() = test(ubytes129) {
            expecting { convert.toBinaryString(padding = false) } that { isEqualTo(binString129.substring(7)) }
            expecting { convert.toBinaryString(padding = true) } that { isEqualTo(binString129) }
            expecting { convert.toByteArray() } that { isEqualTo(bytes129) }
            expecting { this } that { isEqualToUnsigned(ubytes129) }
            expecting { convert.toBigInteger() } that { isEqualTo(bigInt129) }
        }

        @TestFactory
        fun `from 128 Bits big integer`() = test(bigInt128) {
            expecting { convert.asUnsigned.toBinaryString(padding = false) } that { isEqualTo(binString128) }
            expecting { convert.asUnsigned.toBinaryString(padding = true) } that { isEqualTo(binString128) }
            expecting { convert.asUnsigned.toByteArray() } that { contentEquals(bytes128) }
            expecting { convert.asUnsigned.toUByteArray() } that { isEqualToUnsigned(ubytes128) }
            expecting { this } that { isEqualTo(bigInt128) }
        }

        @TestFactory
        fun `from 129 Bits big integer`() = test(bigInt129) {
            expecting { convert.asUnsigned.toBinaryString(padding = false) } that { isEqualTo(binString129.substring(7)) }
            expecting { convert.asUnsigned.toBinaryString(padding = true) } that { isEqualTo(binString129) }
            expecting { convert.asUnsigned.toByteArray() } that { contentEquals(bytes129) }
            expecting { convert.asUnsigned.toUByteArray() } that { isEqualToUnsigned(ubytes129) }
            expecting { this } that { isEqualTo(bigInt129) }
        }
    }

    @TestFactory
    fun `should convert to specified hexadecimal string`() = listOf(
        0 to "0",
        10 to "a",
        15 to "f",
        16 to "10",
        65535 to "ffff",
        65536 to "10000",
    ).testEach { (dec, hex) ->
        expecting { dec.toHexadecimalString(pad = false) } that { isEqualTo(hex) }
    }


    @TestFactory
    fun `should convert to decimal string`() =
        listOf(
            0.toByte() to "0",
            MAX_VALUE to "127",
            MIN_VALUE to "128",
            (-1).toByte() to "255",
        ).testEach { (input, expected) ->
            expecting { input.toDecimalString() } that { isEqualTo(expected) }
        }

    @Nested
    inner class SignedInt {

        @TestFactory
        fun `should convert Int to ByteArray`() =
            listOf(
                255 to byteArrayOf(-1),
                65535 to byteArrayOf(-1, -1),
                16777215 to byteArrayOf(-1, -1, -1),
                -1 to byteArrayOf(-1, -1, -1, -1),
            ).testEach { (input, expected) ->
                expecting { input.toBytes() } that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert Int to untrimmed ByteArray`() =
            listOf(
                255 to byteArrayOf(0, 0, 0, -1),
                65535 to byteArrayOf(0, 0, -1, -1),
                16777215 to byteArrayOf(0, -1, -1, -1),
                -1 to byteArrayOf(-1, -1, -1, -1),
            ).testEach { (input, expected) ->
                expecting { input.toBytes(trim = false) } that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert ByteArray to Int`() =
            listOf(
                byteArrayOf(-1) to 255,
                byteArrayOf(-1, -1) to 65535,
                byteArrayOf(-1, -1, -1) to 16777215,
                byteArrayOf(-1, -1, -1, -1) to -1,
            ).testEach { (input, expected) ->
                expecting { input.toInt() } that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert ByteArray to Int and back to ByteArray`() =
            listOf(
                byteArrayOf(-1),
                byteArrayOf(-1, -1),
                byteArrayOf(-1, -1, -1),
                byteArrayOf(-1, -1, -1, -1),
            ).testEach { input ->
                expecting { input.toInt().toBytes(trim = true) } that { isEqualTo(input) }
            }

        @TestFactory
        fun `should convert ByteArray to Int and back to untrimmed ByteArray`() =
            listOf(
                byteArrayOf(0, 0, 0, -1),
                byteArrayOf(0, 0, -1, -1),
                byteArrayOf(0, -1, -1, -1),
                byteArrayOf(-1, -1, -1, -1),
            ).testEach { input ->
                expecting { input.toInt().toBytes(trim = false) } that { isEqualTo(input) }
            }

        @TestFactory
        fun `should convert ByteArray to UInt`() =
            listOf(
                byteArrayOf(-1) to 255u,
                byteArrayOf(-1, -1) to 65535u,
                byteArrayOf(-1, -1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1, -1) to 4294967295u,
            ).testEach { (input, expected) ->
                expecting { input.toUInt() } that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should pad ByteArray to UInt`() =
            listOf(
                byteArrayOf(-1) to 16777215u,
                byteArrayOf(-1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1, -1) to 4294967295u,
            ).testEach { (input, expected) ->
                expecting { input.padStart(3, -1).toUInt() } that { isEqualTo(expected) }
            }
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
            ).testEach { (input, expected) ->
                expecting { input.toUBytes() } that { isEqualToUnsigned(expected) }
            }

        @TestFactory
        fun `should convert UInt to untrimmed UByteArray`() =
            listOf(
                255u to ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xFFu),
                65535u to ubyteArrayOf(0x00u, 0x00u, 0xFFu, 0xFFu),
                16777215u to ubyteArrayOf(0x00u, 0xFFu, 0xFFu, 0xFFu),
                4294967295u to ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEach { (input, expected) ->
                expecting { input.toUBytes(trim = false) } that { isEqualToUnsigned(expected) }
            }

        @TestFactory
        fun `should convert UByteArray to UInt`() =
            listOf(
                ubyteArrayOf(0xFFu) to 255u,
                ubyteArrayOf(0xFFu, 0xFFu) to 65535u,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215u,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to 4294967295u,
            ).testEach { (input, expected) ->
                expecting { input.toUInt() } that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert UByteArray to UInt and back to UByteArray`() =
            listOf(
                ubyteArrayOf(0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEach { input ->
                expecting { input.toUInt().toUBytes(trim = true) } that { isEqualToUnsigned(input) }
            }

        @TestFactory
        fun `should convert UByteArray to UInt and back to untrimmed UByteArray`() =
            listOf(
                ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xFFu),
                ubyteArrayOf(0x00u, 0x00u, 0xFFu, 0xFFu),
                ubyteArrayOf(0x00u, 0xFFu, 0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEach { input ->
                expecting { input.toUInt().toUBytes(trim = false) } that { isEqualToUnsigned(input) }
            }

        @TestFactory
        fun `should convert UByteArray to Int`() =
            listOf(
                ubyteArrayOf(0xFFu) to 255,
                ubyteArrayOf(0xFFu, 0xFFu) to 65535,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to -1,
            ).testEach { (input, expected) ->
                expecting { input.toInt() } that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should pad UByteArray to UInt`() =
            listOf(
                ubyteArrayOf(0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to -1,
            ).testEach { (input, expected) ->
                expecting { input.padStart(3, 0xFFu).toInt() } that { isEqualTo(expected) }
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
