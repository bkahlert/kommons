package koodies.unit

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.number.formatScientifically
import koodies.number.formatToExactDecimals
import koodies.number.toBigDecimal
import koodies.text.CharRanges
import koodies.text.quoted
import kotlin.reflect.KClass

/**
 * Amount of bytes representable in with the decimal [SI prefixes](https://en.wikipedia.org/wiki/Metric_prefix)
 * [Yotta], [Zetta], [Exa], [Peta], [Tera], [Giga], [Mega], [kilo] as well as with the binary prefixes as defined by
 * [ISO/IEC 80000](https://en.wikipedia.org/wiki/ISO/IEC_80000) [Yobi], [Zebi], [Exbi], [Pebi], [Tebi], [Gibi], [Mebi] and [Kibi].
 */
public inline class Size(public val bytes: BigDecimal) : Comparable<Size> {

    public val bits: BigInteger get() = bytes.toBigInteger() * Byte.SIZE_BITS

    /**
     * Computes the amount of characters needed at most to represent
     * this amount of [bytes] to the specified [base].
     *
     * E.g. `2.bytes.maxLengthOfRepresentationToBaseOf(8) == 4`
     * which is the max length of two bytes represented in octal notation.
     */
    public fun maxLengthOfRepresentationToBaseOf(base: Int): Int = (BigInteger.TWO shl bits.dec().intValue()).dec().toString(base).length

    public companion object {
        public val ZERO: Size = Size(BigDecimal.ZERO)
        public val supportedPrefixes: Map<KClass<out UnitPrefix>, List<UnitPrefix?>> = mapOf(
            BinaryPrefix::class to listOf(
                BinaryPrefix.Yobi,
                BinaryPrefix.Zebi,
                BinaryPrefix.Exbi,
                BinaryPrefix.Pebi,
                BinaryPrefix.Tebi,
                BinaryPrefix.Gibi,
                BinaryPrefix.Mebi,
                BinaryPrefix.Kibi,
                null,
                BinaryPrefix.mibi,
                BinaryPrefix.mubi,
                BinaryPrefix.nabi,
                BinaryPrefix.pibi,
                BinaryPrefix.fembi,
                BinaryPrefix.abi,
                BinaryPrefix.zebi,
                BinaryPrefix.yobi,
            ),
            DecimalPrefix::class to listOf(
                DecimalPrefix.Yotta,
                DecimalPrefix.Zetta,
                DecimalPrefix.Exa,
                DecimalPrefix.Peta,
                DecimalPrefix.Tera,
                DecimalPrefix.Giga,
                DecimalPrefix.Mega,
                DecimalPrefix.kilo,
//                DecimalPrefix.hecto,
//                DecimalPrefix.deca,
                null,
//                DecimalPrefix.deci,
//                DecimalPrefix.centi,
                DecimalPrefix.milli,
                DecimalPrefix.micro,
                DecimalPrefix.nano,
                DecimalPrefix.pico,
                DecimalPrefix.femto,
                DecimalPrefix.atto,
                DecimalPrefix.zepto,
                DecimalPrefix.yocto,
            )
        )
        public const val SYMBOL: String = "B"

        public fun precision(value: BigDecimal, unit: UnitPrefix?): Int = when (unit) {
            null -> 0
            else -> when {
                value < BigDecimal.ONE -> 3
                value < BigDecimal.TEN -> 2
                value < BigDecimal.parseString("100", 10) -> 1
                else -> 0
            }
        }
    }

    /**
     * Returns a string representation of this size value expressed in the unit
     * with the [UnitPrefix] which yields the most compact and readable number value.
     *
     * Special cases:
     *  - zero size is formatted as `"0 B"`
     *  - fraction sizes are formatted as `"0 B"`
     *  - very big sizes (more than a million [Yotta]byte/[Yobi]byte) are expressed
     *  in [Yotta]byte/[Yobi]byte and formatted in scientific notation
     *
     * @return the value of size in the automatically determined [UnitPrefix], e.g. 42.2 MB.
     */
    override fun toString(): String = toString<DecimalPrefix>()

    /**
     * Returns a string representation of this size value expressed in the unit
     * with the [UnitPrefix] which yields the most compact and readable number value.
     *
     * Special cases:
     *  - zero size is formatted as `"0 B"`
     *  - fraction sizes are formatted as `"0 B"`
     *  - very big sizes (more than a million [Yotta]byte/[Yobi]byte) are expressed
     *  in [Yotta]byte/[Yobi]byte and formatted in scientific notation
     *
     * @return the value of size in the automatically determined [UnitPrefix], e.g. 42.2 MB.
     */
    public inline fun <reified T : UnitPrefix> toString(prefixType: KClass<out UnitPrefix> = T::class, decimals: Int? = null): String {
        val prefixes: List<UnitPrefix?>? = supportedPrefixes[prefixType]
        require(prefixes != null) { "$prefixType is not supported. Valid options are: " + supportedPrefixes.keys }
        return when (bytes) {
            BigDecimal.ZERO -> "0 $SYMBOL"
            else -> {
                val absNs = bytes.abs()
                var scientific = false
                val index = prefixes.dropLastWhile { absNs >= it.factor }.size
                val millionish = prefixes.find { it != null }
                    ?.let { unitPrefix -> unitPrefix.radix.pow(2 * unitPrefix.radixExponent) }
                    ?: error("At least one supported unit prefix required.")
                if (index == 0 && absNs >= prefixes.first().factor * millionish) scientific = true
                val prefix = prefixes.getOrNull(index)
                val value = bytes.divide(prefix.factor)
                val formattedValue = when {
                    scientific -> value.formatScientifically()
                    else -> {
                        value.formatToExactDecimals(decimals ?: precision(value.abs(), prefix))
                    }
                }
                "$formattedValue ${prefix.getSymbol<Size>()}$SYMBOL"
            }
        }
    }

    /**
     * Returns a string representation of this size value expressed with the given [unitPrefix]
     * and formatted with the specified [decimals] number of digits after decimal point.
     *
     * @return the value of duration in the specified [unitPrefix]
     *
     * @throws IllegalArgumentException if [decimals] is less than zero.
     */
    public fun toString(unitPrefix: UnitPrefix, decimals: Int = 0): String {
        require(decimals >= 0) { "decimals must be not negative, but was $decimals" }
        val number = bytes.divide(unitPrefix.factor)
        val upperDetailLimit = 1e14.toBigDecimal()
        return when {
            number.abs() < upperDetailLimit -> number.formatToExactDecimals(decimals.coerceAtMost(12))
            else -> BigDecimal.fromBigDecimal(number, UnitPrefix.DECIMAL_MODE).roundToDigitPosition(3).formatScientifically()
        } + " " + unitPrefix.getSymbol<Size>() + SYMBOL
    }

    override fun compareTo(other: Size): Int = this.bytes.compareTo(other.bytes)
    public operator fun unaryPlus(): Size = this
    public operator fun unaryMinus(): Size = this * -BigDecimal.ONE

    public operator fun plus(other: BigDecimal): Size = Size(bytes + other)
    public operator fun plus(other: Number): Size = this + other.toBigDecimal()
    public operator fun plus(other: Size): Size = this + other.bytes

    public operator fun minus(other: BigDecimal): Size = Size(bytes - other)
    public operator fun minus(other: Number): Size = this - other.toBigDecimal()
    public operator fun minus(other: Size): Size = this - other.bytes

    public operator fun times(factor: BigDecimal): Size = (factor * bytes).bytes
    public operator fun times(factor: Number): Size = this * factor.toBigDecimal()

    public operator fun div(other: BigDecimal): Size = Size(bytes.div(other))
    public operator fun div(other: Number): Size = this / other.toBigDecimal()
    public operator fun div(other: Size): BigDecimal = bytes.div(other.bytes)
}

/**
 * Tries to parse this char sequence as a [Size] instance (e.g. `1 MiB` or `1.32GB`).
 *
 * @see parse
 */
public fun CharSequence.toSize(): Size = parse()

private fun Char.isDigit() = this in CharRanges.Numeric

/**
 * Tries to parse this char sequence as a [Size] instance (e.g. `1 MiB` or `1.32GB`).
 *
 * Sizes with and without decimals, as much as all binary and decimal units
 * either with or without a space between value and unit are supported.
 */
public fun CharSequence.parse(): Size {
    val trimmed = trim()
    val unitString = trimmed.takeLastWhile { !it.isDigit() && !it.isWhitespace() }
    val valueString = trimmed.dropLast(unitString.length).trim().toString()
    val value = valueString.toBigDecimal()
    return unitString.removeSuffix(Size.SYMBOL).let { it ->
        when {
            it.isBlank() -> value.bytes
            it == "K" -> (value * BinaryPrefix.Kibi.factor).bytes
            else -> Size.supportedPrefixes.flatMap { prefix -> prefix.value }.find { unit -> unit?.symbol == it }?.let { (value * it.factor).bytes }
        }
    } ?: throw IllegalArgumentException("${unitString.quoted} is no valid size unit like MB or GiB.")
}

/**
 * Contains the equivalent value as [bytes].
 */
public val Number.bytes: Size get() = if (this == 0) Size.ZERO else Size(toBigDecimal())


/**
 * Contains the equivalent value as [bytes].
 */
public val BigDecimal.bytes: Size get() = if (this == BigDecimal.ZERO) Size.ZERO else Size(this)


/**
 * Contains the equivalent value as [bytes].
 */
public val BigInteger.bytes: Size get() = if (this == BigInteger.ZERO) Size.ZERO else Size(toString(10).toBigDecimal(10))


/**
 * Contains the equivalent value as [bytes].
 */
public val Number.bits: Size get() = if (this == 0) Size.ZERO else toBigDecimal().bits

/**
 * Contains the equivalent value as [bytes].
 */
public val BigDecimal.bits: Size get() = if (this == BigDecimal.ZERO) Size.ZERO else Size(this.divide(Byte.SIZE_BITS.toBigDecimal()))

/**
 * Contains the equivalent value as [bytes].
 */
public val BigInteger.bits: Size get() = if (this == BigInteger.ZERO) Size.ZERO else toBigDecimal().bits

