package koodies.number

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.toBigDecimal as kotlinToBigDecimal

/**
 * Returns the value of this number as a [BigDecimal], which may involve rounding.
 */
fun Number.toBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is Float -> kotlinToBigDecimal()
    is Double -> kotlinToBigDecimal()
    is Long -> kotlinToBigDecimal()
    is Short -> BigDecimal(toString())
    is BigInteger -> kotlinToBigDecimal()
    is Byte -> toDouble().kotlinToBigDecimal()
    is Int -> kotlinToBigDecimal()
    else -> BigDecimal.valueOf(toDouble())
}


