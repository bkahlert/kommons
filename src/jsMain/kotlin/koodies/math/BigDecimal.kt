package koodies.math

/**
 * Immutable, arbitrary-precision signed decimal numbers.
 */
public actual class BigDecimal : Number(), Comparable<BigDecimal> {
    override fun compareTo(other: BigDecimal): Int = 0
    override fun toByte(): Byte = 0.toByte()
    override fun toChar(): Char = 0.toChar()
    override fun toDouble(): Double = 0.0
    override fun toFloat(): Float = 0F
    override fun toInt(): Int = 0
    override fun toLong(): Long = 0L
    override fun toShort(): Short = 0.toShort()
}

/**
 * Enables the use of the `+` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.plus(other: BigDecimal): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns `this` `+` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.plus(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.minus(other: BigDecimal): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns `this` `-` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.minus(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.times(other: BigDecimal): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns `this` `*` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.times(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 *
 * The scale of the result is the same as the scale of `this` (divident), and for rounding the [RoundingMode.HALF_EVEN]
 * rounding mode is used.
 */
public actual operator fun BigDecimal.div(other: BigDecimal): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Long): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(other: Long, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Float): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(other: Float, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Double): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(other: Double, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.rem(other: BigDecimal): BigDecimal = BigDecimalConstants.ZERO

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Int): BigDecimal = BigDecimalConstants.ZERO

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Long): BigDecimal = BigDecimalConstants.ZERO

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Float): BigDecimal = BigDecimalConstants.ZERO

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Double): BigDecimal = BigDecimalConstants.ZERO

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.unaryMinus(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Enables the use of the unary `++` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.inc(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Enables the use of the unary `--` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.dec(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Whether this big decimal represents an integer.
 */
public actual val BigDecimal.isInteger: Boolean get() = false

/**
 * Returns the value of this [Byte] number as a [BigDecimal].
 */
public actual fun Byte.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [UByte] number as a [BigDecimal].
 */
public actual fun UByte.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [Short] number as a [BigDecimal].
 */
public actual fun Short.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [UShort] number as a [BigDecimal].
 */
public actual fun UShort.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [Int] number as a [BigDecimal].
 */
public actual fun Int.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [UInt] number as a [BigDecimal].
 */
public actual fun UInt.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [Long] number as a [BigDecimal].
 */
public actual fun Long.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [ULong] number as a [BigDecimal].
 */
public actual fun ULong.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [Double] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 */
public actual fun Double.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [BigInteger] number as a [BigDecimal].
 */
public actual fun BigInteger.toBigDecimal(): BigDecimal = BigDecimalConstants.ZERO

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigDecimal].
 */
public actual fun CharSequence.toBigDecimal(radix: Int): BigDecimal = BigDecimalConstants.ZERO

/**
 * [BigDecimal] constants
 */
public actual object BigDecimalConstants {

    /**
     * The BigDecimal constant zero.
     */
    public actual val ZERO: BigDecimal = BigDecimal()

    /**
     * The BigDecimal constant one.
     */
    public actual val ONE: BigDecimal = ZERO

    /**
     * The BigDecimal constant two.
     */
    public actual val TWO: BigDecimal = ZERO

    /**
     * The BigDecimal constant ten.
     */
    public actual val TEN: BigDecimal = ZERO

    /**
     * The BigDecimal constant ten.
     */
    public actual val HUNDRED: BigDecimal = ZERO
}

/**
 * Returns the absolute value of this value.
 */
public actual val BigDecimal.absoluteValue: BigDecimal get() = BigDecimalConstants.ZERO

/**
 * Raises this value to the power [n].
 */
public actual fun BigDecimal.pow(n: Int): BigDecimal = BigDecimalConstants.ZERO

/**
 * Raises this value to the power [n] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.pow(n: Int, precision: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

public actual val BigDecimal.scale: Int get() = 0

public actual fun BigDecimal.scale(scale: Int, roundingMode: RoundingMode): BigDecimal = BigDecimalConstants.ZERO

public actual val BigDecimal.precision: Int get() = 0

public actual fun BigDecimal.toScientificString(): String = "0"

public actual fun BigDecimal.toExactDecimalsString(decimals: Int): String = "0"

public actual fun BigDecimal.toAtMostDecimalsString(decimals: Int): String = "0"

public actual fun Double.toScientificString(): String = "0"

public actual fun Double.toExactDecimalsString(decimals: Int): String = "0"

public actual fun Double.toAtMostDecimalsString(decimals: Int): String = "0"
