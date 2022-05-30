@file:Suppress("EnumEntryName", "SpellCheckingInspection")

package com.bkahlert.kommons.unit

import com.bkahlert.kommons.math.BigDecimal
import com.bkahlert.kommons.math.BigDecimalConstants
import com.bkahlert.kommons.math.pow
import com.bkahlert.kommons.math.precision
import com.bkahlert.kommons.math.scale
import com.bkahlert.kommons.unit.BinaryPrefixes.BinaryPrefix
import com.bkahlert.kommons.unit.DecimalPrefixes.kilo
import kotlin.jvm.JvmName
import kotlin.math.absoluteValue

/**
 * Binary prefixes as defined by the [IEC 80000-13:2008](https://www.iso.org/standard/31898.html).
 */
public object BinaryPrefixes : AbstractList<BinaryPrefix>() {
    // Not implemented as enum class to avoid the enum field being proposed for code completion when typing `42.Yobi`.
    // Only the extension functions below are supposed to be proposed.

    /**
     * Radix: `2`
     */
    public val radix: BigDecimal = BigDecimalConstants.TWO

    private const val radixExponent: Int = 10
    private val baseFactor: BigDecimal = radix.pow(radixExponent)

    /**
     * @see BinaryPrefixes
     */
    public class BinaryPrefix internal constructor(
        override val symbol: String,
        /**
         * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
         * then this field denotes the exponent (e.g. `1` for [kilo]).
         */
        private val exponent: Int,
    ) : UnitPrefix {
        override val baseFactor: BigDecimal = BinaryPrefixes.baseFactor
        override val factor: BigDecimal = baseFactor.pow(exponent, exponent.absoluteValue * 3 + 1)
        override fun toString(): String = "${symbol.padStart(2)} ≔ " +
            "$radix^${(exponent * radixExponent).toString().padStart(3)} = " +
            "${factor.toString().padStart(26)} (scale: ${factor.scale.toString().padStart(2)}, " +
            "precision: ${factor.precision.toString().padStart(3)})"
    }

    private fun binaryPrefix(symbol: String, exponent: Int) = BinaryPrefix(symbol, exponent).also(list::add)

    private val list = mutableListOf<BinaryPrefix>()
    override val size: Int get() = list.size
    override fun get(index: Int): BinaryPrefix = list[index]

    /**
     * Yobi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁸⁰
     */
    public val Yobi: BinaryPrefix = binaryPrefix("Yi", 8)

    /**
     * Zebi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁷⁰
     */
    public val Zebi: BinaryPrefix = binaryPrefix("Zi", 7)

    /**
     * Exbi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁶⁰
     */
    public val Exbi: BinaryPrefix = binaryPrefix("Ei", 6)

    /**
     * Pebi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁵⁰
     */
    public val Pebi: BinaryPrefix = binaryPrefix("Pi", 5)

    /**
     * Tebi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁴⁰
     */
    public val Tebi: BinaryPrefix = binaryPrefix("Ti", 4)

    /**
     * Gibi is a [BinaryPrefix] in the binary system denoting multiplications by 2³⁰
     */
    public val Gibi: BinaryPrefix = binaryPrefix("Gi", 3)

    /**
     * Mebi is a [BinaryPrefix] in the binary system denoting multiplications by 2²⁰
     */
    public val Mebi: BinaryPrefix = binaryPrefix("Mi", 2)

    /**
     * Kibi is a [BinaryPrefix] in the binary system denoting multiplications by 2¹⁰
     */
    public val Kibi: BinaryPrefix = binaryPrefix("Ki", 1)

    /**
     * mibi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻¹⁰
     */
    public val mibi: BinaryPrefix = binaryPrefix("mi", -1)

    /**
     * mubi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻²⁰
     */
    public val mubi: BinaryPrefix = binaryPrefix("ui", -2)

    /**
     * nabi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻³⁰
     */
    public val nabi: BinaryPrefix = binaryPrefix("ni", -3)

    /**
     * pibi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻⁴⁰
     */
    public val pibi: BinaryPrefix = binaryPrefix("pi", -4)

    /**
     * fembi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻⁵⁰
     */
    public val fembi: BinaryPrefix = binaryPrefix("fi", -5)

    /**
     * abi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻⁶⁰
     */
    public val abi: BinaryPrefix = binaryPrefix("ai", -6)

    /**
     * zebi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻⁷⁰
     */
    @get:JvmName("getzebi")
    public val zebi: BinaryPrefix = binaryPrefix("Zi", -7)

    /**
     * yobi is a [BinaryPrefix] in the binary system denoting multiplications by 2⁻⁸⁰
     */
    @get:JvmName("getyobi")
    public val yobi: BinaryPrefix = binaryPrefix("Yi", -8)
}

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Yobi], that is multiplied by 2⁸⁰.
 */
public val Number.Yobi: BigDecimal get() = BinaryPrefixes.Yobi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Zebi], that is multiplied by 2⁷⁰.
 */
public val Number.Zebi: BigDecimal get() = BinaryPrefixes.Zebi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Exbi], that is multiplied by 2⁶⁰.
 */
public val Number.Exbi: BigDecimal get() = BinaryPrefixes.Exbi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Pebi], that is multiplied by 2⁵⁰.
 */
public val Number.Pebi: BigDecimal get() = BinaryPrefixes.Pebi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Tebi], that is multiplied by 2⁴⁰.
 */
public val Number.Tebi: BigDecimal get() = BinaryPrefixes.Tebi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Gibi], that is multiplied by 2³⁰.
 */
public val Number.Gibi: BigDecimal get() = BinaryPrefixes.Gibi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Mebi], that is multiplied by 2²⁰.
 */
public val Number.Mebi: BigDecimal get() = BinaryPrefixes.Mebi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.Kibi], that is multiplied by 2¹⁰.
 */
public val Number.Kibi: BigDecimal get() = BinaryPrefixes.Kibi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.mibi], that is multiplied by 2⁻¹⁰.
 */
public val Number.mibi: BigDecimal get() = BinaryPrefixes.mibi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.mubi], that is multiplied by 2⁻²⁰.
 */
public val Number.mubi: BigDecimal get() = BinaryPrefixes.mubi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.nabi], that is multiplied by 2⁻³⁰.
 */
public val Number.nabi: BigDecimal get() = BinaryPrefixes.nabi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.pibi], that is multiplied by 2⁻⁴⁰.
 */
public val Number.pibi: BigDecimal get() = BinaryPrefixes.pibi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.fembi], that is multiplied by 2⁻⁵⁰.
 */
public val Number.fembi: BigDecimal get() = BinaryPrefixes.fembi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.abi], that is multiplied by 2⁻⁶⁰.
 */
public val Number.abi: BigDecimal get() = BinaryPrefixes.abi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.zebi], that is multiplied by 2⁻⁷⁰.
 */
@get:JvmName("getzebi")
public val Number.zebi: BigDecimal
    get() = BinaryPrefixes.zebi.of(this)

/**
 * Denotes this number as prefixed with [BinaryPrefixes.yobi], that is multiplied by 2⁻⁸⁰.
 */
@get:JvmName("getyobi")
public val Number.yobi: BigDecimal
    get() = BinaryPrefixes.yobi.of(this)
