package koodies.number

import koodies.test.test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.Byte.Companion.MAX_VALUE
import kotlin.Byte.Companion.MIN_VALUE

@Execution(CONCURRENT)
class BytesKtTest {

    @TestFactory
    fun `should convert to positive int`() =
        listOf(
            0.toByte() to 0,
            MAX_VALUE to 127,
            MIN_VALUE to 128,
            (-1).toByte() to 255,
        ).test { (input, expected) ->
            expectThat(input.toPositiveInt()).isEqualTo(expected)
        }

    @TestFactory
    fun `should convert to hexadecimal string`() =
        listOf(
            0.toByte() to "00",
            MAX_VALUE to "7f",
            MIN_VALUE to "80",
            (-1).toByte() to "ff",
        ).test { (input, expected) ->
            expectThat(input.toHexadecimalString()).isEqualTo(expected)
        }

    @Nested
    inner class ToHexStringKtTest {

        @TestFactory
        fun `should convert to padded hex representation by default`() = listOf(
            0 to "00",
            10 to "0a",
            15 to "0f",
            16 to "10",
            65535 to "ffff",
            65536 to "010000",
        ).test { (dec, hex) ->
            expectThat(dec.toHexadecimalString()).isEqualTo(hex)
        }

        @TestFactory
        fun `should convert to specified hex representation`() = listOf(
            0 to "0",
            10 to "a",
            15 to "f",
            16 to "10",
            65535 to "ffff",
            65536 to "10000",
        ).test { (dec, hex) ->
            expectThat(dec.toHexadecimalString(pad = false)).isEqualTo(hex)
        }
    }

    @TestFactory
    fun `should convert to decimal string`() =
        listOf(
            0.toByte() to "0",
            MAX_VALUE to "127",
            MIN_VALUE to "128",
            (-1).toByte() to "255",
        ).test { (input, expected) ->
            expectThat(input.toDecimalString()).isEqualTo(expected)
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
            ).test { (input, expected) ->
                expectThat(input.toBytes()).isEqualTo(expected)
            }

        @TestFactory
        fun `should convert Int to untrimmed ByteArray`() =
            listOf(
                255 to byteArrayOf(0, 0, 0, -1),
                65535 to byteArrayOf(0, 0, -1, -1),
                16777215 to byteArrayOf(0, -1, -1, -1),
                -1 to byteArrayOf(-1, -1, -1, -1),
            ).test { (input, expected) ->
                expectThat(input.toBytes(trim = false)).isEqualTo(expected)
            }

        @TestFactory
        fun `should convert ByteArray to Int`() =
            listOf(
                byteArrayOf(-1) to 255,
                byteArrayOf(-1, -1) to 65535,
                byteArrayOf(-1, -1, -1) to 16777215,
                byteArrayOf(-1, -1, -1, -1) to -1,
            ).test { (input, expected) ->
                expectThat(input.toInt()).isEqualTo(expected)
            }

        @TestFactory
        fun `should convert ByteArray to Int and back to ByteArray`() =
            listOf(
                byteArrayOf(-1),
                byteArrayOf(-1, -1),
                byteArrayOf(-1, -1, -1),
                byteArrayOf(-1, -1, -1, -1),
            ).test { input ->
                expectThat(input.toInt().toBytes(trim = true)).isEqualTo(input)
            }

        @TestFactory
        fun `should convert ByteArray to Int and back to untrimmed ByteArray`() =
            listOf(
                byteArrayOf(0, 0, 0, -1),
                byteArrayOf(0, 0, -1, -1),
                byteArrayOf(0, -1, -1, -1),
                byteArrayOf(-1, -1, -1, -1),
            ).test { input ->
                expectThat(input.toInt().toBytes(trim = false)).isEqualTo(input)
            }

        @TestFactory
        fun `should convert ByteArray to UInt`() =
            listOf(
                byteArrayOf(-1) to 255u,
                byteArrayOf(-1, -1) to 65535u,
                byteArrayOf(-1, -1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1, -1) to 4294967295u,
            ).test { (input, expected) ->
                expectThat(input.toUInt()).isEqualTo(expected)
            }

        @TestFactory
        fun `should pad ByteArray to UInt`() =
            listOf(
                byteArrayOf(-1) to 16777215u,
                byteArrayOf(-1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1, -1) to 4294967295u,
            ).test { (input, expected) ->
                expectThat(input.padStart(3, -1).toUInt()).isEqualTo(expected)
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
            ).test { (input, expected) ->
                expectThat(input.toUBytes()).isEqualToUnsigned(expected)
            }

        @TestFactory
        fun `should convert UInt to untrimmed UByteArray`() =
            listOf(
                255u to ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xFFu),
                65535u to ubyteArrayOf(0x00u, 0x00u, 0xFFu, 0xFFu),
                16777215u to ubyteArrayOf(0x00u, 0xFFu, 0xFFu, 0xFFu),
                4294967295u to ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).test { (input, expected) ->
                expectThat(input.toUBytes(trim = false)).isEqualToUnsigned(expected)
            }

        @TestFactory
        fun `should convert UByteArray to UInt`() =
            listOf(
                ubyteArrayOf(0xFFu) to 255u,
                ubyteArrayOf(0xFFu, 0xFFu) to 65535u,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215u,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to 4294967295u,
            ).test { (input, expected) ->
                expectThat(input.toUInt()).isEqualTo(expected)
            }

        @TestFactory
        fun `should convert UByteArray to UInt and back to UByteArray`() =
            listOf(
                ubyteArrayOf(0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).test { input ->
                expectThat(input.toUInt().toUBytes(trim = true)).isEqualToUnsigned(input)
            }

        @TestFactory
        fun `should convert UByteArray to UInt and back to untrimmed UByteArray`() =
            listOf(
                ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xFFu),
                ubyteArrayOf(0x00u, 0x00u, 0xFFu, 0xFFu),
                ubyteArrayOf(0x00u, 0xFFu, 0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).test { input ->
                expectThat(input.toUInt().toUBytes(trim = false)).isEqualToUnsigned(input)
            }

        @TestFactory
        fun `should convert UByteArray to Int`() =
            listOf(
                ubyteArrayOf(0xFFu) to 255,
                ubyteArrayOf(0xFFu, 0xFFu) to 65535,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to -1,
            ).test { (input, expected) ->
                expectThat(input.toInt()).isEqualTo(expected)
            }

        @TestFactory
        fun `should pad UByteArray to UInt`() =
            listOf(
                ubyteArrayOf(0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to -1,
            ).test { (input, expected) ->
                expectThat(input.padStart(3, 0xFFu).toInt()).isEqualTo(expected)
            }
    }
}


/**
 * Asserts that the subject is equal to [expected] according to the standard
 * Kotlin `==` operator.
 *
 * @param expected the expected value.
 */
infix fun Assertion.Builder<UByteArray>.isEqualToUnsigned(expected: UByteArray?): Assertion.Builder<UByteArray> =
    assert("is equal to %s", expected) {
        when (it) {
            expected -> pass()
            else -> if (expected is UByteArray && it.contentEquals(expected)) pass() else fail(actual = it)
        }
    }
