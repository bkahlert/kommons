package koodies.number

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger

/**
 * Returns the value of this number as a [BigInteger], which may involve rounding.
 */
fun Number.toBigInteger(): BigInteger = when (this) {
    is Float -> toBigDecimal(DecimalMode.DEFAULT).toBigInteger()
    is Double -> toBigDecimal(DecimalMode.DEFAULT).toBigInteger()
    is Long -> toBigInteger()
    is Short -> toBigInteger()
    is Byte -> toBigInteger()
    is Int -> toBigInteger()
    else -> BigInteger.fromLong(toLong())
}

/**
 * Returns the value of this number as a [BigDecimal], which may involve rounding.
 */
fun Number.toBigDecimal(): BigDecimal = when (this) {
    is Float -> toBigDecimal(DecimalMode.DEFAULT)
    is Double -> toBigDecimal(DecimalMode.DEFAULT)
    is Long -> toBigDecimal()
    is Short -> toBigDecimal()
    is Byte -> toDouble().toBigDecimal(DecimalMode.DEFAULT)
    is Int -> toBigDecimal()
    else -> BigDecimal.fromDouble(toDouble(), DecimalMode.DEFAULT)
}

fun BigInteger.toInt() = intValue()
fun BigDecimal.toInt() = intValue()

fun BigInteger.toBigDecimal() = toString(10).toBigDecimal(10)
