package koodies.math

import koodies.math.RoundingMode.CEILING
import koodies.math.RoundingMode.DOWN
import koodies.math.RoundingMode.FLOOR
import koodies.math.RoundingMode.HALF_DOWN
import koodies.math.RoundingMode.HALF_EVEN
import koodies.math.RoundingMode.HALF_UP
import koodies.math.RoundingMode.UNNECESSARY
import koodies.math.RoundingMode.UP
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.concurrent.getOrSet
import java.math.BigDecimal as JavaBigDecimal
import kotlin.text.toBigDecimal as toBigDecimalKotlin
import kotlin.toBigDecimal as toBigDecimalKotlin

/**
 * Immutable, arbitrary-precision signed decimal numbers.
 */
public actual typealias BigDecimal = JavaBigDecimal

/**
 * Returns  the use of the `+` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.plus(other: BigDecimal): BigDecimal = this.add(other)

/**
 * Returns `this` `+` [other] with the provided [precision] and [roundingMode].
 */
public actual fun BigDecimal.plus(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.add(other, MathContext(precision, roundingMode.toJavaMathRoundMode()))

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.minus(other: BigDecimal): BigDecimal = this.subtract(other)

/**
 * Returns `this` `-` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.minus(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.subtract(other, MathContext(precision, roundingMode.toJavaMathRoundMode()))

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.times(other: BigDecimal): BigDecimal = multiply(other)

/**
 * Returns `this` `*` [other] with the provided [precision] and [roundingMode].
 */
public actual fun BigDecimal.times(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.multiply(other, MathContext(precision, roundingMode.toJavaMathRoundMode()))

/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 *
 * The scale of the result is the same as the scale of `this` (dividend), and for rounding the [RoundingMode.HALF_EVEN]
 * rounding mode is used.
 */
public actual operator fun BigDecimal.div(other: BigDecimal): BigDecimal = divide(other, java.math.RoundingMode.HALF_EVEN)

/**
 * Returns `this` `*` [other] with the provided [precision] and [roundingMode].
 */
public actual fun BigDecimal.div(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.divide(other, MathContext(precision, roundingMode.toJavaMathRoundMode()))

/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Long): BigDecimal = this.div(other.toBigDecimalKotlin())

/**
 * Returns `this` `/` [other] with the provided [precision] and [roundingMode].
 */
public actual fun BigDecimal.div(other: Long, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.divide(other.toBigDecimalKotlin(), MathContext(precision, roundingMode.toJavaMathRoundMode()))

/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Float): BigDecimal = this.div(other.toBigDecimalKotlin())

/**
 * Returns `this` `/` [other] with the provided [precision] and [roundingMode].
 */
public actual fun BigDecimal.div(other: Float, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.divide(other.toBigDecimalKotlin(), MathContext(precision, roundingMode.toJavaMathRoundMode()))

/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Double): BigDecimal = this.div(other.toBigDecimalKotlin())

/**
 * Returns `this` `/` [other] with the provided [precision] and [roundingMode].
 */
public actual fun BigDecimal.div(other: Double, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.divide(other.toBigDecimalKotlin(), MathContext(precision, roundingMode.toJavaMathRoundMode()))

/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.rem(other: BigDecimal): BigDecimal = this.remainder(other)

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Int): BigDecimal = this.rem(other.toBigDecimalKotlin())

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Long): BigDecimal = this.rem(other.toBigDecimalKotlin())

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Float): BigDecimal = this.rem(other.toBigDecimalKotlin())

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Double): BigDecimal = this.rem(other.toBigDecimalKotlin())

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.unaryMinus(): BigDecimal = this.negate()

/**
 * Enables the use of the unary `++` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.inc(): BigDecimal = this.add(BigDecimal.ONE)

/**
 * Enables the use of the unary `--` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.dec(): BigDecimal = this.subtract(BigDecimal.ONE)

/**
 * Whether this big decimal represents an integer.
 */
public actual val BigDecimal.isInteger: Boolean get() = stripTrailingZeros().scale() <= 0

/**
 * Returns the value of this [Byte] number as a [BigDecimal].
 */
public actual fun Byte.toBigDecimal(): BigDecimal = toInt().toBigDecimalKotlin()

/**
 * Returns the value of this [UByte] number as a [BigDecimal].
 */
public actual fun UByte.toBigDecimal(): BigDecimal = toInt().toBigDecimalKotlin()

/**
 * Returns the value of this [Short] number as a [BigDecimal].
 */
public actual fun Short.toBigDecimal(): BigDecimal = toInt().toBigDecimalKotlin()

/**
 * Returns the value of this [UShort] number as a [BigDecimal].
 */
public actual fun UShort.toBigDecimal(): BigDecimal = toInt().toBigDecimalKotlin()

/**
 * Returns the value of this [Int] number as a [BigDecimal].
 */
public actual fun Int.toBigDecimal(): BigDecimal = toBigDecimalKotlin()

/**
 * Returns the value of this [UInt] number as a [BigDecimal].
 */
public actual fun UInt.toBigDecimal(): BigDecimal = toLong().toBigDecimalKotlin()

/**
 * Returns the value of this [Long] number as a [BigDecimal].
 */
public actual fun Long.toBigDecimal(): BigDecimal = toBigDecimalKotlin()

/**
 * Returns the value of this [ULong] number as a [BigDecimal].
 */
public actual fun ULong.toBigDecimal(): BigDecimal = toString().toBigDecimalKotlin()

/**
 * Returns the value of this [Double] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 */
public actual fun Double.toBigDecimal(): BigDecimal = toBigDecimalKotlin()

/**
 * Returns the value of this [BigInteger] number as a [BigDecimal].
 */
public actual fun BigInteger.toBigDecimal(): BigDecimal = toBigDecimal()

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigDecimal].
 */
public actual fun CharSequence.toBigDecimal(radix: Int): BigDecimal = toString().toBigDecimalKotlin()

/**
 * [BigDecimal] constants
 */
public actual object BigDecimalConstants {

    /**
     * The BigDecimal constant zero.
     */
    public actual val ZERO: BigDecimal = JavaBigDecimal.ZERO

    /**
     * The BigDecimal constant one.
     */
    public actual val ONE: BigDecimal = JavaBigDecimal.ONE

    /**
     * The BigDecimal constant two.
     */
    public actual val TWO: BigDecimal = ONE + ONE

    /**
     * The BigDecimal constant ten.
     */
    public actual val TEN: BigDecimal = JavaBigDecimal.TEN

    /**
     * The BigDecimal constant ten.
     */
    public actual val HUNDRED: BigDecimal = JavaBigDecimal.TEN.let { it * it }
}

/**
 * Returns the absolute value of this value.
 */
public actual val BigDecimal.absoluteValue: BigDecimal get() = this.abs()

/**
 * Raises this value to the power [n].
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual fun BigDecimal.pow(n: Int): BigDecimal = this.pow(n)

/**
 * Raises this value to the power [n] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.pow(n: Int, precision: Int, roundingMode: RoundingMode): BigDecimal =
    this.pow(n, MathContext(precision, roundingMode.toJavaMathRoundMode()))

public actual val BigDecimal.scale: Int
    get() = this.scale()

public actual fun BigDecimal.scale(scale: Int, roundingMode: RoundingMode): BigDecimal =
    setScale(scale, roundingMode.toJavaMathRoundMode())

public actual val BigDecimal.precision: Int
    get() = precision()

private fun RoundingMode.toJavaMathRoundMode() = when (this) {
    UP -> java.math.RoundingMode.UP
    DOWN -> java.math.RoundingMode.DOWN
    CEILING -> java.math.RoundingMode.CEILING
    FLOOR -> java.math.RoundingMode.FLOOR
    HALF_UP -> java.math.RoundingMode.HALF_UP
    HALF_DOWN -> java.math.RoundingMode.HALF_DOWN
    HALF_EVEN -> java.math.RoundingMode.HALF_EVEN
    UNNECESSARY -> java.math.RoundingMode.UNNECESSARY
}

private val Double.scientificFormat: String get() = privateFormatScientific(this)
private val BigDecimal.scientificFormat: String get() = privateFormatScientific(this.toDouble())

private val rootNegativeExpFormatSymbols = DecimalFormatSymbols(Locale.ROOT).apply { exponentSeparator = "e" }
private val rootPositiveExpFormatSymbols = DecimalFormatSymbols(Locale.ROOT).apply { exponentSeparator = "e+" }

private val scientificFormat = ThreadLocal<DecimalFormat>()
private fun privateFormatScientific(value: Double): String =
    scientificFormat.getOrSet {
        DecimalFormat("0E0", rootNegativeExpFormatSymbols).apply { minimumFractionDigits = 2 }
    }.apply {
        decimalFormatSymbols = if (value >= 1 || value <= -1) rootPositiveExpFormatSymbols else rootNegativeExpFormatSymbols
    }.format(value)


private val precisionFormats = Array(4) { ThreadLocal<DecimalFormat>() }
private fun createFormatForDecimals(decimals: Int) = DecimalFormat("0", rootNegativeExpFormatSymbols).apply {
    if (decimals > 0) minimumFractionDigits = decimals
    roundingMode = java.math.RoundingMode.HALF_UP
}

private fun privateFormatToExactDecimals(value: Double, decimals: Int): String {
    val format = if (decimals < precisionFormats.size) {
        precisionFormats[decimals].getOrSet { createFormatForDecimals(decimals) }
    } else
        createFormatForDecimals(decimals)
    return format.format(value)
}

private fun privateFormatUpToDecimals(value: Double, decimals: Int): String =
    createFormatForDecimals(0)
        .apply { maximumFractionDigits = decimals }
        .format(value)

public actual fun Double.toScientificString(): String = scientificFormat

public actual fun Double.toExactDecimalsString(decimals: Int): String = privateFormatToExactDecimals(this, decimals)

public actual fun Double.toAtMostDecimalsString(decimals: Int): String = privateFormatUpToDecimals(this, decimals)

public actual fun BigDecimal.toScientificString(): String = scientificFormat

public actual fun BigDecimal.toExactDecimalsString(decimals: Int): String = privateFormatToExactDecimals(this.toDouble(), decimals)

public actual fun BigDecimal.toAtMostDecimalsString(decimals: Int): String = privateFormatUpToDecimals(this.toDouble(), decimals)
