package koodies.math

/**
 * Returns `true` if this numbers equals `0`.
 */
public val Number.isZero: Boolean get() = toDouble() == 0.0

/**
 * Returns `true` if this numbers equals `0`.
 */
public val UByte.isZero: Boolean get() = toDouble() == 0.0

/**
 * Returns `true` if this numbers equals `0`.
 */
public val UShort.isZero: Boolean get() = toDouble() == 0.0

/**
 * Returns `true` if this numbers equals `0`.
 */
public val UInt.isZero: Boolean get() = toDouble() == 0.0

/**
 * Returns `true` if this numbers equals `0`.
 */
public val ULong.isZero: Boolean get() = toDouble() == 0.0
