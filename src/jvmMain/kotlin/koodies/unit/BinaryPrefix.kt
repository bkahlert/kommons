@file:Suppress("KDocMissingDocumentation", "EnumEntryName")

package koodies.unit

import koodies.number.BigDecimalConstants.TWO
import koodies.number.toBigDecimal
import java.math.BigDecimal
import kotlin.math.absoluteValue
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

enum class BinaryPrefix(
    override val symbol: String,
    override val exponent: Int,
    override val factor: BigDecimal =
        if (exponent > 0) TWO.pow(exponent)
        else BigDecimal.ONE / TWO.pow(exponent.absoluteValue),
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

    override val basis: BigDecimal = TWO
    override val baseExponent: Int = 10
    override val prefix: String get() = name.toLowerCase()
    override fun getValue(thisRef: Number, property: KProperty<*>): BigDecimal {
        require(exponent >= 0) { "Small $this are currently not fully supported!" }
        return thisRef.toBigDecimal() * factor
    }
}


/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Yobi]. */
val Number.Yobi: BigDecimal by BinaryPrefix.Yobi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Zebi]. */
val Number.Zebi: BigDecimal by BinaryPrefix.Zebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Exbi]. */
val Number.Exbi: BigDecimal by BinaryPrefix.Exbi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Pebi]. */
val Number.Pebi: BigDecimal by BinaryPrefix.Pebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Tebi]. */
val Number.Tebi: BigDecimal by BinaryPrefix.Tebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Gibi]. */
val Number.Gibi: BigDecimal by BinaryPrefix.Gibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Mebi]. */
val Number.Mebi: BigDecimal by BinaryPrefix.Mebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.Kibi]. */
val Number.Kibi: BigDecimal by BinaryPrefix.Kibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.mibi]. */
val Number.mibi: BigDecimal by BinaryPrefix.mibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.mubi]. */
val Number.mubi: BigDecimal by BinaryPrefix.mubi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.nabi]. */
val Number.nabi: BigDecimal by BinaryPrefix.nabi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.pibi]. */
val Number.pibi: BigDecimal by BinaryPrefix.pibi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.fembi]. */
val Number.fembi: BigDecimal by BinaryPrefix.fembi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.abi]. */
val Number.abi: BigDecimal by BinaryPrefix.abi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.zebi]. */
val Number.zebi_: BigDecimal by BinaryPrefix.zebi

/** Returns a [BigDecimal] equal to this [Number] times [BinaryPrefix.yobi]. */
val Number.yobi_: BigDecimal by BinaryPrefix.yobi
