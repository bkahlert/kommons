package koodies.math

import java.math.BigInteger as JavaBigInteger
import kotlin.text.toBigInteger as toBigIntegerKotlin
import kotlin.toBigInteger as toBigIntegerKotlin

/**
 * Immutable arbitrary-precision integers.
 */
public actual typealias BigInteger = JavaBigInteger

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
 * Returns the value of this [Byte] number as a [BigInteger].
 */
public actual fun Byte.toBigInteger(): BigInteger = toInt().toBigIntegerKotlin()

/**
 * Returns the value of this [UByte] number as a [BigInteger].
 */
public actual fun UByte.toBigInteger(): BigInteger = toInt().toBigIntegerKotlin()

/**
 * Returns the value of this [Short] number as a [BigInteger].
 */
public actual fun Short.toBigInteger(): BigInteger = toInt().toBigIntegerKotlin()

/**
 * Returns the value of this [UShort] number as a [BigInteger].
 */
public actual fun UShort.toBigInteger(): BigInteger = toInt().toBigIntegerKotlin()

/**
 * Returns the value of this [Int] number as a [BigInteger].
 */
public actual fun Int.toBigInteger(): BigInteger = toBigIntegerKotlin()

/**
 * Returns the value of this [UInt] number as a [BigInteger].
 */
public actual fun UInt.toBigInteger(): BigInteger = toLong().toBigIntegerKotlin()

/**
 * Returns the value of this [Long] number as a [BigInteger].
 */
public actual fun Long.toBigInteger(): BigInteger = toBigIntegerKotlin()

/**
 * Returns the value of this [ULong] number as a [BigInteger].
 */
public actual fun ULong.toBigInteger(): BigInteger = toString().toBigIntegerKotlin()

/**
 * Returns the value of this [BigDecimal] number as a [BigInteger].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun BigDecimal.toBigInteger(): BigInteger = toBigInteger()

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigInteger].
 */
public actual fun CharSequence.toBigInteger(radix: Int): BigInteger = toString().toBigIntegerKotlin(radix)

/**
 * Returns the value of this [BigInteger] as a [ByteArray].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun BigInteger.toString(radix: Int): String = toString(radix)

/**
 * [BigInteger] constants
 */
public actual object BigIntegerConstants {
    /**
     * The BigInteger constant zero.
     */
    public actual val ZERO: BigInteger = JavaBigInteger.ZERO

    /**
     * The BigInteger constant one.
     */
    public actual val ONE: BigInteger = JavaBigInteger.ONE

    /**
     * The BigInteger constant two.
     */
    public actual val TWO: BigInteger = JavaBigInteger.TWO

    /**
     * The BigInteger constant ten.
     */
    public actual val TEN: BigInteger = JavaBigInteger.TEN

    /**
     * The BigDecimal constant ten.
     */
    public actual val HUNDRED: BigInteger = JavaBigInteger.TEN * JavaBigInteger.TEN
}

/**
 * Returns the absolute value of this value.
 */
public actual val BigInteger.absoluteValue: BigInteger get() = abs()

/**
 * Raises this value to the power [n].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
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
