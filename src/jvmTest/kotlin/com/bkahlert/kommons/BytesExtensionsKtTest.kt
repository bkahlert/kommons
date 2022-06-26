package com.bkahlert.kommons

import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.testOld
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

// TODO delete
class BytesExtensionsKtTest {

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
}

private const val TWO_POW_128_PLUS_1_HEX_STRING: String =
    "01" +
        "00" + "00" + "00" + "00" +
        "00" + "00" + "00" + "00" +
        "00" + "00" + "00" + "00" +
        "00" + "00" + "00" + "00"
private const val TWO_POW_128_PLUS_1_DEC_STRING: String =
    "340282366920938463463374607431768211456"
private const val TWO_POW_128_PLUS_1_BIN_STRING: String =
    "00000001" +
        "00000000" + "00000000" + "00000000" + "00000000" +
        "00000000" + "00000000" + "00000000" + "00000000" +
        "00000000" + "00000000" + "00000000" + "00000000" +
        "00000000" + "00000000" + "00000000" + "00000000"
private val TWO_POW_128_PLUS_1_BYTES: ByteArray = byteArrayOf(
    0x1,
    0x0, 0x0, 0x0, 0x0,
    0x0, 0x0, 0x0, 0x0,
    0x0, 0x0, 0x0, 0x0,
    0x0, 0x0, 0x0, 0x0,
)
private val TWO_POW_128_PLUS_1_UBYTES: UByteArray = ubyteArrayOf(
    0x1u,
    0x0u, 0x0u, 0x0u, 0x0u,
    0x0u, 0x0u, 0x0u, 0x0u,
    0x0u, 0x0u, 0x0u, 0x0u,
    0x0u, 0x0u, 0x0u, 0x0u,
)

private const val TWO_POW_128_HEX_STRING: String = "" +
    "ff" + "ff" + "ff" + "ff" +
    "ff" + "ff" + "ff" + "ff" +
    "ff" + "ff" + "ff" + "ff" +
    "ff" + "ff" + "ff" + "ff"
private const val TWO_POW_128_DEC_STRING: String =
    "340282366920938463463374607431768211455"
private const val TWO_POW_128_BIN_STRING: String = "" +
    "11111111" + "11111111" + "11111111" + "11111111" +
    "11111111" + "11111111" + "11111111" + "11111111" +
    "11111111" + "11111111" + "11111111" + "11111111" +
    "11111111" + "11111111" + "11111111" + "11111111"
private val TWO_POW_128_BYTES: ByteArray = byteArrayOf(
    -0x01, -0x01, -0x01, -0x01,
    -0x01, -0x01, -0x01, -0x01,
    -0x01, -0x01, -0x01, -0x01,
    -0x01, -0x01, -0x01, -0x01,
)
private val TWO_POW_128_UBYTES: UByteArray = ubyteArrayOf(
    0xffu, 0xffu, 0xffu, 0xffu,
    0xffu, 0xffu, 0xffu, 0xffu,
    0xffu, 0xffu, 0xffu, 0xffu,
    0xffu, 0xffu, 0xffu, 0xffu,
)
