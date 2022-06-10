package com.bkahlert.kommons

import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.toBigInteger
import com.bkahlert.kommons.math.toUByteArray

private val hexChars: Array<Char> = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

public fun bigIntegerOf(byteArray: ByteArray): BigInteger = bigIntegerOfHexadecimalString(byteArray.toHexadecimalString())

public fun ubyteArrayOfDecimalString(decimalString: String): UByteArray = decimalString.toBigInteger(10).toUByteArray().trim()

public fun bigIntegerOf(ubyteArray: UByteArray): BigInteger = bigIntegerOfHexadecimalString(ubyteArray.toHexadecimalString())

public fun bigIntegerOfBinaryString(binaryString: String): BigInteger = binaryString.toBigInteger(2)
public fun bigIntegerOfDecimalString(decimalString: String): BigInteger = decimalString.toBigInteger(10)
public fun bigIntegerOfHexadecimalString(hexadecimalString: String): BigInteger = hexadecimalString.toBigInteger(16)

public fun Int.toHexadecimalString(): String {
    var rem: Int
    var decimal = this
    val hex = StringBuilder()
    if (decimal == 0) return "00"
    while (decimal > 0) {
        rem = decimal % 16
        hex.append(hexChars[rem])
        decimal /= 16
    }
    return hex.reversed().toString().let {
        if (it.length.mod(2) == 1) "0$it" else it
    }
}

public fun UInt.toUBytes(trim: Boolean = true): UByteArray =
    0.until(Int.SIZE_BYTES)
        .map { i -> shr((UInt.SIZE_BYTES - i - 1) * UByte.SIZE_BITS).toUByte() }
        .let { if (trim) it.dropWhile { byte -> byte == 0.toUByte() } else it }
        .toUByteArray()


public fun ByteArray.trim(): ByteArray = dropWhile { it == Byte.ZERO }.takeUnless { it.isEmpty() }?.toByteArray() ?: byteArrayOf(Byte.ZERO)
public fun UByteArray.trim(): UByteArray = dropWhile { it == UByte.ZERO }.takeUnless { it.isEmpty() }?.toUByteArray() ?: ubyteArrayOf(UByte.ZERO)

public fun ByteArray.padStart(size: Int, padByte: Byte = Byte.ZERO): ByteArray =
    takeUnless { this.size < size }
        ?: byteArrayOf(*MutableList(size - this.size) { padByte }.toByteArray(), *this)

public fun UByteArray.padStart(size: Int, padByte: UByte = UByte.ZERO): UByteArray =
    takeUnless { this.count() < size }
        ?: ubyteArrayOf(*MutableList(size - this.count()) { padByte }.toUByteArray(), *this)
