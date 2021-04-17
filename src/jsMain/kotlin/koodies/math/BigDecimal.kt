package koodies.math

/**
 * Immutable, arbitrary-precision signed decimal numbers.
 */
public actual class BigDecimal : Number(), Comparable<BigDecimal> {
    override fun compareTo(other: BigDecimal): Int {
        TODO("Not yet implemented")
    }

    override fun toByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun toChar(): Char {
        TODO("Not yet implemented")
    }

    override fun toDouble(): Double {
        TODO("Not yet implemented")
    }

    override fun toFloat(): Float {
        TODO("Not yet implemented")
    }

    override fun toInt(): Int {
        TODO("Not yet implemented")
    }

    override fun toLong(): Long {
        TODO("Not yet implemented")
    }

    override fun toShort(): Short {
        TODO("Not yet implemented")
    }
}


/**
 * Enables the use of the `+` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.plus(other: BigDecimal): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns `this` `+` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.plus(other: BigDecimal, precision: Int, roundingMode: RoundingMode): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Enables the use of the `-` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.minus(other: BigDecimal): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns `this` `-` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.minus(    other: BigDecimal,    precision: Int,    roundingMode: RoundingMode,): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Enables the use of the `*` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.times(other: BigDecimal): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns `this` `*` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.times(    other: BigDecimal,    precision: Int,    roundingMode: RoundingMode,): BigDecimal {
    TODO("Not yet implemented")
}
/**
 * Enables the use of the `/` operator for [BigDecimal] instances.
 *
 * The scale of the result is the same as the scale of `this` (divident), and for rounding the [RoundingMode.HALF_EVEN]
 * rounding mode is used.
 */
public actual operator fun BigDecimal.div(other: BigDecimal): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(    other: BigDecimal,    precision: Int,    roundingMode: RoundingMode,): BigDecimal {
    TODO("Not yet implemented")
}
/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Long): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(    other: Long,    precision: Int,    roundingMode: RoundingMode,): BigDecimal {
    TODO("Not yet implemented")
}

/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Float): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(    other: Float,    precision: Int,    roundingMode: RoundingMode,): BigDecimal {
    TODO("Not yet implemented")
}
/** Divides this value by the other value. */
public actual operator fun BigDecimal.div(other: Double): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns `this` `/` [other] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.div(    other: Double,    precision: Int,    roundingMode: RoundingMode,): BigDecimal {
    TODO("Not yet implemented")
}
/**
 * Enables the use of the `%` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.rem(other: BigDecimal): BigDecimal {
    TODO("Not yet implemented")
}

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Int): BigDecimal {
    TODO("Not yet implemented")
}

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Long): BigDecimal {
    TODO("Not yet implemented")
}

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Float): BigDecimal {
    TODO("Not yet implemented")
}

/** Calculates the remainder of dividing this value by the other value. */
public actual operator fun BigDecimal.rem(other: Double): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Enables the use of the unary `-` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.unaryMinus(): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Enables the use of the unary `++` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.inc(): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Enables the use of the unary `--` operator for [BigDecimal] instances.
 */
public actual operator fun BigDecimal.dec(): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns the value of this [Int] number as a [BigDecimal].
 */
public actual fun Int.toBigDecimal(): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns the value of this [UInt] number as a [BigDecimal].
 */
public actual fun UInt.toBigDecimal(): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns the value of this [Double] number as a [BigDecimal].
 *
 * The number is converted to a string and then the string is converted to a [BigDecimal].
 */
public actual fun Double.toBigDecimal(): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns the value of this [BigInteger] number as a [BigDecimal].
 */
public actual fun BigInteger.toBigDecimal(): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Returns the value of this [CharSequence] representing a number
 * to the given [radix] as a [BigDecimal].
 */
public actual fun CharSequence.toBigDecimal(radix: Int): BigDecimal {
    TODO("Not yet implemented")
}

public actual object BigDecimalConstants {
    /**
     * The BigDecimal constant zero.
     */
    public actual val ZERO: BigDecimal
        get() = TODO("Not yet implemented")

    /**
     * The BigDecimal constant one.
     */
    public actual val ONE: BigDecimal
        get() = TODO("Not yet implemented")

    /**
     * The BigDecimal constant two.
     */
    public actual val TWO: BigDecimal
        get() = TODO("Not yet implemented")

    /**
     * The BigDecimal constant ten.
     */
    public actual val TEN: BigDecimal
        get() = TODO("Not yet implemented")

    /**
     * The BigDecimal constant ten.
     */
    public actual val HUNDRED: BigDecimal
        get() = TODO("Not yet implemented")
}

/**
 * Returns the absolute value of this value.
 */
public actual val BigDecimal.absoluteValue: BigDecimal
    get() {
        TODO("Not yet implemented")
    }

/**
 * Raises this value to the power [n].
 */
public actual fun BigDecimal.pow(n: Int): BigDecimal {
    TODO("Not yet implemented")
}

/**
 * Raises this value to the power [n] with the provided
 * [precision] (number of digits to be used) and
 * [roundingMode] (specifies the algorithm to be used for rounding).
 */
public actual fun BigDecimal.pow(n: Int, precision: Int, roundingMode: RoundingMode): BigDecimal {
    TODO("Not yet implemented")
}

public actual val BigDecimal.scale: Int
    get() = TODO("Not yet implemented")

public actual fun BigDecimal.scale(scale: Int, roundingMode: RoundingMode): BigDecimal {
    TODO("Not yet implemented")
}
public actual val BigDecimal.precision: Int
    get() = TODO("Not yet implemented")
public actual fun BigDecimal.toScientificString(): String {
    TODO("Not yet implemented")
}

public actual fun BigDecimal.toExactDecimalsString(decimals: Int): String {
    TODO("Not yet implemented")
}

public actual fun BigDecimal.toAtMostDecimalsString(decimals: Int): String {
    TODO("Not yet implemented")
}

public actual fun Double.toScientificString(): String {
    TODO("Not yet implemented")
}

public actual fun Double.toExactDecimalsString(decimals: Int): String {
    TODO("Not yet implemented")
}

public actual fun Double.toAtMostDecimalsString(decimals: Int): String {
    TODO("Not yet implemented")
}
