@file:Suppress("EnumEntryName")

package koodies.unit

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import koodies.number.toBigDecimal
import koodies.unit.UnitPrefix.Companion.DECIMAL_MODE
import kotlin.math.absoluteValue
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("KDocMissingDocumentation")
public enum class DecimalPrefix(
    override val symbol: String,
    override val exponent: Int,
    override val factor: BigDecimal =
        if (exponent > 0) BigDecimal.TEN.pow(exponent.absoluteValue)
        else BigDecimal.ONE.divide(BigDecimal.TEN.pow(exponent.absoluteValue), DECIMAL_MODE),
) : UnitPrefix, ReadOnlyProperty<Number, BigDecimal> {
    Yotta("Y", 24),
    Zetta("Z", 21),
    Exa("E", 18),
    Peta("P", 15),
    Tera("T", 12),
    Giga("G", 9),
    Mega("M", 6),
    kilo("k", 3),
    hecto("h", 2),
    deca("da", 1),
    deci("d", -1),
    centi("c", -2),
    milli("m", -3),
    micro("μ", -6),
    nano("n", -9),
    pico("p", -12),
    femto("f", -15),
    atto("a", -18),
    zepto("z", -21),
    yocto("y", -24),
    ;

    override val basis: BigDecimal get() = BigDecimal.TEN
    override val baseExponent: Int = 3
    override val prefix: String get() = name.toLowerCase()
    override fun getValue(thisRef: Number, property: KProperty<*>): BigDecimal {
        require(exponent >= 0) { "Small $this are currently not fully supported!" }
        return thisRef.toBigDecimal() * factor
    }
}


/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.Yotta]. */
public val Number.Yotta: BigDecimal by DecimalPrefix.Yotta

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.Zetta]. */
public val Number.Zetta: BigDecimal by DecimalPrefix.Zetta

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.Exa]. */
public val Number.Exa: BigDecimal by DecimalPrefix.Exa

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.Peta]. */
public val Number.Peta: BigDecimal by DecimalPrefix.Peta

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.Tera]. */
public val Number.Tera: BigDecimal by DecimalPrefix.Tera

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.Giga]. */
public val Number.Giga: BigDecimal by DecimalPrefix.Giga

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.Mega]. */
public val Number.Mega: BigDecimal by DecimalPrefix.Mega

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.kilo]. */
public val Number.kilo: BigDecimal by DecimalPrefix.kilo

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.hecto]. */
public val Number.hecto: BigDecimal by DecimalPrefix.hecto

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.deca]. */
public val Number.deca: BigDecimal by DecimalPrefix.deca

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.deci]. */
public val Number.deci: BigDecimal by DecimalPrefix.deci

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.centi]. */
public val Number.centi: BigDecimal by DecimalPrefix.centi

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.milli]. */
public val Number.milli: BigDecimal by DecimalPrefix.milli

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.micro]. */
public val Number.micro: BigDecimal by DecimalPrefix.micro

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.nano]. */
public val Number.nano: BigDecimal by DecimalPrefix.nano

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.pico]. */
public val Number.pico: BigDecimal by DecimalPrefix.pico

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.femto]. */
public val Number.femto: BigDecimal by DecimalPrefix.femto

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.atto]. */
public val Number.atto: BigDecimal by DecimalPrefix.atto

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.zepto]. */
public val Number.zepto: BigDecimal by DecimalPrefix.zepto

/** Returns a [BigDecimal] equal to this [Number] times [DecimalPrefix.yocto]. */
public val Number.yocto: BigDecimal by DecimalPrefix.yocto
