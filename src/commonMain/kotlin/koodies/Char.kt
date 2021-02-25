package koodies

val toInt = Char.MIN_SURROGATE.toInt()
val toInt1 = Char.MAX_SURROGATE.toInt()
val intRange = toInt..toInt1

/**
 * The minimum value of a Unicode surrogate code unit.
 */
val Char.Companion.MIN_SURROGATE_CODE_POINT: Int get() = MIN_SURROGATE.toInt()

/**
 * The maximum value of a Unicode surrogate code unit.
 */
val Char.Companion.MAX_SURROGATE_CODE_POINT: Int get() = MAX_SURROGATE.toInt()

/**
 * The range of Unicode surrogate code units.
 */
val Char.Companion.SURROGATE_CODE_POINT_RANGE: IntRange get() = MIN_SURROGATE_CODE_POINT..MAX_SURROGATE_CODE_POINT


/**
 * The minimum value of a Unicode code unit that can be represented by a single [Char].
 */
val Char.Companion.MIN_CODE_POINT: Int get() = MIN_VALUE.toInt()

/**
 * The maximum value of a Unicode code unit that can be represented by a single [Char].
 */
val Char.Companion.MAX_CODE_POINT: Int get() = MAX_VALUE.toInt()

/**
 * The range of Unicode code units that can be represented by a single [Char].
 */
val Char.Companion.CODE_POINT_RANGE: IntRange get() = MIN_CODE_POINT..MAX_CODE_POINT
