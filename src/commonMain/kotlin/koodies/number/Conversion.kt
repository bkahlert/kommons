package koodies.number

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.toBigInteger

/**
 * Returns the value of this number as a [BigInteger], which may involve rounding.
 */
public fun Number.toBigInteger(): BigInteger = when (this) {
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
public fun Number.toBigDecimal(): BigDecimal = when (this) {
    is Float -> toBigDecimal(DecimalMode.DEFAULT)
    is Double -> toBigDecimal(DecimalMode.DEFAULT)
    is Long -> toBigDecimal()
    is Short -> toBigDecimal()
    is Byte -> toDouble().toBigDecimal(DecimalMode.DEFAULT)
    is Int -> toBigDecimal()
    else -> BigDecimal.fromDouble(toDouble(), DecimalMode.DEFAULT)
}

/**
 * Returns the value of this [BigInteger] as a [BigDecimal].
 */
public fun BigInteger.toBigDecimal(): BigDecimal = toString(10).toBigDecimal(10)


/**
 * Returns the sum of all values produced by [selector] function applied to each element in the sequence.
 *
 * The operation is _terminal_.
 */
public inline fun <T> Sequence<T>.sumBy(selector: (T) -> BigInteger): BigInteger {
    var sum: BigInteger = BigInteger.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the sequence.
 *
 * The operation is _terminal_.
 */
public inline fun <T> Sequence<T>.sumBy(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimal.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
