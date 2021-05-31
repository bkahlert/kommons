package koodies.math

import kotlin.math.ceil
import kotlin.math.floor

/**
 * Divides `this` dividend by the given [divisor]
 * and in case the result is no integer, applies [ceil] to it.
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun Int.ceilDiv(divisor: Int): Int =
    -(-this floorDiv divisor)

/**
 * Divides `this` dividend by the given [divisor]
 * and in case the result is no integer, applies [floor] to it.
 */
@Suppress("NOTHING_TO_INLINE")
public inline infix fun Int.floorDiv(divisor: Int): Int =
    (this / divisor).let {
        if (this xor divisor < 0 && it * divisor != this) it - 1 else it
    }
