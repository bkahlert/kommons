@file:Suppress("FunctionName", "AddOperatorModifier")

package koodies.number

/** Calculates the positive remainder of dividing this value by the other value. */
fun Byte.mod(other: Byte): Int = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
fun Short.mod(other: Short): Int = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
fun Int.mod(other: Int): Int = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
fun Long.mod(other: Long): Long = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
fun Float.mod(other: Float): Float = (this % other).let { if (it < 0) it + other else it }

/** Calculates the positive remainder of dividing this value by the other value. */
fun Double.mod(other: Double): Double = (this % other).let { if (it < 0) it + other else it }
