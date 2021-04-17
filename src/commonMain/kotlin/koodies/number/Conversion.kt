package koodies.number

import koodies.math.BigDecimal
import koodies.math.BigDecimalConstants
import koodies.math.BigInteger
import koodies.math.BigIntegerConstants
import koodies.math.plus

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the sequence.
 *
 * The operation is _terminal_.
 */
@Deprecated("no more used and untested")
public inline fun <T> Sequence<T>.sumBy(selector: (T) -> BigInteger): BigInteger {
    var sum: BigInteger = BigIntegerConstants.ZERO
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
@Deprecated("no more used and untested")
public inline fun <T> Sequence<T>.sumBy(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = BigDecimalConstants.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
