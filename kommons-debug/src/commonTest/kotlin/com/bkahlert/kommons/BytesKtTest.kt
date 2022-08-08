package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BytesTest {

    @Test fun to_byte_array() = testAll {
        Int.MIN_VALUE.toByteArray() shouldBe byteArrayOf(-128, 0, 0, 0)
        Int.MIN_VALUE.toByteArray(trimmed = false) shouldBe Int.MIN_VALUE.toByteArray()
        Int.MIN_VALUE.toByteArray(trimmed = true) shouldBe byteArrayOf(-128, 0, 0, 0)
        0.toByteArray() shouldBe byteArrayOf(0, 0, 0, 0)
        0.toByteArray(trimmed = false) shouldBe 0.toByteArray()
        0.toByteArray(trimmed = true) shouldBe byteArrayOf(0)
        Int.MAX_VALUE.toByteArray() shouldBe byteArrayOf(127, -1, -1, -1)
        Int.MAX_VALUE.toByteArray(trimmed = false) shouldBe Int.MAX_VALUE.toByteArray()
        Int.MAX_VALUE.toByteArray(trimmed = true) shouldBe byteArrayOf(127, -1, -1, -1)

        Long.MIN_VALUE.toByteArray() shouldBe byteArrayOf(-128, 0, 0, 0, 0, 0, 0, 0)
        Long.MIN_VALUE.toByteArray(trimmed = false) shouldBe Long.MIN_VALUE.toByteArray()
        Long.MIN_VALUE.toByteArray(trimmed = true) shouldBe byteArrayOf(-128, 0, 0, 0, 0, 0, 0, 0)
        0L.toByteArray() shouldBe byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
        0L.toByteArray(trimmed = false) shouldBe 0L.toByteArray()
        0L.toByteArray(trimmed = true) shouldBe byteArrayOf(0)
        Long.MAX_VALUE.toByteArray() shouldBe byteArrayOf(127, -1, -1, -1, -1, -1, -1, -1)
        Long.MAX_VALUE.toByteArray(trimmed = false) shouldBe Long.MAX_VALUE.toByteArray()
        Long.MAX_VALUE.toByteArray(trimmed = true) shouldBe byteArrayOf(127, -1, -1, -1, -1, -1, -1, -1)
    }

    @Test fun to_ubyte_array() = testAll {
        UInt.MIN_VALUE.toUByteArray() shouldBe ubyteArrayOf(0u, 0u, 0u, 0u)
        UInt.MIN_VALUE.toUByteArray(trimmed = false) shouldBe UInt.MIN_VALUE.toUByteArray()
        UInt.MIN_VALUE.toUByteArray(trimmed = true) shouldBe ubyteArrayOf(0u)
        UInt.MAX_VALUE.toUByteArray() shouldBe ubyteArrayOf(255u, 255u, 255u, 255u)
        UInt.MAX_VALUE.toUByteArray(trimmed = false) shouldBe UInt.MAX_VALUE.toUByteArray()
        UInt.MAX_VALUE.toUByteArray(trimmed = true) shouldBe ubyteArrayOf(255u, 255u, 255u, 255u)

        ULong.MIN_VALUE.toUByteArray() shouldBe ubyteArrayOf(0u, 0u, 0u, 0u, 0u, 0u, 0u, 0u)
        ULong.MIN_VALUE.toUByteArray(trimmed = false) shouldBe ULong.MIN_VALUE.toUByteArray()
        ULong.MIN_VALUE.toUByteArray(trimmed = true) shouldBe ubyteArrayOf(0u)
        ULong.MAX_VALUE.toUByteArray() shouldBe ubyteArrayOf(255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u)
        ULong.MAX_VALUE.toUByteArray(trimmed = false) shouldBe ULong.MAX_VALUE.toUByteArray()
        ULong.MAX_VALUE.toUByteArray(trimmed = true) shouldBe ubyteArrayOf(255u, 255u, 255u, 255u, 255u, 255u, 255u, 255u)
    }

    @Suppress("SpellCheckingInspection")
    @Test fun to_hexadecimal_string() = testAll {
        byteArray should { array ->
            array.map { it.toHexadecimalString() } shouldContainExactly listOf("00", "7f", "80", "ff")
            array.toHexadecimalString() shouldBe "007f80ff"
        }
        largeByteArrayOf.toHexadecimalString() shouldBe "ffffffffffffffffffffffffffffffff"
        veryLargeByteArray.toHexadecimalString() shouldBe "0100000000000000000000000000000000"

        0.toHexadecimalString() shouldBe "0"
        Int.MAX_VALUE.toHexadecimalString() shouldBe "7fffffff"

        0L.toHexadecimalString() shouldBe "0"
        Long.MAX_VALUE.toHexadecimalString() shouldBe "7fffffffffffffff"

        ubyteArray should { array ->
            array.map { it.toHexadecimalString() } shouldContainExactly listOf("00", "7f", "80", "ff")
            array.toHexadecimalString() shouldBe "007f80ff"
        }
        largeUbyteArray.toHexadecimalString() shouldBe "ffffffffffffffffffffffffffffffff"
        veryLargeUbyteArrayOf.toHexadecimalString() shouldBe "0100000000000000000000000000000000"

        0u.toHexadecimalString() shouldBe "0"
        UInt.MAX_VALUE.toHexadecimalString() shouldBe "ffffffff"

        0uL.toHexadecimalString() shouldBe "0"
        ULong.MAX_VALUE.toHexadecimalString() shouldBe "ffffffffffffffff"
    }

    @Test fun to_decimal_string() = testAll {
        byteArray should { array ->
            array.map { it.toDecimalString() } shouldContainExactly listOf("0", "127", "128", "255")
        }

        ubyteArray should { array ->
            array.map { it.toDecimalString() } shouldContainExactly listOf("0", "127", "128", "255")
        }
    }

    @Test fun to_octal_string() = testAll {
        byteArray should { array ->
            array.map { it.toOctalString() } shouldContainExactly listOf("000", "177", "200", "377")
            array.toOctalString() shouldBe "000177200377"
        }
        largeByteArrayOf.toOctalString() shouldBe "377377377377377377377377377377377377377377377377"
        veryLargeByteArray.toOctalString() shouldBe "001000000000000000000000000000000000000000000000000"

        0.toOctalString() shouldBe "0"
        Int.MAX_VALUE.toOctalString() shouldBe "17777777777"

        0L.toOctalString() shouldBe "0"
        Long.MAX_VALUE.toOctalString() shouldBe "777777777777777777777"

        ubyteArray should { array ->
            array.map { it.toOctalString() } shouldContainExactly listOf("000", "177", "200", "377")
            array.toOctalString() shouldBe "000177200377"
        }
        largeUbyteArray.toOctalString() shouldBe "377377377377377377377377377377377377377377377377"
        veryLargeUbyteArrayOf.toOctalString() shouldBe "001000000000000000000000000000000000000000000000000"

        0u.toOctalString() shouldBe "0"
        UInt.MAX_VALUE.toOctalString() shouldBe "37777777777"

        0uL.toOctalString() shouldBe "0"
        ULong.MAX_VALUE.toOctalString() shouldBe "1777777777777777777777"
    }

    @Suppress("LongLine")
    @Test fun to_binary_string() = testAll {
        byteArray should { array ->
            array.map { it.toBinaryString() } shouldContainExactly listOf("00000000", "01111111", "10000000", "11111111")
            array.toBinaryString() shouldBe "00000000011111111000000011111111"
        }
        largeByteArrayOf.toBinaryString() shouldBe "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
        veryLargeByteArray.toBinaryString() shouldBe "0000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"

        0.toBinaryString() shouldBe "0"
        Int.MAX_VALUE.toBinaryString() shouldBe "1111111111111111111111111111111"

        0L.toBinaryString() shouldBe "0"
        Long.MAX_VALUE.toBinaryString() shouldBe "111111111111111111111111111111111111111111111111111111111111111"

        ubyteArray should { array ->
            array.map { it.toBinaryString() } shouldContainExactly listOf("00000000", "01111111", "10000000", "11111111")
            array.toBinaryString() shouldBe "00000000011111111000000011111111"
        }
        largeUbyteArray.toBinaryString() shouldBe "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
        veryLargeUbyteArrayOf.toBinaryString() shouldBe "0000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"

        0u.toBinaryString() shouldBe "0"
        UInt.MAX_VALUE.toBinaryString() shouldBe "11111111111111111111111111111111"

        0uL.toBinaryString() shouldBe "0"
        ULong.MAX_VALUE.toBinaryString() shouldBe "1111111111111111111111111111111111111111111111111111111111111111"
    }


    @Test fun encode_to_base64() = base64Bytes.testAll { (bytes, base64) ->
        bytes.encodeToBase64() shouldBe base64
    }

    @Test fun encode_to_base64_url_safe() {
        byteArrayOf(248.toByte()).encodeToBase64(urlSafe = true) shouldBe "-A%3d%3d\r\n"
        byteArrayOf(252.toByte()).encodeToBase64(urlSafe = true) shouldBe "_A%3d%3d\r\n"
    }

    @Test fun encode_to_base64_no_chunking() {
        byteArrayOf(248.toByte()).encodeToBase64(chunked = false) shouldBe "+A=="
        byteArrayOf(252.toByte()).encodeToBase64(chunked = false) shouldBe "/A=="
    }

    @Test fun decode_from_base64() = base64Bytes.testAll { (bytes, base64) ->
        base64.decodeFromBase64() shouldBe bytes
    }

    @Test fun decode_from_base64_url_safe() {
        "-A%3d%3d\r\n".decodeFromBase64() shouldBe byteArrayOf(248.toByte())
        "_A%3d%3d\r\n".decodeFromBase64() shouldBe byteArrayOf(252.toByte())
    }

    @Test fun decode_from_base64_no_chunking() {
        "+A==".decodeFromBase64() shouldBe byteArrayOf(248.toByte())
        "/A==".decodeFromBase64() shouldBe byteArrayOf(252.toByte())
    }
}

internal val ubyteArray = ubyteArrayOf(0x00u, 0x7fu, 0x80u, 0xffu)
internal val byteArray = byteArrayOf(0x00, 0x7f, -0x80, -0x01)
internal val largeUbyteArray = ubyteArrayOf(0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu, 0xffu)
internal val largeByteArrayOf = byteArrayOf(-0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01, -0x01)
internal val veryLargeUbyteArrayOf = ubyteArrayOf(0x1u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u, 0x0u)
internal val veryLargeByteArray = byteArrayOf(0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

@Suppress("SpellCheckingInspection")
internal val base64Bytes = listOf(
    byteArrayOf() to "",
    byteArrayOf(0) to "AA==\r\n",
    byteArrayOf(0, 0) to "AAA=\r\n",
    byteArrayOf(0, 0, 0) to "AAAA\r\n",
    byteArrayOf(0, 0, 0, 0) to "AAAAAA==\r\n",
    byteArrayOf(-1) to "/w==\r\n",
    byteArrayOf(-1, -1) to "//8=\r\n",
    byteArrayOf(-1, -1, -1) to "////\r\n",
    byteArrayOf(-1, -1, -1, -1) to "/////w==\r\n",
)
