package com.bkahlert.kommons.math

import com.bkahlert.kommons.math.ValueRange.Normalized
import com.bkahlert.kommons.math.ValueRange.Scaling
import com.bkahlert.kommons.ranges.difference
import kotlin.math.roundToInt

/**
 * Common value ranges, such as [ValueRange.Normalized].
 */
public sealed class ValueRange<T : Comparable<T>>(
    override inline val start: T,
    override inline val endInclusive: T,
) : ClosedRange<T> {
    /** Synonym for [start] */
    public inline val Minimum: T get() = start

    /** Synonym for [endInclusive] */
    public inline val Maximum: T get() = endInclusive

    /** Values with allowed values `0.0..1.0` */
    public object Normalized : ValueRange<Double>(0.0, 1.0)

    /** Values with allowed values `0..255` */
    public object Bytes : ValueRange<Int>(0, 255)

    /** Values with allowed values `0.0..360.0`° */
    public object Angle : ValueRange<Double>(0.0, 360.0)

    /** Values with allowed values `0.0..100.0`% */
    public object Percent : ValueRange<Double>(0.0, 100.0)

    /** Values with allowed values `-1.0..+1.0` */
    public object Scaling : ValueRange<Double>(-1.0, +1.0) {
        public inline val None: Double get() = 0.0
    }
}

/**
 * Returns this value with the specified [sourceRange] (default: `0.0..1.0`) mapped to the specified [destinationRange].
 *
 * @throws IllegalArgumentException if this value is not in the specified [sourceRange]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.map(
    destinationRange: ClosedRange<Double>,
    sourceRange: ClosedRange<Double> = Normalized,
): Double {
    require(this in sourceRange) { "$this must be in $sourceRange" }
    return destinationRange.start + (destinationRange.difference * (this - sourceRange.start)) / sourceRange.difference
}

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`) and [sourceRangeEndInclusive] (default: `1.0`)
 * mapped to the [ClosedRange] starting with the specified [destinationRangeStart] (default: `0.0`) and ending with the
 * specified [destinationRangeEndInclusive].
 *
 * @throws IllegalArgumentException if this value is not in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.map(
    destinationRangeEndInclusive: Double,
    destinationRangeStart: Double = Normalized.start,
    sourceRangeStart: Double = Normalized.start,
    sourceRangeEndInclusive: Double = Normalized.endInclusive,
): Double = map(destinationRangeStart..destinationRangeEndInclusive, sourceRangeStart..sourceRangeEndInclusive)

/**
 * Returns this value with the specified [sourceRange] (default: `0.0..1.0`) mapped to the specified [destinationRange].
 *
 * @throws IllegalArgumentException if this value is not in the specified [sourceRange]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.map(
    destinationRange: ClosedRange<Int>,
    sourceRange: ClosedRange<Double> = Normalized,
): Int = map(destinationRange.start.toDouble()..destinationRange.endInclusive.toDouble(), sourceRange).roundToInt()

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`) and [sourceRangeEndInclusive] (default: `1.0`)
 * mapped to the [ClosedRange] starting with the specified [destinationRangeStart] (default: `0.0`) and ending with the
 * specified [destinationRangeEndInclusive].
 *
 * @throws IllegalArgumentException if this value is not in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.map(
    destinationRangeEndInclusive: Int,
    destinationRangeStart: Int = Normalized.Minimum.toInt(),
    sourceRangeStart: Double = Normalized.Minimum,
    sourceRangeEndInclusive: Double = Normalized.Maximum,
): Int = map(destinationRangeStart..destinationRangeEndInclusive, sourceRangeStart..sourceRangeEndInclusive)

/**
 * Returns this value with the specified [sourceRange]
 * mapped to the [ClosedRange] `0.0..1.0`.
 *
 * @throws IllegalArgumentException if this value is not in the specified [sourceRange]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.normalize(
    sourceRange: ClosedRange<Double>,
): Double = map(Normalized.Maximum, Normalized.Minimum, sourceRange.start, sourceRange.endInclusive)

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`) and [sourceRangeEndInclusive]
 * mapped to the [ClosedRange] `0.0..1.0`.
 *
 * @throws IllegalArgumentException if this value is not in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.normalize(
    sourceRangeEndInclusive: Double,
    sourceRangeStart: Double = Normalized.start,
): Double = map(Normalized, sourceRangeStart..sourceRangeEndInclusive)

/**
 * Returns this value with the specified [sourceRange]
 * mapped to the [ClosedRange] `0.0..1.0`.
 *
 * @throws IllegalArgumentException if this value is not in the specified [sourceRange]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Int.normalize(
    sourceRange: ClosedRange<Int>,
): Double = toDouble().map(
    destinationRangeStart = Normalized.Minimum,
    destinationRangeEndInclusive = Normalized.Maximum,
    sourceRangeStart = sourceRange.start.toDouble(),
    sourceRangeEndInclusive = sourceRange.endInclusive.toDouble(),
)

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`) and [sourceRangeEndInclusive]
 * mapped to the [ClosedRange] `0.0..1.0`.
 *
 * @throws IllegalArgumentException if this value is not in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Int.normalize(
    sourceRangeEndInclusive: Int,
    sourceRangeStart: Int = Normalized.Minimum.toInt(),
): Double = toDouble().map(
    destinationRangeStart = Normalized.Minimum,
    destinationRangeEndInclusive = Normalized.Maximum,
    sourceRangeStart = sourceRangeStart.toDouble(),
    sourceRangeEndInclusive = sourceRangeEndInclusive.toDouble(),
)

/**
 * Returns this value with the specified [sourceRange] (default: `0.0..1.0`)
 * closer to its maximum value (for [amount] in range `(0.0..1.0]`)
 * respective closer to its minimum value (for [amount] in range `[-1.0..0.0)`).
 *
 * @throws IllegalArgumentException if this value is not in the specified [sourceRange] or [amount] is not in `[-1.0..+1.0]`
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.scale(
    amount: Double,
    sourceRange: ClosedRange<Double> = Normalized,
): Double {
    require(this in sourceRange) { "$this must be in $sourceRange" }
    require(amount in Scaling) { "$this must be in $Scaling" }
    return this + amount * (if (amount > 0.0) sourceRange.endInclusive - this else this - sourceRange.start)
}

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`)
 * and [sourceRangeEndInclusive] (default: `1.0`)
 * closer to its maximum value [sourceRangeEndInclusive] (for [amount] in range `(0.0..1.0]`)
 * respective closer to its minimum value [sourceRangeStart] (for [amount] in range `[-1.0..0.0)`).
 *
 * @throws IllegalArgumentException if this value is not in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive] or [amount] is not in `[-1.0..+1.0]`
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.scale(
    amount: Double,
    sourceRangeEndInclusive: Double = Normalized.Maximum,
    sourceRangeStart: Double = Normalized.Minimum,
): Double = scale(amount, sourceRangeStart..sourceRangeEndInclusive)
