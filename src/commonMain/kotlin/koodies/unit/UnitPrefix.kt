package koodies.unit

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import koodies.unit.DecimalPrefix.kilo
import koodies.unit.DecimalPrefix.milli

/**
 * > *A unit prefix is a specifier or mnemonic that is prepended to units of measurement to indicate multiples or fractions of the units.
 * Units of various sizes are commonly formed by the use of such prefixes.
 * The prefixes of the metric system, such as [kilo] and [milli], represent multiplication by powers of ten.*
 *
 * > *In information technology it is common to use [binary prefixes][BinaryPrefix], which are based on powers of two.
 * Historically, many prefixes have been used or proposed by various sources, but only a narrow set has been recognised by standards organisations.*
 *
 * @see <a href="https://en.wikipedia.org/wiki/Unit_prefix">Wikipedia: Unit prefix</a>
 * @see DecimalPrefix
 * @see BinaryPrefix
 */
public interface UnitPrefix {

    /**
     * The string a unit's symbol is prefixed with, e.g. `1 kg` corresponds to `1000 g`, whereas `k` is the symbol for [kilo].
     */
    public val symbol: String

    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field denotes the basis (e.g. `10` for [kilo]).
     */
    public val radix: BigDecimal

    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field denotes the base exponent (e.g. `3` for [kilo]).
     */
    public val radixExponent: Int

    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field denotes the exponent (e.g. `1` for [kilo]).
     */
    public val exponent: Int

    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field is result of the formula (e.g. `1000` for [kilo]).
     */
    public val factor: BigDecimal

    public companion object {
        /**
         * [DecimalMode] used to round [factor].
         *
         * @see [BigDecimal.divide]
         */
        public val DECIMAL_MODE: DecimalMode = DecimalMode(20, RoundingMode.ROUND_HALF_CEILING)
    }
}

/**
 * Assuming this unit prefix is of the form `([UnitPrefix.radix]^[UnitPrefix.radixExponent])^[UnitPrefix.exponent]` (e.g. [kilo] ≙ `(10³)¹`),
 * then this field is result of the formula (e.g. `1000` for [kilo]).
 */
public val UnitPrefix?.factor: BigDecimal get() = this?.factor ?: BigDecimal.ONE

/**
 * Returns the string a unit's symbol is prefixed with, e.g. `1 kg` corresponds to `1000 g`, whereas `k` is the symbol for [kilo].
 */
public inline fun <reified T> UnitPrefix?.getSymbol(): String = when {
    this == null -> ""
    symbol == kilo.symbol && T::class == Size::class -> {
        symbol.toUpperCase()
    }
    else -> symbol
}
