package koodies.number

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import com.ionspin.kotlin.bignum.integer.BigInteger

public fun Double.formatScientifically(): String = toBigDecimal().formatScientifically()
public fun BigDecimal.formatScientifically(): String = roundSignificand(DecimalMode(5, RoundingMode.ROUND_HALF_CEILING)).run {

    fun placeADotInString(input: String, position: Int): String {

        val prefix = input.substring(0 until input.length - position)
        val suffix = input.substring(input.length - position until input.length).run {
            when {
                length > 2 -> substring(0, 2)
                length < 2 -> padEnd(2, '0')
                else -> this
            }
        }
        val prepared = "$prefix.$suffix"

        return prepared
    }

    val significandString = significand.toString(10)
    val modifier = if (significand < 0) 2 else 1

    val formattedSignificant = placeADotInString(significandString, significandString.length - modifier)
    if (exponent > 0 || (significand > BigInteger.ZERO && exponent == 0L)) {
        "${formattedSignificant}e+$exponent"
    } else {
        "${formattedSignificant}e${exponent.takeUnless { it < 0 && significand == BigInteger.ZERO } ?: "0"}"
    }
}

public fun Double.formatToExactDecimals(decimals: Int): String = toBigDecimal().formatToExactDecimals(decimals)
public fun BigDecimal.formatToExactDecimals(decimals: Int): String =
    roundToDigitPositionAfterDecimalPoint(decimals.toLong(), RoundingMode.ROUND_HALF_TOWARDS_ZERO).toStringExpanded()
        .split(".").run { first() to lastOrNull()?.takeIf { size > 1 } }.let { (integer, actualDecimals) ->
            StringBuilder(integer).apply {
                if (decimals > 0) {
                    append(".")
                    actualDecimals?.let { append(it) }
                    (decimals - (actualDecimals?.length ?: 0)).takeIf { it > 0 }?.let { append("0".repeat(it)) }
                }
            }.toString()
        }

public fun Double.formatUpToDecimals(decimals: Int): String = toBigDecimal().formatUpToDecimals(decimals)
public fun BigDecimal.formatUpToDecimals(decimals: Int): String =
    roundToDigitPositionAfterDecimalPoint(decimals.toLong(), RoundingMode.ROUND_HALF_CEILING).toStringExpanded()
