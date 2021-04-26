package koodies.number

import koodies.math.BigDecimal
import koodies.math.toAtMostDecimalsString
import koodies.math.toBigDecimal
import koodies.math.toExactDecimalsString
import koodies.math.toScientificString

@Deprecated("replace", ReplaceWith("this.toScientificString()", "koodies.math.formatScientifically"))
public fun Double.formatScientifically(): String = toBigDecimal().toScientificString()

@Deprecated("replace", ReplaceWith("this.formatScientifically()", "koodies.math.toScientificString"))
public fun BigDecimal.formatScientifically(): String = toScientificString()

@Deprecated("replace", ReplaceWith("this.toAtMostDecimalsString(decimals)", "koodies.math.toAtMostDecimalsString"))
public fun Double.formatToExactDecimals(decimals: Int): String = toBigDecimal().toExactDecimalsString(decimals)

@Deprecated("replace", ReplaceWith("this.toAtMostDecimalsString(decimals)", "koodies.math.toAtMostDecimalsString"))
public fun BigDecimal.formatToExactDecimals(decimals: Int): String = toExactDecimalsString(decimals)

@Deprecated("replace", ReplaceWith("this.toAtMostDecimalsString(decimals)", "koodies.math.toAtMostDecimalsString"))
public fun Double.formatUpToDecimals(decimals: Int): String = toBigDecimal().toAtMostDecimalsString(decimals)

@Deprecated("replace", ReplaceWith("this.toAtMostDecimalsString(decimals)", "koodies.math.toAtMostDecimalsString"))
public fun BigDecimal.formatUpToDecimals(decimals: Int): String = toAtMostDecimalsString(decimals)
