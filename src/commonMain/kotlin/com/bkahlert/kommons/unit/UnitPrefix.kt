package com.bkahlert.kommons.unit

import com.bkahlert.kommons.math.BigDecimal
import com.bkahlert.kommons.math.BigDecimalConstants
import com.bkahlert.kommons.math.BigInteger
import com.bkahlert.kommons.math.times
import com.bkahlert.kommons.math.toBigDecimal
import com.bkahlert.kommons.takeUnlessBlank
import com.bkahlert.kommons.takeUnlessEmpty
import com.bkahlert.kommons.text.CharRanges
import com.bkahlert.kommons.unit.DecimalPrefixes.kilo
import com.bkahlert.kommons.unit.DecimalPrefixes.milli

/**
 * > *A unit prefix is a specifier or mnemonic that is prepended to units of measurement to indicate multiples or fractions of the units.
 * Units of various sizes are commonly formed by the use of such prefixes.
 * The prefixes of the metric system, such as [kilo] and [milli], represent multiplication by powers of ten.*
 *
 * > *In information technology it is common to use [binary prefixes][BinaryPrefixes], which are based on powers of two.
 * Historically, many prefixes have been used or proposed by various sources, but only a narrow set has been recognised by standards organisations.*
 *
 * @see <a href="https://en.wikipedia.org/wiki/Unit_prefix">Wikipedia: Unit prefix</a>
 * @see DecimalPrefixes
 * @see BinaryPrefixes
 */
public interface UnitPrefix {

    /**
     * The string a unit's symbol is prefixed with, e.g. `1 kg` corresponds to `1000 g`, whereas `k` is the symbol for [kilo].
     */
    public val symbol: String

    /**
     * One resolution step, e.g. `1000` or `1024`.
     */
    public val baseFactor: BigDecimal

    /**
     * Factor by which a number needs to be multiplied.
     */
    public val factor: BigDecimal

    /**
     * Calculates the number based on the given [number] that represents this
     * unit.
     */
    public fun of(number: Number): BigDecimal =
        when (number) {
            is Byte -> number.toBigDecimal()
            is Short -> number.toBigDecimal()
            is Int -> number.toBigDecimal()
            is Long -> number.toBigDecimal()
            is BigDecimal -> number
            is BigInteger -> number.toBigDecimal()
            else -> number.toDouble().toBigDecimal()
        }.times(factor, 0)

    public companion object {

        private fun Char.isDigit() = this in CharRanges.Numeric

        private val knownPrefixes: List<UnitPrefix> by lazy { sequenceOf(*BinaryPrefixes.toTypedArray(), *DecimalPrefixes.toTypedArray()).toList() }

        private fun parseUnitPrefixOrNull(text: CharSequence): UnitPrefix? =
            when (text.trim()) {
                "" -> null
                "K" -> BinaryPrefixes.Kibi
                else -> knownPrefixes.find { unit -> unit.symbol == text }
            }

        /**
         * Tries to parse this character sequence as an instance of any unit (e.g. `1 MiB` or `1.32GB`).
         *
         * Sizes with and without decimals, as much as all binary and decimal units
         * either with or without a space between value and unit are supported.
         */
        public fun parse(text: CharSequence): Pair<BigDecimal, String?> {
            val trimmed = text.trim()

            val unitString = trimmed.takeLastWhile { !it.isDigit() && !it.isWhitespace() }
            val unitPrefixAndLength = generateSequence(unitString) { it.dropLast(1).takeUnlessEmpty() }.mapNotNull { prefix ->
                parseUnitPrefixOrNull(prefix)?.let { unitPrefix -> unitPrefix to prefix.length }
            }.firstOrNull() ?: (null to 0)

            val factorAndBaseUnit = unitPrefixAndLength.let { (unitPrefix, length) ->
                unitPrefix.factor to unitString.substring(length).takeUnlessBlank()
            }

            val valueString = trimmed.dropLast(unitString.length).trim()

            val value = "$valueString".toBigDecimal().let { value -> factorAndBaseUnit.let { (factor, _) -> value * factor } }

            return value to factorAndBaseUnit.let { (_, baseUnit) -> baseUnit }
        }
    }
}

/**
 * Assuming this unit prefix is of the form `([UnitPrefix.radix]^[UnitPrefix.radixExponent])^[UnitPrefix.exponent]` (e.g. [kilo] ≙ `(10³)¹`),
 * then this field is result of the formula (e.g. `1000` for [kilo]).
 */
public inline val <reified T : UnitPrefix> T?.factor: BigDecimal
    get() = this?.factor ?: BigDecimalConstants.ONE

/**
 * Returns the string a unit's symbol is prefixed with, e.g. `1 kg` corresponds to `1000 g`, whereas `k` is the symbol for [kilo].
 */
public inline fun <reified T> UnitPrefix?.getSymbol(): String = when {
    this == null -> ""
    symbol == kilo.symbol && T::class == Size::class -> symbol.uppercase()
    else -> symbol
}
