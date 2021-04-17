@file:Suppress("EnumEntryName", "SpellCheckingInspection")

package koodies.unit

import koodies.math.BigDecimal
import koodies.math.BigDecimalConstants
import koodies.math.pow
import koodies.math.precision
import koodies.math.scale
import koodies.unit.DecimalPrefix.kilo
import kotlin.math.absoluteValue

/**
 * Metric prefixes in resolutions dating from 1960 to 1991 for use with the International System of Units (SI)
 * by the Bureau International des Poids et Mesures (BIPM).
 *
 * @see <a href="https://www.bipm.org/en/measurement-units/prefixes.html">BIPM - SI prefixes</a>
 */
public enum class DecimalPrefix(
    override val symbol: String,
    /**
     * Assuming this unit prefix is of the form `[radix]^[exponent]` (e.g. [kilo] ≙ `10³`),
     * then this field denotes the exponent (e.g. `3` for [kilo]).
     */
    private val exponent: Int,
) : UnitPrefix {

    /**
     * Yotta is a [UnitPrefix] in the metric system denoting multiplication by 10²⁴.
     */
    Yotta("Y", 24),

    /**
     * Zetta is a [UnitPrefix] in the metric system denoting multiplication by 10²¹.
     */
    Zetta("Z", 21),

    /**
     * Exa is a [UnitPrefix] in the metric system denoting multiplication by 10¹⁸.
     */
    Exa("E", 18),

    /**
     * Peta is a [UnitPrefix] in the metric system denoting multiplication by 10¹⁵.
     */
    Peta("P", 15),

    /**
     * Tera is a [UnitPrefix] in the metric system denoting multiplication by 10¹².
     */
    Tera("T", 12),

    /**
     * Giga is a [UnitPrefix] in the metric system denoting multiplication by 10⁹.
     */
    Giga("G", 9),

    /**
     * Mega is a [UnitPrefix] in the metric system denoting multiplication by 10⁶.
     */
    Mega("M", 6),

    /**
     * kilo is a [UnitPrefix] in the metric system denoting multiplication by 10³.
     */
    kilo("k", 3),

    /**
     * hecto is a [UnitPrefix] in the metric system denoting multiplication by 10².
     */
    hecto("h", 2),

    /**
     * deca is a [UnitPrefix] in the metric system denoting multiplication by 10¹.
     */
    deca("da", 1),

    /**
     * deci is a [UnitPrefix] in the metric system denoting multiplication by 10⁻¹.
     */
    deci("d", -1),

    /**
     * centi is a [UnitPrefix] in the metric system denoting multiplication by 10⁻².
     */
    centi("c", -2),

    /**
     * milli is a [UnitPrefix] in the metric system denoting multiplication by 10⁻³.
     */
    milli("m", -3),

    /**
     * micro is a [UnitPrefix] in the metric system denoting multiplication by 10⁻⁶.
     */
    micro("μ", -6),

    /**
     * nano is a [UnitPrefix] in the metric system denoting multiplication by 10⁻⁹.
     */
    nano("n", -9),

    /**
     * pico is a [UnitPrefix] in the metric system denoting multiplication by 10⁻¹².
     */
    pico("p", -12),

    /**
     * femto is a [UnitPrefix] in the metric system denoting multiplication by 10⁻¹⁵.
     */
    femto("f", -15),

    /**
     * atto is a [UnitPrefix] in the metric system denoting multiplication by 10⁻¹⁸.
     */
    atto("a", -18),

    /**
     * zepto is a [UnitPrefix] in the metric system denoting multiplication by 10⁻²¹.
     */
    zepto("z", -21),

    /**
     * yocto is a [UnitPrefix] in the metric system denoting multiplication by 10⁻²⁴.
     */
    yocto("y", -24),
    ;

    /**
     * Assuming this unit prefix is of the form `[radix]^[exponent]` (e.g. [kilo] ≙ `10³`),
     * then this field denotes the basis (e.g. `10` for [kilo]).
     */
    public val radix: BigDecimal = BigDecimalConstants.TEN

    override val baseFactor: BigDecimal = radix.pow(3)

    /**
     * Assuming this unit prefix is of the form `[radix]^[exponent]` (e.g. [kilo] ≙ `10³`),
     * then this field is result of the formula (e.g. `1000` for [kilo]).
     */
    public override val factor: BigDecimal by lazy { radix.pow(exponent, exponent.absoluteValue + 1) }

    override fun toString(): String = "${name.padStart(5)} ($symbol) ≔ " +
        "$radix^${exponent.toString().padStart(3)} = " +
        "${factor.toString().padStart(26)} (scale: ${factor.scale.toString().padStart(2)}, " +
        "precision: ${factor.precision.toString().padStart(3)})"
}

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.Yotta], that is multiplied by 10²⁴.
 */
public val Number.Yotta: BigDecimal by DecimalPrefix.Yotta

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.Zetta], that is multiplied by 10²¹.
 */
public val Number.Zetta: BigDecimal by DecimalPrefix.Zetta

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.Exa], that is multiplied by 10¹⁸.
 */
public val Number.Exa: BigDecimal by DecimalPrefix.Exa

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.Peta], that is multiplied by 10¹⁵.
 */
public val Number.Peta: BigDecimal by DecimalPrefix.Peta

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.Tera], that is multiplied by 10¹².
 */
public val Number.Tera: BigDecimal by DecimalPrefix.Tera

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.Giga], that is multiplied by 10⁹.
 */
public val Number.Giga: BigDecimal by DecimalPrefix.Giga

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.Mega], that is multiplied by 10⁶.
 */
public val Number.Mega: BigDecimal by DecimalPrefix.Mega

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.kilo], that is multiplied by 10³.
 */
public val Number.kilo: BigDecimal by DecimalPrefix.kilo

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.hecto], that is multiplied by 10².
 */
public val Number.hecto: BigDecimal by DecimalPrefix.hecto

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.deca], that is multiplied by 10¹.
 */
public val Number.deca: BigDecimal by DecimalPrefix.deca

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.deci], that is multiplied by 10⁻¹.
 */
public val Number.deci: BigDecimal by DecimalPrefix.deci

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.centi], that is multiplied by 10⁻².
 */
public val Number.centi: BigDecimal by DecimalPrefix.centi

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.milli], that is multiplied by 10⁻³.
 */
public val Number.milli: BigDecimal by DecimalPrefix.milli

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.micro], that is multiplied by 10⁻⁶.
 */
public val Number.micro: BigDecimal by DecimalPrefix.micro

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.nano], that is multiplied by 10⁻⁹.
 */
public val Number.nano: BigDecimal by DecimalPrefix.nano

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.pico], that is multiplied by 10⁻¹².
 */
public val Number.pico: BigDecimal by DecimalPrefix.pico

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.femto], that is multiplied by 10⁻¹⁵.
 */
public val Number.femto: BigDecimal by DecimalPrefix.femto

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.atto], that is multiplied by 10⁻¹⁸.
 */
public val Number.atto: BigDecimal by DecimalPrefix.atto

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.zepto], that is multiplied by 10⁻²¹.
 */
public val Number.zepto: BigDecimal by DecimalPrefix.zepto

/**
 * Denotes `this` number as prefixed with [DecimalPrefix.yocto], that is multiplied by 10⁻²⁴.
 */
public val Number.yocto: BigDecimal by DecimalPrefix.yocto
