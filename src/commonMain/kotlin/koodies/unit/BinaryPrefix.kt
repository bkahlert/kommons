@file:Suppress("EnumEntryName", "SpellCheckingInspection")

package koodies.unit

import koodies.math.BigDecimal
import koodies.math.BigDecimalConstants
import koodies.math.pow
import koodies.math.precision
import koodies.math.scale
import koodies.unit.DecimalPrefix.kilo
import kotlin.jvm.JvmName
import kotlin.math.absoluteValue

/**
 * Binary prefixes as defined by the [IEC 80000-13:2008](https://www.iso.org/standard/31898.html).
 */
public enum class BinaryPrefix(
    override val symbol: String,
    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field denotes the exponent (e.g. `1` for [kilo]).
     */
    private val exponent: Int,
) : UnitPrefix {

    /**
     * Yobi is a [UnitPrefix] in the binary system denoting multiplications by 2⁸⁰
     */
    Yobi("Yi", 8),

    /**
     * Zebi is a [UnitPrefix] in the binary system denoting multiplications by 2⁷⁰
     */
    Zebi("Zi", 7),

    /**
     * Exbi is a [UnitPrefix] in the binary system denoting multiplications by 2⁶⁰
     */
    Exbi("Ei", 6),

    /**
     * Pebi is a [UnitPrefix] in the binary system denoting multiplications by 2⁵⁰
     */
    Pebi("Pi", 5),

    /**
     * Tebi is a [UnitPrefix] in the binary system denoting multiplications by 2⁴⁰
     */
    Tebi("Ti", 4),

    /**
     * Gibi is a [UnitPrefix] in the binary system denoting multiplications by 2³⁰
     */
    Gibi("Gi", 3),

    /**
     * Mebi is a [UnitPrefix] in the binary system denoting multiplications by 2²⁰
     */
    Mebi("Mi", 2),

    /**
     * Kibi is a [UnitPrefix] in the binary system denoting multiplications by 2¹⁰
     */
    Kibi("Ki", 1),

    /**
     * mibi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻¹⁰
     */
    mibi("mi", -1),

    /**
     * mubi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻²⁰
     */
    mubi("ui", -2),

    /**
     * nabi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻³⁰
     */
    nabi("ni", -3),

    /**
     * pibi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻⁴⁰
     */
    pibi("pi", -4),

    /**
     * fembi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻⁵⁰
     */
    fembi("fi", -5),

    /**
     * abi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻⁶⁰
     */
    abi("ai", -6),

    /**
     * zebi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻⁷⁰
     */
    zebi("Zi", -7),

    /**
     * yobi is a [UnitPrefix] in the binary system denoting multiplications by 2⁻⁸⁰
     */
    yobi("Yi", -8),
    ;

    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field denotes the basis (e.g. `10` for [kilo]).
     */
    public val radix: BigDecimal = BigDecimalConstants.TWO

    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field denotes the base exponent (e.g. `3` for [kilo]).
     */
    public val radixExponent: Int = 10

    override val baseFactor: BigDecimal = radix.pow(radixExponent)

    /**
     * Assuming this unit prefix is of the form `([radix]^[radixExponent])^[exponent]` (e.g. [kilo] ≙ `(10³)¹`),
     * then this field is result of the formula (e.g. `1000` for [kilo]).
     */
    public override val factor: BigDecimal by lazy { baseFactor.pow(exponent, exponent.absoluteValue * 3 + 1) }

    override fun toString(): String = "${name.padStart(5)} ($symbol) ≔ " +
        "$radix^${(exponent * radixExponent).toString().padStart(3)} = " +
        "${factor.toString().padStart(26)} (scale: ${factor.scale.toString().padStart(2)}, " +
        "precision: ${factor.precision.toString().padStart(3)})"
}
// TODO refactor so that only one import is offered when typing "3."
/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Yobi], that is multiplied by 2⁸⁰.
 */
public val Number.Yobi: BigDecimal by BinaryPrefix.Yobi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Zebi], that is multiplied by 2⁷⁰.
 */
public val Number.Zebi: BigDecimal by BinaryPrefix.Zebi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Exbi], that is multiplied by 2⁶⁰.
 */
public val Number.Exbi: BigDecimal by BinaryPrefix.Exbi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Pebi], that is multiplied by 2⁵⁰.
 */
public val Number.Pebi: BigDecimal by BinaryPrefix.Pebi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Tebi], that is multiplied by 2⁴⁰.
 */
public val Number.Tebi: BigDecimal by BinaryPrefix.Tebi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Gibi], that is multiplied by 2³⁰.
 */
public val Number.Gibi: BigDecimal by BinaryPrefix.Gibi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Mebi], that is multiplied by 2²⁰.
 */
public val Number.Mebi: BigDecimal by BinaryPrefix.Mebi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.Kibi], that is multiplied by 2¹⁰.
 */
public val Number.Kibi: BigDecimal by BinaryPrefix.Kibi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.mibi], that is multiplied by 2⁻¹⁰.
 */
public val Number.mibi: BigDecimal by BinaryPrefix.mibi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.mubi], that is multiplied by 2⁻²⁰.
 */
public val Number.mubi: BigDecimal by BinaryPrefix.mubi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.nabi], that is multiplied by 2⁻³⁰.
 */
public val Number.nabi: BigDecimal by BinaryPrefix.nabi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.pibi], that is multiplied by 2⁻⁴⁰.
 */
public val Number.pibi: BigDecimal by BinaryPrefix.pibi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.fembi], that is multiplied by 2⁻⁵⁰.
 */
public val Number.fembi: BigDecimal by BinaryPrefix.fembi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.abi], that is multiplied by 2⁻⁶⁰.
 */
public val Number.abi: BigDecimal by BinaryPrefix.abi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.zebi], that is multiplied by 2⁻⁷⁰.
 */
@get:JvmName("getzebi")
public val Number.zebi: BigDecimal by BinaryPrefix.zebi

/**
 * Denotes `this` number as prefixed with [BinaryPrefix.yobi], that is multiplied by 2⁻⁸⁰.
 */
@get:JvmName("getyobi")
public val Number.yobi: BigDecimal by BinaryPrefix.yobi
