package koodies.number

import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
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
        ).testEach { (input, expected) ->
            expect { input.toPositiveInt() }.that { isEqualTo(expected) }
        }

    @TestFactory
    fun `should convert to hexadecimal string`() =
        listOf(
            0.toByte() to "00",
            MAX_VALUE to "7f",
            MIN_VALUE to "80",
            (-1).toByte() to "ff",
        ).testEach { (input, expected) ->
            expect { input.toHexadecimalString() }.that { isEqualTo(expected) }
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
        ).testEach { (dec, hex) ->
            expect { dec.toHexadecimalString() }.that { isEqualTo(hex) }
        }

        @TestFactory
        fun `should convert to specified hex representation`() = listOf(
            0 to "0",
            10 to "a",
            15 to "f",
            16 to "10",
            65535 to "ffff",
            65536 to "10000",
        ).testEach { (dec, hex) ->
            expect { dec.toHexadecimalString(pad = false) }.that { isEqualTo(hex) }
        }
    }

    @TestFactory
    fun `should convert to decimal string`() =
        listOf(
            0.toByte() to "0",
            MAX_VALUE to "127",
            MIN_VALUE to "128",
            (-1).toByte() to "255",
        ).testEach { (input, expected) ->
            expect { input.toDecimalString() }.that { isEqualTo(expected) }
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
                expect { input.toBytes() }.that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert Int to untrimmed ByteArray`() =
            listOf(
                255 to byteArrayOf(0, 0, 0, -1),
                65535 to byteArrayOf(0, 0, -1, -1),
                16777215 to byteArrayOf(0, -1, -1, -1),
                -1 to byteArrayOf(-1, -1, -1, -1),
            ).testEach { (input, expected) ->
                expect { input.toBytes(trim = false) }.that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert ByteArray to Int`() =
            listOf(
                byteArrayOf(-1) to 255,
                byteArrayOf(-1, -1) to 65535,
                byteArrayOf(-1, -1, -1) to 16777215,
                byteArrayOf(-1, -1, -1, -1) to -1,
            ).testEach { (input, expected) ->
                expect { input.toInt() }.that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert ByteArray to Int and back to ByteArray`() =
            listOf(
                byteArrayOf(-1),
                byteArrayOf(-1, -1),
                byteArrayOf(-1, -1, -1),
                byteArrayOf(-1, -1, -1, -1),
            ).testEach { input ->
                expect { input.toInt().toBytes(trim = true) }.that { isEqualTo(input) }
            }

        @TestFactory
        fun `should convert ByteArray to Int and back to untrimmed ByteArray`() =
            listOf(
                byteArrayOf(0, 0, 0, -1),
                byteArrayOf(0, 0, -1, -1),
                byteArrayOf(0, -1, -1, -1),
                byteArrayOf(-1, -1, -1, -1),
            ).testEach { input ->
                expect { input.toInt().toBytes(trim = false) }.that { isEqualTo(input) }
            }

        @TestFactory
        fun `should convert ByteArray to UInt`() =
            listOf(
                byteArrayOf(-1) to 255u,
                byteArrayOf(-1, -1) to 65535u,
                byteArrayOf(-1, -1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1, -1) to 4294967295u,
            ).testEach { (input, expected) ->
                expect { input.toUInt() }.that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should pad ByteArray to UInt`() =
            listOf(
                byteArrayOf(-1) to 16777215u,
                byteArrayOf(-1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1) to 16777215u,
                byteArrayOf(-1, -1, -1, -1) to 4294967295u,
            ).testEach { (input, expected) ->
                expect { input.padStart(3, -1).toUInt() }.that { isEqualTo(expected) }
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
                expect { input.toUBytes() }.that { isEqualToUnsigned(expected) }
            }

        @TestFactory
        fun `should convert UInt to untrimmed UByteArray`() =
            listOf(
                255u to ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xFFu),
                65535u to ubyteArrayOf(0x00u, 0x00u, 0xFFu, 0xFFu),
                16777215u to ubyteArrayOf(0x00u, 0xFFu, 0xFFu, 0xFFu),
                4294967295u to ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEach { (input, expected) ->
                expect { input.toUBytes(trim = false) }.that { isEqualToUnsigned(expected) }
            }

        @TestFactory
        fun `should convert UByteArray to UInt`() =
            listOf(
                ubyteArrayOf(0xFFu) to 255u,
                ubyteArrayOf(0xFFu, 0xFFu) to 65535u,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215u,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to 4294967295u,
            ).testEach { (input, expected) ->
                expect { input.toUInt() }.that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should convert UByteArray to UInt and back to UByteArray`() =
            listOf(
                ubyteArrayOf(0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEach { input ->
                expect { input.toUInt().toUBytes(trim = true) }.that { isEqualToUnsigned(input) }
            }

        @TestFactory
        fun `should convert UByteArray to UInt and back to untrimmed UByteArray`() =
            listOf(
                ubyteArrayOf(0x00u, 0x00u, 0x00u, 0xFFu),
                ubyteArrayOf(0x00u, 0x00u, 0xFFu, 0xFFu),
                ubyteArrayOf(0x00u, 0xFFu, 0xFFu, 0xFFu),
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu),
            ).testEach { input ->
                expect { input.toUInt().toUBytes(trim = false) }.that { isEqualToUnsigned(input) }
            }

        @TestFactory
        fun `should convert UByteArray to Int`() =
            listOf(
                ubyteArrayOf(0xFFu) to 255,
                ubyteArrayOf(0xFFu, 0xFFu) to 65535,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to -1,
            ).testEach { (input, expected) ->
                expect { input.toInt() }.that { isEqualTo(expected) }
            }

        @TestFactory
        fun `should pad UByteArray to UInt`() =
            listOf(
                ubyteArrayOf(0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu) to 16777215,
                ubyteArrayOf(0xFFu, 0xFFu, 0xFFu, 0xFFu) to -1,
            ).testEach { (input, expected) ->
                expect { input.padStart(3, 0xFFu).toInt() }.that { isEqualTo(expected) }
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
