@file:Suppress("KDocMissingDocumentation", "EnumEntryName")

package koodies.unit

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import koodies.number.toBigDecimal
import koodies.unit.UnitPrefix.Companion.DECIMAL_MODE
import kotlin.math.absoluteValue
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public enum class BinaryPrefix(
    override val symbol: String,
    override val exponent: Int,
    override val factor: BigDecimal =
        if (exponent > 0) BigDecimal.TWO.pow(exponent)
        else BigDecimal.ONE.divide(BigDecimal.TWO.pow(exponent.absoluteValue), DECIMAL_MODE),
) : UnitPrefix, ReadOnlyProperty<Number, BigDecimal> {
    Yobi("Yi", 80),
    Zebi("Zi", 70),
    Exbi("Ei", 60),
    Pebi("Pi", 50),
    Tebi("Ti", 40),
    Gibi("Gi", 30),
    Mebi("Mi", 20),
    Kibi("Ki", 10),
    mibi("mi", -10),
    mubi("ui", -20),
    nabi("ni", -30),
    pibi("pi", -40),
    fembi("fi", -50),
    abi("ai", -60),
    zebi("Zi", -70),
    yobi("Yi", -80),
    ; // TODO radix = 1024, POWER 1,2,3...

    override val basis: BigDecimal = BigDecimal.TWO
    override val baseExponent: Int = 10
    override val prefix: String get() = name.toLowerCase()
    override fun getValue(thisRef: Number, property: KProperty<*>): BigDecimal {
        require(exponent >= 0) { "Small $this are currently not fully supported!" }
        return thisRef.toBigDecimal() * factor
    }
}


/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Yobi]. */
public val Number.Yobi: BigDecimal by BinaryPrefix.Yobi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Zebi]. */
public val Number.Zebi: BigDecimal by BinaryPrefix.Zebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Exbi]. */
public val Number.Exbi: BigDecimal by BinaryPrefix.Exbi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Pebi]. */
public val Number.Pebi: BigDecimal by BinaryPrefix.Pebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Tebi]. */
public val Number.Tebi: BigDecimal by BinaryPrefix.Tebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Gibi]. */
public val Number.Gibi: BigDecimal by BinaryPrefix.Gibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Mebi]. */
public val Number.Mebi: BigDecimal by BinaryPrefix.Mebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Kibi]. */
public val Number.Kibi: BigDecimal by BinaryPrefix.Kibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.mibi]. */
public val Number.mibi: BigDecimal by BinaryPrefix.mibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.mubi]. */
public val Number.mubi: BigDecimal by BinaryPrefix.mubi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.nabi]. */
public val Number.nabi: BigDecimal by BinaryPrefix.nabi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.pibi]. */
public val Number.pibi: BigDecimal by BinaryPrefix.pibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.fembi]. */
public val Number.fembi: BigDecimal by BinaryPrefix.fembi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.abi]. */
public val Number.abi: BigDecimal by BinaryPrefix.abi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.zebi]. */
public val Number.zebi_: BigDecimal by BinaryPrefix.zebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.yobi]. */
public val Number.yobi_: BigDecimal by BinaryPrefix.yobi
