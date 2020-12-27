package koodies.unit

import java.math.BigDecimal

interface UnitPrefix {
    val symbol: String
    val prefix: String
    val basis: BigDecimal
    val baseExponent: Int
    val exponent: Int
    val factor: BigDecimal
}

val UnitPrefix?.factor: BigDecimal get() = this?.factor ?: BigDecimal.ONE
inline fun <reified T> UnitPrefix?.getSymbol(): String {
    if (this == null) return ""
    if (symbol == DecimalPrefix.kilo.symbol) {
        if (T::class == Size::class) return symbol.toUpperCase()
    }
    return symbol
}
