package koodies.math

/**
 * Immutable arbitrary-precision integers.
 */
public expect class BigInteger : Number, Comparable<BigInteger>

/**
 * Enables the use of the `+` operator for [BigInteger] instances.
 */
public expect operator fun BigInteger.plus(other: BigInteger): BigInteger

/**
 * Enables the use of the `-` operator for [BigInteger] instances.
 */
public expect operator fun BigInteger.minus(other: BigInteger): BigInteger

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
public expect operator fun BigInteger.times(other: Int): BigInteger

/**
 * Enables the use of the `*` operator for [BigInteger] instances.
 */
public expect operator fun BigInteger.times(other: BigInteger): BigInteger

/**
 * Enables the use of the unary `-` operator for [BigInteger] instances.
 */
public expect operator fun BigInteger.unaryMinus(): BigInteger

/**
 * Enables the use of the `++` operator for [BigInteger] instances.
 */
public expect operator fun BigInteger.inc(): BigInteger

/**
 * Enables the use of the `--` operator for [BigInteger] instances.
 */
public expect operator fun BigInteger.dec(): BigInteger

/** Inverts the bits including the sign bit in this value. */
public expect val BigInteger.invertedValue: BigInteger

/** Performs a bitwise AND operation between the two values. */
public expect infix fun BigInteger.and(other: BigInteger): BigInteger

/** Performs a bitwise OR operation between the two values. */
public expect infix fun BigInteger.or(other: BigInteger): BigInteger

/** Performs a bitwise XOR operation between the two values. */
public expect infix fun BigInteger.xor(other: BigInteger): BigInteger

/** Shifts this value left by the [n] number of bits. */
public expect infix fun BigInteger.shl(n: Int): BigInteger

/** Shifts this value right by the [n] number of bits, filling the leftmost bits with copies of the sign bit. */
public expect infix fun BigInteger.shr(n: Int): BigInteger

/**
 * Returns the value of this [Byte] number as a [BigInteger].
 */
public expect fun Byte.toBigInteger(): BigInteger

/**
 * Returns the value of this [UByte] number as a [BigInteger].
 */
public expect fun UByte.toBigInteger(): BigInteger

/**
 * Returns the value of this [Short] number as a [BigInteger].
 */
public expect fun Short.toBigInteger(): BigInteger

/**
 * Returns the value of this [UShort] number as a [BigInteger].
 */
public expect fun UShort.toBigInteger(): BigInteger

/**
 * Returns the value of this [Int] number as a [BigInteger].
 */
public expect fun Int.toBigInteger(): BigInteger

/**
 * Returns the value of this [UInt] number as a [BigInteger].
 */
public expect fun UInt.toBigInteger(): BigInteger

/**
 * Returns the value of this [Long] number as a [BigInteger].
 */
public expect fun Long.toBigInteger(): BigInteger

/**
 * Returns the value of this [ULong] number as a [BigInteger].
 */
public expect fun ULong.toBigInteger(): BigInteger

/**
 * Returns the value of this [BigDecimal] number as a [BigInteger].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect fun BigDecimal.toBigInteger(): BigInteger

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigInteger].
 */
public expect fun CharSequence.toBigInteger(radix: Int = 10): BigInteger

/**
 * Returns the value of this [BigInteger] as a [ByteArray].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect fun BigInteger.toByteArray(): ByteArray

/**
 * Returns the value of this [BigInteger] as a [UByteArray].
 */
public expect fun BigInteger.toUByteArray(): UByteArray

/**
 * Creates a [BigInteger] from this [ByteArray].
 */
public expect fun ByteArray.toBigInteger(): BigInteger

/**
 * Creates a [BigInteger] from this [UByteArray].
 */
public expect fun UByteArray.toBigInteger(): BigInteger

/**
 * Returns a string representation of this [BigInteger] value in the specified [radix].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect fun BigInteger.toString(radix: Int = 10): String

/**
 * [BigInteger] constants
 */
public expect object BigIntegerConstants {
    /**
     * The BigInteger constant zero.
     */
    public val ZERO: BigInteger

    /**
     * The BigInteger constant one.
     */
    public val ONE: BigInteger

    /**
     * The BigInteger constant two.
     */
    public val TWO: BigInteger

    /**
     * The BigInteger constant ten.
     */
    public val TEN: BigInteger

    /**
     * The BigDecimal constant ten.
     */
    public val HUNDRED: BigInteger
}

/**
 * Returns the absolute value of this value.
 */
public expect val BigInteger.absoluteValue: BigInteger

/**
 * Raises this value to the power [n].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect fun BigInteger.pow(n: Int): BigInteger

/**
 * Returns a number having a single bit set in the position of the most significant bit of this [BigInteger] number,
 * or zero, if this number is zero.
 */
public expect fun BigInteger.takeHighestOneBit(): Int

/**
 * Returns a number having a single bit set in the position of the least significant bit of this [BigInteger] number,
 * or zero, if this number is zero.
 */
public expect fun BigInteger.takeLowestOneBit(): Int
