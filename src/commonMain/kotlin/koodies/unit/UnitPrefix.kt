package koodies.unit

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode

interface UnitPrefix {
    val symbol: String
    val prefix: String
    val basis: BigDecimal
    val baseExponent: Int
    val exponent: Int
    val factor: BigDecimal

    companion object {
        val DECIMAL_MODE = DecimalMode(20, RoundingMode.ROUND_HALF_CEILING)
    }
}

val UnitPrefix?.factor: BigDecimal get() = this?.factor ?: BigDecimal.ONE
inline fun <reified T> UnitPrefix?.getSymbol(): String {
    if (this == null) return ""
    if (symbol == DecimalPrefix.kilo.symbol) {
        if (T::class == Size::class) return symbol.toUpperCase()
    }
    return symbol
}
