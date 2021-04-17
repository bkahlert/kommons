package koodies.math

import koodies.text.takeUnlessEmpty

private val hexChars: Array<Char> = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
private fun String.format(length: Int, pad: Boolean = false): String =
    if (pad) padStart(length, '0') else dropWhile { it == '0' }.takeUnlessEmpty() ?: "0"

public val Byte.Companion.ZERO: Byte get() = 0x0
public val UByte.Companion.ZEROu: UByte get() = 0x0u
public inline val Byte.Companion.OO: Byte get() = ZERO
public inline val Byte.Companion.FF: Byte get() = -1
public inline val UByte.Companion.OOu: UByte get() = MIN_VALUE
public inline val UByte.Companion.FFu: UByte get() = MAX_VALUE

public fun Byte.toPositiveInt(): Int = toInt() and 0xFF

public fun Byte.toBinaryString(pad: Boolean = true): String = toPositiveInt().toString(2).format(Byte.SIZE_BITS, pad)
public fun Byte.toDecimalString(): String = toPositiveInt().toString(10)
public fun Byte.toHexadecimalString(pad: Boolean = true): String = toPositiveInt().toString(16).format(2, pad)
public fun ByteArray.toBinaryString(pad: Boolean = true): String = joinToString("") { it.toBinaryString(true) }.format(size * 2, pad)
public fun ByteArray.toDecimalString(): String = bigIntegerOf(this).toString(10)
public fun ByteArray.toHexadecimalString(pad: Boolean = true): String = joinToString("") { it.toHexadecimalString(true) }.format(size * 2, pad)
public fun byteArrayOfBinaryString(binaryString: String): ByteArray = binaryString.toBigInteger(2).toByteArray().trim()
public fun byteArrayOfDecimalString(decimalString: String): ByteArray = decimalString.toBigInteger(10).toByteArray().trim()
public fun byteArrayOfHexadecimalString(hexadecimalString: String): ByteArray = hexadecimalString.toBigInteger(16).toByteArray().trim()
public fun bigIntegerOf(byteArray: ByteArray): BigInteger = bigIntegerOfHexadecimalString(byteArray.toHexadecimalString())

public fun UByte.toBinaryString(pad: Boolean = true): String = toInt().toString(2).format(Byte.SIZE_BITS, pad)
public fun UByte.toDecimalString(): String = toInt().toString(10)
public fun UByte.toHexadecimalString(pad: Boolean = true): String = toInt().toString(16).format(2, pad)
public fun UByteArray.toBinaryString(pad: Boolean = true): String = joinToString("") { it.toBinaryString(true) }.format(count() * 2, pad)
public fun UByteArray.toDecimalString(): String = bigIntegerOf(this).toString(10)
public fun UByteArray.toHexadecimalString(pad: Boolean = true): String = joinToString("") { it.toHexadecimalString(true) }.format(size * 2, pad)
public fun ubyteArrayOfBinaryString(binaryString: String): UByteArray = binaryString.toBigInteger(2).toUByteArray().trim()
public fun ubyteArrayOfDecimalString(decimalString: String): UByteArray = decimalString.toBigInteger(10).toUByteArray().trim()
public fun ubyteArrayOfHexadecimalString(hexadecimalString: String): UByteArray = hexadecimalString.toBigInteger(16).toUByteArray().trim()
public fun bigIntegerOf(ubyteArray: UByteArray): BigInteger = bigIntegerOfHexadecimalString(ubyteArray.toHexadecimalString())

public fun bigIntegerOfBinaryString(binaryString: String): BigInteger = binaryString.toBigInteger(2)
public fun bigIntegerOfDecimalString(decimalString: String): BigInteger = decimalString.toBigInteger(10)
public fun bigIntegerOfHexadecimalString(hexadecimalString: String): BigInteger = hexadecimalString.toBigInteger(16)

public fun Int.toHexadecimalString(pad: Boolean = true): String {
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


public val UByte.Companion.ZERO: UByte get() = 0x0u
public inline val UByte.Companion.OO: UByte get() = ZERO

public fun Int.toBytes(trim: Boolean = true): ByteArray =
    0.until(Int.SIZE_BYTES)
        .map { i -> (shr((Int.SIZE_BYTES - i - 1) * Byte.SIZE_BITS) and 0xFF).toByte() }
        .let { if (trim) it.dropWhile { byte -> byte == 0.toByte() } else it }
        .toByteArray()

public fun UInt.toUBytes(trim: Boolean = true): UByteArray =
    0.until(Int.SIZE_BYTES)
        .map { i -> shr((UInt.SIZE_BYTES - i - 1) * UByte.SIZE_BITS).toUByte() }
        .let { if (trim) it.dropWhile { byte -> byte == 0.toUByte() } else it }
        .toUByteArray()

public fun Iterable<Byte>.toInt(): Int = toList().toByteArray().toInt()
public fun Iterable<UByte>.toUInt(): UInt = toList().toUByteArray().toUInt()

public fun ByteArray.toInt(): Int {
    require(size <= Int.SIZE_BYTES) { "The byte array must not consist of more than ${Int.SIZE_BYTES} bytes." }
    return fold(0) { value, byte -> value.shl(Byte.SIZE_BITS) + byte.toPositiveInt() }
}

public fun ByteArray.toUInt(): UInt = toInt().toUInt()

public fun UByteArray.toUInt(): UInt {
    require(count() <= UInt.SIZE_BYTES) { "The byte array must not consist of more than ${UInt.SIZE_BYTES} bytes." }
    return fold(0u) { value, byte -> value.shl(UByte.SIZE_BITS) + byte }
}

public fun UByteArray.toInt(): Int = toUInt().toInt()


public fun ByteArray.trim(): ByteArray = dropWhile { it == Byte.ZERO }.takeUnless { it.isEmpty() }?.toByteArray() ?: byteArrayOf(Byte.ZERO)
public fun UByteArray.trim(): UByteArray = dropWhile { it == UByte.ZEROu }.takeUnless { it.isEmpty() }?.toUByteArray() ?: ubyteArrayOf(UByte.ZEROu)

public fun ByteArray.padStart(size: Int, padByte: Byte = Byte.ZERO): ByteArray =
    takeUnless { this.size < size }
        ?: byteArrayOf(*MutableList(size - this.size) { padByte }.toByteArray(), *this)

// TODO use default parameter as soon as JS no more has a problem with it (default ubyte)
public fun UByteArray.padStart(size: Int): UByteArray = padStart(size, UByte.ZERO)

public fun UByteArray.padStart(size: Int, padByte: UByte): UByteArray =
    takeUnless { this.count() < size }
        ?: ubyteArrayOf(*MutableList(size - this.count()) { padByte }.toUByteArray(), *this)


