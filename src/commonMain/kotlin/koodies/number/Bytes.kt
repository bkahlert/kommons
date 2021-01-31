package koodies.number

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign

private val hexChars = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
private fun String.format(length: Int, pad: Boolean = false) = if (pad) padStart(length, '0') else dropWhile { it == '0' }.takeUnless { it.isEmpty() } ?: "0"

val Byte.Companion.ZERO: Byte get() = 0x0
val UByte.Companion.ZEROu: UByte get() = 0x0u
inline val Byte.Companion.OO: Byte get() = ZERO
inline val Byte.Companion.FF: Byte get() = -1

fun Byte.toPositiveInt() = toInt() and 0xFF

fun Byte.toBinaryString(pad: Boolean = true) = toPositiveInt().toString(2).format(Byte.SIZE_BITS, pad)
fun Byte.toDecimalString() = toPositiveInt().toString(10)
fun Byte.toHexadecimalString(pad: Boolean = true) = toPositiveInt().toString(16).format(2, pad)
fun ByteArray.toBinaryString(pad: Boolean = true) = joinToString("") { it.toBinaryString(true) }.format(size * 2, pad)
fun ByteArray.toDecimalString() = BigInteger.fromByteArray(this, Sign.POSITIVE).toString(10)
fun ByteArray.toHexadecimalString(pad: Boolean = true) = joinToString("") { it.toHexadecimalString(true) }.format(size * 2, pad)
fun byteArrayOfBinaryString(binaryString: String) = BigInteger.parseString(binaryString, 2).toByteArray().trim()
fun byteArrayOfDecimalString(decimalString: String) = BigInteger.parseString(decimalString, 10).toByteArray().trim()
fun byteArrayOfHexadecimalString(hexadecimalString: String) = BigInteger.parseString(hexadecimalString, 16).toByteArray().trim()
fun bigIntegerOf(byteArray: ByteArray) = BigInteger.fromByteArray(byteArray, Sign.POSITIVE)

fun UByte.toBinaryString(pad: Boolean = true) = toInt().toString(2).format(Byte.SIZE_BITS, pad)
fun UByte.toDecimalString() = toInt().toString(10)
fun UByte.toHexadecimalString(pad: Boolean = true) = toInt().toString(16).format(2, pad)
fun UByteArray.toBinaryString(pad: Boolean = true) = joinToString("") { it.toBinaryString(true) }.format(count() * 2, pad)
fun UByteArray.toDecimalString() = BigInteger.fromUByteArray(this, Sign.POSITIVE).toString(10)
fun UByteArray.toHexadecimalString(pad: Boolean = true) = joinToString("") { it.toHexadecimalString(true) }.format(count() * 2, pad)
fun ubyteArrayOfBinaryString(binaryString: String) = BigInteger.parseString(binaryString, 2).toUByteArray().trim()
fun ubyteArrayOfDecimalString(decimalString: String) = BigInteger.parseString(decimalString, 10).toUByteArray().trim()
fun ubyteArrayOfHexadecimalString(hexadecimalString: String) = BigInteger.parseString(hexadecimalString, 16).toUByteArray().trim()
fun bigIntegerOf(ubyteArray: UByteArray) = BigInteger.fromUByteArray(ubyteArray, Sign.POSITIVE)

fun bigIntegerOfBinaryString(binaryString: String) = BigInteger.parseString(binaryString, 2)
fun bigIntegerOfDecimalString(decimalString: String) = BigInteger.parseString(decimalString, 10)
fun bigIntegerOfHexadecimalString(hexadecimalString: String) = BigInteger.parseString(hexadecimalString, 16)

fun Int.toHexadecimalString(pad: Boolean = true): String {
    var rem: Int
    var decimal = this
    val hex = StringBuilder()
    if (decimal == 0) return if (pad) "00" else "0"
    while (decimal > 0) {
        rem = decimal % 16
        hex.append(hexChars[rem])
        decimal /= 16
    }
    return hex.reversed().toString().let {
        if (it.length.mod(2) == 1 && pad) "0$it" else it
    }
}


val UByte.Companion.ZERO: UByte get() = 0x0u
inline val UByte.Companion.OO: UByte get() = ZERO

fun Int.toBytes(trim: Boolean = true): ByteArray =
    0.until(Int.SIZE_BYTES)
        .map { i -> (shr((Int.SIZE_BYTES - i - 1) * Byte.SIZE_BITS) and 0xFF).toByte() }
        .let { if (trim) it.dropWhile { byte -> byte == 0.toByte() } else it }
        .toByteArray()

fun UInt.toUBytes(trim: Boolean = true): UByteArray =
    0.until(Int.SIZE_BYTES)
        .map { i -> shr((UInt.SIZE_BYTES - i - 1) * UByte.SIZE_BITS).toUByte() }
        .let { if (trim) it.dropWhile { byte -> byte == 0.toUByte() } else it }
        .toUByteArray()

fun Iterable<Byte>.toInt() = toList().toByteArray().toInt()
fun Iterable<UByte>.toUInt() = toList().toUByteArray().toUInt()

fun ByteArray.toInt(): Int {
    require(size <= Int.SIZE_BYTES) { "The byte array must not consist of more than ${Int.SIZE_BYTES} bytes." }
    return fold(0) { value, byte -> value.shl(Byte.SIZE_BITS) + byte.toPositiveInt() }
}

fun ByteArray.toUInt(): UInt = toInt().toUInt()

fun UByteArray.toUInt(): UInt {
    require(count() <= UInt.SIZE_BYTES) { "The byte array must not consist of more than ${UInt.SIZE_BYTES} bytes." }
    return fold(0u) { value, byte -> value.shl(UByte.SIZE_BITS) + byte }
}

fun UByteArray.toInt(): Int = toUInt().toInt()


fun ByteArray.trim(): ByteArray = dropWhile { it == Byte.ZERO }.takeUnless { it.isEmpty() }?.toByteArray() ?: byteArrayOf(Byte.ZERO)
fun UByteArray.trim(): UByteArray = dropWhile { it == UByte.ZEROu }.takeUnless { it.isEmpty() }?.toUByteArray() ?: ubyteArrayOf(UByte.ZEROu)

fun ByteArray.padStart(size: Int, padByte: Byte = Byte.ZERO): ByteArray =
    takeUnless { this.size < size }
        ?: byteArrayOf(*MutableList(size - this.size) { padByte }.toByteArray(), *this)

fun UByteArray.padStart(size: Int, padByte: UByte = UByte.ZERO): UByteArray =
    takeUnless { this.count() < size }
        ?: ubyteArrayOf(*MutableList(size - this.count()) { padByte }.toUByteArray(), *this)

