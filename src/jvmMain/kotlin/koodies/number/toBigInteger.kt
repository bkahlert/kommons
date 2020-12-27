package koodies.number

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.toBigInteger as kotlinToBigInteger

/**
 * Returns the value of this number as a [BigInteger], which may involve rounding.
 */
fun Number.toBigInteger(): BigInteger = when (this) {
    is BigDecimal -> toBigInteger()
    is Float -> toLong().kotlinToBigInteger()
    is Double -> toLong().kotlinToBigInteger()
    is Long -> kotlinToBigInteger()
    is Short -> toLong().kotlinToBigInteger()
    is BigInteger -> this
    is Byte -> toLong().kotlinToBigInteger()
    is Int -> kotlinToBigInteger()
    else -> BigInteger.valueOf(toLong())
}
