package koodies.unit

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode

public interface UnitPrefix {
    public val symbol: String
    public val prefix: String
    public val basis: BigDecimal
    public val baseExponent: Int
    public val exponent: Int
    public val factor: BigDecimal

    public companion object {
        public val DECIMAL_MODE = DecimalMode(20, RoundingMode.ROUND_HALF_CEILING)
    }
}

public val UnitPrefix?.factor: BigDecimal get() = this?.factor ?: BigDecimal.ONE
public inline fun <reified T> UnitPrefix?.getSymbol(): String {
    if (this == null) return ""
    if (symbol == DecimalPrefix.kilo.symbol) {
        if (T::class == Size::class) return symbol.toUpperCase()
    }
    return symbol
}
