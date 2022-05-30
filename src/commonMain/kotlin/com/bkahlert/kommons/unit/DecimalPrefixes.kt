@file:Suppress("EnumEntryName", "SpellCheckingInspection")

package com.bkahlert.kommons.unit

import com.bkahlert.kommons.math.BigDecimal
import com.bkahlert.kommons.math.BigDecimalConstants
import com.bkahlert.kommons.math.pow
import com.bkahlert.kommons.math.precision
import com.bkahlert.kommons.math.scale
import com.bkahlert.kommons.unit.DecimalPrefixes.DecimalPrefix
import com.bkahlert.kommons.unit.DecimalPrefixes.kilo
import kotlin.math.absoluteValue

/**
 * Metric prefixes in resolutions dating from 1960 to 1991 for use with the International System of Units (SI)
 * by the Bureau International des Poids et Mesures (BIPM).
 *
 * @see <a href="https://www.bipm.org/en/measurement-units/prefixes.html">BIPM - SI prefixes</a>
 */
public object DecimalPrefixes : AbstractList<DecimalPrefix>() {
    // Not implemented as enum class to avoid the enum field being proposed for code completion when typing `42.Yotta`.
    // Only the extension functions below are supposed to be proposed.

    /**
     * Radix: `10`
     */
    public val radix: BigDecimal = BigDecimalConstants.TEN
    private val baseFactor: BigDecimal = radix.pow(3)

    /**
     * @see DecimalPrefixes
     */
    public class DecimalPrefix internal constructor(
        override val symbol: String,
        /**
         * Assuming this unit prefix is of the form `[radix]^[exponent]` (e.g. [kilo] ≙ `10³`),
         * then this field denotes the exponent (e.g. `3` for [kilo]).
         */
        private val exponent: Int,
    ) : UnitPrefix {
        override val baseFactor: BigDecimal = DecimalPrefixes.baseFactor
        override val factor: BigDecimal = radix.pow(exponent, exponent.absoluteValue + 1)
        override fun toString(): String = "${symbol.padStart(2)} ≔ " +
            "$radix^${exponent.toString().padStart(3)} = " +
            "${factor.toString().padStart(26)} (scale: ${factor.scale.toString().padStart(2)}, " +
            "precision: ${factor.precision.toString().padStart(3)})"
    }

    private fun decimalPrefix(symbol: String, exponent: Int) = DecimalPrefix(symbol, exponent).also(list::add)

    private val list = mutableListOf<DecimalPrefix>()
    override val size: Int get() = list.size
    override fun get(index: Int): DecimalPrefix = list[index]

    /**
     * Yotta is a [UnitPrefix] in the metric system denoting multiplication by 10²⁴.
     */
    public val Yotta: DecimalPrefix = decimalPrefix("Y", 24)

    /**
     * Zetta is a [DecimalPrefix] in the metric system denoting multiplication by 10²¹.
     */
    public val Zetta: DecimalPrefix = decimalPrefix("Z", 21)

    /**
     * Exa is a [DecimalPrefix] in the metric system denoting multiplication by 10¹⁸.
     */
    public val Exa: DecimalPrefix = decimalPrefix("E", 18)

    /**
     * Peta is a [DecimalPrefix] in the metric system denoting multiplication by 10¹⁵.
     */
    public val Peta: DecimalPrefix = decimalPrefix("P", 15)

    /**
     * Tera is a [DecimalPrefix] in the metric system denoting multiplication by 10¹².
     */
    public val Tera: DecimalPrefix = decimalPrefix("T", 12)

    /**
     * Giga is a [DecimalPrefix] in the metric system denoting multiplication by 10⁹.
     */
    public val Giga: DecimalPrefix = decimalPrefix("G", 9)

    /**
     * Mega is a [DecimalPrefix] in the metric system denoting multiplication by 10⁶.
     */
    public val Mega: DecimalPrefix = decimalPrefix("M", 6)

    /**
     * kilo is a [DecimalPrefix] in the metric system denoting multiplication by 10³.
     */
    public val kilo: DecimalPrefix = decimalPrefix("k", 3)

    /**
     * hecto is a [DecimalPrefix] in the metric system denoting multiplication by 10².
     */
    public val hecto: DecimalPrefix = decimalPrefix("h", 2)

    /**
     * deca is a [DecimalPrefix] in the metric system denoting multiplication by 10¹.
     */
    public val deca: DecimalPrefix = decimalPrefix("da", 1)

    /**
     * deci is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻¹.
     */
    public val deci: DecimalPrefix = decimalPrefix("d", -1)

    /**
     * centi is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻².
     */
    public val centi: DecimalPrefix = decimalPrefix("c", -2)

    /**
     * milli is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻³.
     */
    public val milli: DecimalPrefix = decimalPrefix("m", -3)

    /**
     * micro is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻⁶.
     */
    public val micro: DecimalPrefix = decimalPrefix("μ", -6)

    /**
     * nano is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻⁹.
     */
    public val nano: DecimalPrefix = decimalPrefix("n", -9)

    /**
     * pico is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻¹².
     */
    public val pico: DecimalPrefix = decimalPrefix("p", -12)

    /**
     * femto is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻¹⁵.
     */
    public val femto: DecimalPrefix = decimalPrefix("f", -15)

    /**
     * atto is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻¹⁸.
     */
    public val atto: DecimalPrefix = decimalPrefix("a", -18)

    /**
     * zepto is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻²¹.
     */
    public val zepto: DecimalPrefix = decimalPrefix("z", -21)

    /**
     * yocto is a [DecimalPrefix] in the metric system denoting multiplication by 10⁻²⁴.
     */
    public val yocto: DecimalPrefix = decimalPrefix("y", -24)
}

/**
 * Denotes this number as prefixed with [DecimalPrefixes.Yotta], that is multiplied by 10²⁴.
 */
public val Number.Yotta: BigDecimal get() = DecimalPrefixes.Yotta.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.Zetta], that is multiplied by 10²¹.
 */
public val Number.Zetta: BigDecimal get() = DecimalPrefixes.Zetta.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.Exa], that is multiplied by 10¹⁸.
 */
public val Number.Exa: BigDecimal get() = DecimalPrefixes.Exa.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.Peta], that is multiplied by 10¹⁵.
 */
public val Number.Peta: BigDecimal get() = DecimalPrefixes.Peta.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.Tera], that is multiplied by 10¹².
 */
public val Number.Tera: BigDecimal get() = DecimalPrefixes.Tera.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.Giga], that is multiplied by 10⁹.
 */
public val Number.Giga: BigDecimal get() = DecimalPrefixes.Giga.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.Mega], that is multiplied by 10⁶.
 */
public val Number.Mega: BigDecimal get() = DecimalPrefixes.Mega.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.kilo], that is multiplied by 10³.
 */
public val Number.kilo: BigDecimal get() = DecimalPrefixes.kilo.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.hecto], that is multiplied by 10².
 */
public val Number.hecto: BigDecimal get() = DecimalPrefixes.hecto.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.deca], that is multiplied by 10¹.
 */
public val Number.deca: BigDecimal get() = DecimalPrefixes.deca.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.deci], that is multiplied by 10⁻¹.
 */
public val Number.deci: BigDecimal get() = DecimalPrefixes.deci.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.centi], that is multiplied by 10⁻².
 */
public val Number.centi: BigDecimal get() = DecimalPrefixes.centi.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.milli], that is multiplied by 10⁻³.
 */
public val Number.milli: BigDecimal get() = DecimalPrefixes.milli.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.micro], that is multiplied by 10⁻⁶.
 */
public val Number.micro: BigDecimal get() = DecimalPrefixes.micro.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.nano], that is multiplied by 10⁻⁹.
 */
public val Number.nano: BigDecimal get() = DecimalPrefixes.nano.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.pico], that is multiplied by 10⁻¹².
 */
public val Number.pico: BigDecimal get() = DecimalPrefixes.pico.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.femto], that is multiplied by 10⁻¹⁵.
 */
public val Number.femto: BigDecimal get() = DecimalPrefixes.femto.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.atto], that is multiplied by 10⁻¹⁸.
 */
public val Number.atto: BigDecimal get() = DecimalPrefixes.atto.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.zepto], that is multiplied by 10⁻²¹.
 */
public val Number.zepto: BigDecimal get() = DecimalPrefixes.zepto.of(this)

/**
 * Denotes this number as prefixed with [DecimalPrefixes.yocto], that is multiplied by 10⁻²⁴.
 */
public val Number.yocto: BigDecimal get() = DecimalPrefixes.yocto.of(this)
