package com.bkahlert.kommons.math

import com.bkahlert.kommons.math.RoundingMode.HALF_DOWN
import com.bkahlert.kommons.math.RoundingMode.HALF_EVEN

/**
 * Immutable, arbitrary-precision signed decimal numbers.
 */
public expect class BigDecimal : Number, Comparable<BigDecimal>

/**
 * Adds the `+` operator for [BigDecimal] instances.
 */
public expect operator fun BigDecimal.plus(other: BigDecimal): BigDecimal

/**
 * Returns this `+` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.plus(other: BigDecimal, precision: Int, roundingMode: RoundingMode = HALF_DOWN): BigDecimal

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
public expect operator fun BigDecimal.minus(other: BigDecimal): BigDecimal

/**
 * Returns this `-` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.minus(other: BigDecimal, precision: Int, roundingMode: RoundingMode = HALF_DOWN): BigDecimal

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
public expect operator fun BigDecimal.times(other: BigDecimal): BigDecimal

/**
 * Returns this `*` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.times(other: BigDecimal, precision: Int, roundingMode: RoundingMode = HALF_DOWN): BigDecimal

/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 *
 * The scale of the result is the same as the scale of this (divident), and for rounding the [RoundingMode.HALF_EVEN]
 * rounding mode is used.
 */
public expect operator fun BigDecimal.div(other: BigDecimal): BigDecimal

/**
 * Returns this `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.div(other: BigDecimal, precision: Int, roundingMode: RoundingMode = HALF_DOWN): BigDecimal

/** Divides this value by the other value. */
public expect operator fun BigDecimal.div(other: Long): BigDecimal

/**
 * Returns this `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.div(other: Long, precision: Int, roundingMode: RoundingMode = HALF_DOWN): BigDecimal

/** Divides this value by the other value. */
public expect operator fun BigDecimal.div(other: Float): BigDecimal

/**
 * Returns this `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.div(other: Float, precision: Int, roundingMode: RoundingMode = HALF_DOWN): BigDecimal

/** Divides this value by the other value. */
public expect operator fun BigDecimal.div(other: Double): BigDecimal

/**
 * Returns this `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.div(other: Double, precision: Int, roundingMode: RoundingMode = HALF_DOWN): BigDecimal

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
public expect operator fun BigDecimal.rem(other: BigDecimal): BigDecimal

/** Calculates the remainder of dividing this value by the other value. */
public expect operator fun BigDecimal.rem(other: Int): BigDecimal

/** Calculates the remainder of dividing this value by the other value. */
public expect operator fun BigDecimal.rem(other: Long): BigDecimal

/** Calculates the remainder of dividing this value by the other value. */
public expect operator fun BigDecimal.rem(other: Float): BigDecimal

/** Calculates the remainder of dividing this value by the other value. */
public expect operator fun BigDecimal.rem(other: Double): BigDecimal

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
public expect operator fun BigDecimal.unaryMinus(): BigDecimal

/**
 * Enables the use of the unary `++` operator for [BigDecimal] instances.
 */
public expect operator fun BigDecimal.inc(): BigDecimal

/**
 * Enables the use of the unary `--` operator for [BigDecimal] instances.
 */
public expect operator fun BigDecimal.dec(): BigDecimal

/**
 * Whether this big decimal represents an integer.
 */
public expect val BigDecimal.isInteger: Boolean

/**
 * Returns the value of this [Byte] number as a [BigDecimal].
 */
public expect fun Byte.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [UByte] number as a [BigDecimal].
 */
public expect fun UByte.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [Short] number as a [BigDecimal].
 */
public expect fun Short.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [UShort] number as a [BigDecimal].
 */
public expect fun UShort.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [Int] number as a [BigDecimal].
 */
public expect fun Int.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [UInt] number as a [BigDecimal].
 */
public expect fun UInt.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [Long] number as a [BigDecimal].
 */
public expect fun Long.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [ULong] number as a [BigDecimal].
 */
public expect fun ULong.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [Double] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 */
public expect fun Double.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [BigInteger] number as a [BigDecimal].
 */
public expect fun BigInteger.toBigDecimal(): BigDecimal

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigDecimal].
 */
public expect fun CharSequence.toBigDecimal(radix: Int = 10): BigDecimal

/**
 * [BigDecimal] constants
 */
public expect object BigDecimalConstants {
    /**
     * The BigDecimal constant zero.
     */
    public val ZERO: BigDecimal

    /**
     * The BigDecimal constant one.
     */
    public val ONE: BigDecimal

    /**
     * The BigDecimal constant two.
     */
    public val TWO: BigDecimal

    /**
     * The BigDecimal constant ten.
     */
    public val TEN: BigDecimal

    /**
     * The BigDecimal constant ten.
     */
    public val HUNDRED: BigDecimal
}

/**
 * Returns the absolute value of this value.
 */
public expect val BigDecimal.absoluteValue: BigDecimal

/**
 * Raises this value to the power [n].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect fun BigDecimal.pow(n: Int): BigDecimal

/**
 * Raises this value to the power [n] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public expect fun BigDecimal.pow(n: Int, precision: Int, roundingMode: RoundingMode = HALF_EVEN): BigDecimal

public expect val BigDecimal.scale: Int

public expect fun BigDecimal.scale(scale: Int, roundingMode: RoundingMode = HALF_EVEN): BigDecimal

public expect val BigDecimal.precision: Int


public expect fun Double.toScientificString(): String

public expect fun Double.toExactDecimalsString(decimals: Int): String

public expect fun Double.toAtMostDecimalsString(decimals: Int): String


public expect fun BigDecimal.toScientificString(): String

public expect fun BigDecimal.toExactDecimalsString(decimals: Int): String

public expect fun BigDecimal.toAtMostDecimalsString(decimals: Int): String
