package com.bkahlert.kommons

import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.toBigInteger
import com.bkahlert.kommons.math.toUByteArray

public fun bigIntegerOf(byteArray: ByteArray): BigInteger = bigIntegerOfHexadecimalString(byteArray.toHexadecimalString())
public fun ubyteArrayOfDecimalString(decimalString: String): UByteArray = decimalString.toBigInteger(10).toUByteArray().trim()
public fun bigIntegerOf(ubyteArray: UByteArray): BigInteger = bigIntegerOfHexadecimalString(ubyteArray.toHexadecimalString())

public fun bigIntegerOfBinaryString(binaryString: String): BigInteger = binaryString.toBigInteger(2)
public fun bigIntegerOfDecimalString(decimalString: String): BigInteger = decimalString.toBigInteger(10)
public fun bigIntegerOfHexadecimalString(hexadecimalString: String): BigInteger = hexadecimalString.toBigInteger(16)


public fun ByteArray.trim(): ByteArray = dropWhile { it == Byte.ZERO }.takeUnless { it.isEmpty() }?.toByteArray() ?: byteArrayOf(Byte.ZERO)
public fun UByteArray.trim(): UByteArray = dropWhile { it == UByte.ZERO }.takeUnless { it.isEmpty() }?.toUByteArray() ?: ubyteArrayOf(UByte.ZERO)

public fun ByteArray.padStart(size: Int, padByte: Byte = Byte.ZERO): ByteArray =
    takeUnless { this.size < size }
        ?: byteArrayOf(*MutableList(size - this.size) { padByte }.toByteArray(), *this)

public fun UByteArray.padStart(size: Int, padByte: UByte = UByte.ZERO): UByteArray =
    takeUnless { this.count() < size }
        ?: ubyteArrayOf(*MutableList(size - this.count()) { padByte }.toUByteArray(), *this)
