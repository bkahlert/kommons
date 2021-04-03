@file:Suppress("FunctionName", "AddOperatorModifier")

package koodies.math

/** Calculates the positive remainder of dividing this value by the other value. */
public fun Byte.mod(other: Byte): Int = rem(other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
public fun Short.mod(other: Short): Int = rem(other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
public fun Int.mod(other: Int): Int = rem(other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
public fun Long.mod(other: Long): Long = rem(other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
public fun Float.mod(other: Float): Float = rem(other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
public fun Double.mod(other: Double): Double = rem(other).let { if (it < 0) it + other else it }
