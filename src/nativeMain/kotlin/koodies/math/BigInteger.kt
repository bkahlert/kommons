@file:Suppress("RedundantVisibilityModifier")

package koodies.math

/**
 * Immutable arbitrary-precision integers.
 */
public actual class BigInteger : Number(), Comparable<BigInteger> {
    override fun compareTo(other: BigInteger): Int = 0
    override fun toByte(): Byte = 0.toByte()
    override fun toChar(): Char = 0.toChar()
    override fun toDouble(): Double = 0.0
    override fun toFloat(): Float = 0F
    override fun toInt(): Int = 0
    override fun toLong(): Long = 0L
    override fun toShort(): Short = 0.toShort()
}

/**
 * Enables the use of the `+` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.plus(other: BigInteger): BigInteger = BigIntegerConstants.ZERO

/**
 * Enables the use of the `-` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.minus(other: BigInteger): BigInteger = BigIntegerConstants.ZERO

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.times(other: Int): BigInteger = BigIntegerConstants.ZERO

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.times(other: BigInteger): BigInteger = BigIntegerConstants.ZERO

/**
 * Enables the use of the unary `-` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.unaryMinus(): BigInteger = BigIntegerConstants.ZERO

/**
 * Enables the use of the `++` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.inc(): BigInteger = BigIntegerConstants.ZERO

/**
 * Enables the use of the `--` operator for [BigInteger] instances.
 */
public actual operator fun BigInteger.dec(): BigInteger = BigIntegerConstants.ZERO

/** Inverts the bits including the sign bit in this value. */
public actual inline val BigInteger.invertedValue: BigInteger get() = BigIntegerConstants.ZERO

/** Performs a bitwise AND operation between the two values. */
public actual infix fun BigInteger.and(other: BigInteger): BigInteger = BigIntegerConstants.ZERO

/** Performs a bitwise OR operation between the two values. */
public actual infix fun BigInteger.or(other: BigInteger): BigInteger = BigIntegerConstants.ZERO

/** Performs a bitwise XOR operation between the two values. */
public actual infix fun BigInteger.xor(other: BigInteger): BigInteger = BigIntegerConstants.ZERO

/** Shifts this value left by the [n] number of bits. */
public actual infix fun BigInteger.shl(n: Int): BigInteger = BigIntegerConstants.ZERO

/** Shifts this value right by the [n] number of bits, filling the leftmost bits with copies of the sign bit. */
public actual infix fun BigInteger.shr(n: Int): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [Byte] number as a [BigInteger].
 */
public actual fun Byte.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [UByte] number as a [BigInteger].
 */
public actual fun UByte.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [Short] number as a [BigInteger].
 */
public actual fun Short.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [UShort] number as a [BigInteger].
 */
public actual fun UShort.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [Int] number as a [BigInteger].
 */
public actual fun Int.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [UInt] number as a [BigInteger].
 */
public actual fun UInt.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [Long] number as a [BigInteger].
 */
public actual fun Long.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [ULong] number as a [BigInteger].
 */
public actual fun ULong.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [BigDecimal] number as a [BigInteger].
 */
public actual fun BigDecimal.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigInteger].
 */
public actual fun CharSequence.toBigInteger(radix: Int): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns the value of this [BigInteger] as a [ByteArray].
 */
public actual fun BigInteger.toByteArray(): ByteArray = ByteArray(0)

/**
 * Returns the value of this [BigInteger] as a [UByteArray].
 */
public actual fun BigInteger.toUByteArray(): UByteArray = UByteArray(0)

/**
 * Creates a [BigInteger] from this [ByteArray].
 */
public actual fun ByteArray.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Creates a [BigInteger] from this [UByteArray].
 */
public actual fun UByteArray.toBigInteger(): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns a string representation of this [BigInteger] value in the specified [radix].
 */
public actual fun BigInteger.toString(radix: Int): String = "0"

/**
 * [BigInteger] constants
 */
public actual object BigIntegerConstants {
    /**
     * The BigInteger constant zero.
     */
    public actual val ZERO: BigInteger = BigInteger()

    /**
     * The BigInteger constant one.
     */
    public actual val ONE: BigInteger = ZERO

    /**
     * The BigInteger constant two.
     */
    public actual val TWO: BigInteger = ZERO

    /**
     * The BigInteger constant ten.
     */
    public actual val TEN: BigInteger = ZERO

    /**
     * The BigDecimal constant ten.
     */
    public actual val HUNDRED: BigInteger = ZERO
}

/**
 * Returns the absolute value of this value.
 */
public actual val BigInteger.absoluteValue: BigInteger
    get() = BigIntegerConstants.ZERO

/**
 * Raises this value to the power [n].
 */
public actual fun BigInteger.pow(n: Int): BigInteger = BigIntegerConstants.ZERO

/**
 * Returns a number having a single bit set in the position of the most significant bit of this [BigInteger] number,
 * or zero, if this number is zero.
 */
public actual fun BigInteger.takeHighestOneBit(): Int = 0

/**
 * Returns a number having a single bit set in the position of the least significant bit of this [BigInteger] number,
 * or zero, if this number is zero.
 */
public actual fun BigInteger.takeLowestOneBit(): Int = 0
