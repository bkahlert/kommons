package koodies.math

import kotlin.text.toBigInteger as toBigIntegerKotlin
import kotlin.toBigInteger as toBigIntegerKotlin

/**
 * Immutable arbitrary-precision integers.
 */
public actual typealias BigInteger = java.math.BigInteger

/**
 * Enables the use of the `+` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.plus(other: BigInteger): BigInteger = this.add(other)

/**
 * Enables the use of the `-` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.minus(other: BigInteger): BigInteger = this.subtract(other)

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.times(other: Int): BigInteger = this.multiply(other.toBigInteger())

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.times(other: BigInteger): BigInteger = this.multiply(other)

/**
 * Enables the use of the unary `-` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.unaryMinus(): BigInteger = this.negate()

/**
 * Enables the use of the `++` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.inc(): BigInteger = this.add(BigInteger.ONE)

/**
 * Enables the use of the `--` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.dec(): BigInteger = this.subtract(BigInteger.ONE)

/** Inverts the bits including the sign bit in this value. */
public actual inline val BigInteger.invertedValue: BigInteger
    get() = this.not()

/** Performs a bitwise AND operation between the two values. */
public actual infix fun BigInteger.and(other: BigInteger): BigInteger = this.and(other)

/** Performs a bitwise OR operation between the two values. */
public actual infix fun BigInteger.or(other: BigInteger): BigInteger = this.or(other)

/** Performs a bitwise XOR operation between the two values. */
public actual infix fun BigInteger.xor(other: BigInteger): BigInteger = this.xor(other)

/** Shifts this value left by the [n] number of bits. */
public actual infix fun BigInteger.shl(n: Int): BigInteger = this.shiftLeft(n)

/** Shifts this value right by the [n] number of bits, filling the leftmost bits with copies of the sign bit. */
public actual infix fun BigInteger.shr(n: Int): BigInteger = this.shiftRight(n)

/**
 * Returns the value of this [Int] number as a [BigInteger].
 */
public actual fun Int.toBigInteger(): BigInteger = toBigIntegerKotlin()

/**
 * Returns the value of this [UInt] number as a [BigInteger].
 */
public actual fun UInt.toBigInteger(): BigInteger = toLong().toBigIntegerKotlin()

/**
 * Returns the value of this [BigDecimal] number as a [BigInteger].
 */
public actual fun BigDecimal.toBigInteger(): BigInteger = toBigInteger()

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigInteger].
 */
public actual fun CharSequence.toBigInteger(radix: Int): BigInteger = toString().toBigIntegerKotlin(radix)

/**
 * Returns the value of this [BigInteger] as a [ByteArray].
 */
public actual fun BigInteger.toByteArray(): ByteArray = toByteArray()

/**
 * Returns the value of this [BigInteger] as a [UByteArray].
 */
public actual fun BigInteger.toUByteArray(): UByteArray = toByteArray().toUByteArray()

/**
 * Creates a [BigInteger] from this [ByteArray].
 */
public actual fun ByteArray.toBigInteger(): BigInteger = BigInteger(this)

/**
 * Creates a [BigInteger] from this [UByteArray].
 */
public actual fun UByteArray.toBigInteger(): BigInteger = BigInteger(toByteArray())

/**
 * Returns a string representation of this [BigInteger] value in the specified [radix].
 */
public actual fun BigInteger.toString(radix: Int): String = toString(radix)

public actual object BigIntegerConstants {
    /**
     * The BigInteger constant zero.
     */
    public actual val ZERO: BigInteger = java.math.BigInteger.ZERO

    /**
     * The BigInteger constant one.
     */
    public actual val ONE: BigInteger = java.math.BigInteger.ONE

    /**
     * The BigInteger constant two.
     */
    public actual val TWO: BigInteger = java.math.BigInteger.TWO

    /**
     * The BigInteger constant ten.
     */
    public actual val TEN: BigInteger = java.math.BigInteger.TEN

    /**
     * The BigDecimal constant ten.
     */
    public actual val HUNDRED: BigInteger = java.math.BigInteger.TEN * java.math.BigInteger.TEN
}

/**
 * Returns the absolute value of this value.
 */
public actual val BigInteger.absoluteValue: BigInteger get() = abs()

/**
 * Raises this value to the power [n].
 */
public actual fun BigInteger.pow(n: Int): BigInteger = pow(n)

/**
 * Returns a number having a single bit set in the position of the most significant set bit of this [BigInteger] number,
 * or zero, if this number is zero.
 */
public actual fun BigInteger.takeHighestOneBit(): Int = bitLength()

/**
 * Returns a number having a single bit set in the position of the least significant set bit of this [BigInteger] number,
 * or zero, if this number is zero.
 */
public actual fun BigInteger.takeLowestOneBit(): Int = lowestSetBit
