package com.bkahlert.kommons

import com.bkahlert.kommons.ValueRange.Normalized
import com.bkahlert.kommons.ValueRange.Scaling
import kotlin.math.roundToInt
import kotlin.random.Random

/** Common value ranges, such as [ValueRange.Normalized]. */
public sealed class ValueRange<T : Comparable<T>>(
    override inline val start: T,
    override inline val endInclusive: T,
) : ClosedRange<T> {
    /** Synonym for [start] */
    public inline val min: T get() = start

    /** Synonym for [endInclusive] */
    public inline val max: T get() = endInclusive

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
        /** Value that signifies no scaling */
        public const val None: Double = 0.0
    }
}

/**
 * Returns this value with the specified [sourceRange] (default: `0.0..1.0`) mapped to the specified [destinationRange].
 *
 * @throws IllegalArgumentException if this value isn't in the specified [sourceRange]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.map(
    destinationRange: ClosedRange<Double>,
    sourceRange: ClosedRange<Double> = Normalized,
): Double {
    require(this in sourceRange) { "$this must be in $sourceRange" }
    val destinationDiff = destinationRange.endInclusive - destinationRange.start
    val sourceDiff = sourceRange.endInclusive - sourceRange.start
    return destinationRange.start + (destinationDiff * (this - sourceRange.start)) / sourceDiff
}

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`) and [sourceRangeEndInclusive] (default: `1.0`)
 * mapped to the [ClosedRange] starting with the specified [destinationRangeStart] (default: `0.0`) and ending with the
 * specified [destinationRangeEndInclusive].
 *
 * @throws IllegalArgumentException if this value isn't in the [ClosedRange]
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
 * @throws IllegalArgumentException if this value isn't in the specified [sourceRange]
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
 * @throws IllegalArgumentException if this value isn't in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.map(
    destinationRangeEndInclusive: Int,
    destinationRangeStart: Int = Normalized.min.toInt(),
    sourceRangeStart: Double = Normalized.min,
    sourceRangeEndInclusive: Double = Normalized.max,
): Int = map(destinationRangeStart..destinationRangeEndInclusive, sourceRangeStart..sourceRangeEndInclusive)

/**
 * Returns this value with the specified [sourceRange]
 * mapped to the [ClosedRange] `0.0..1.0`.
 *
 * @throws IllegalArgumentException if this value isn't in the specified [sourceRange]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.normalize(
    sourceRange: ClosedRange<Double>,
): Double = map(Normalized.max, Normalized.min, sourceRange.start, sourceRange.endInclusive)

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`) and [sourceRangeEndInclusive]
 * mapped to the [ClosedRange] `0.0..1.0`.
 *
 * @throws IllegalArgumentException if this value isn't in the [ClosedRange]
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
 * @throws IllegalArgumentException if this value isn't in the specified [sourceRange]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Int.normalize(
    sourceRange: ClosedRange<Int>,
): Double = toDouble().map(
    destinationRangeStart = Normalized.min,
    destinationRangeEndInclusive = Normalized.max,
    sourceRangeStart = sourceRange.start.toDouble(),
    sourceRangeEndInclusive = sourceRange.endInclusive.toDouble(),
)

/**
 * Returns this value with the specified [sourceRangeStart] (default: `0.0`) and [sourceRangeEndInclusive]
 * mapped to the [ClosedRange] `0.0..1.0`.
 *
 * @throws IllegalArgumentException if this value isn't in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Int.normalize(
    sourceRangeEndInclusive: Int,
    sourceRangeStart: Int = Normalized.min.toInt(),
): Double = toDouble().map(
    destinationRangeStart = Normalized.min,
    destinationRangeEndInclusive = Normalized.max,
    sourceRangeStart = sourceRangeStart.toDouble(),
    sourceRangeEndInclusive = sourceRangeEndInclusive.toDouble(),
)

/**
 * Returns this value with the specified [sourceRange] (default: `0.0..1.0`)
 * closer to its maximum value for [amount] in range `(0.0..1.0]`
 * respective closer to its minimum value for [amount] in range `[-1.0..0.0)`.
 *
 * @throws IllegalArgumentException if this value isn't in the specified [sourceRange] or [amount] isn't in `[-1.0..+1.0]`
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
 * closer to its maximum value [sourceRangeEndInclusive] for [amount] in range `(0.0..1.0]`
 * respective closer to its minimum value [sourceRangeStart] for [amount] in range `[-1.0..0.0)`.
 *
 * @throws IllegalArgumentException if this value isn't in the [ClosedRange]
 * starting with the specified [sourceRangeStart] and
 * ending with the specified [sourceRangeEndInclusive] or [amount] isn't in `[-1.0..+1.0]`
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Double.scale(
    amount: Double,
    sourceRangeEndInclusive: Double = Normalized.max,
    sourceRangeStart: Double = Normalized.min,
): Double = scale(amount, sourceRangeStart..sourceRangeEndInclusive)


// end -----------------------------------------------------------------------------------------------------------------

/**
 * The exclusive end of this range.
 *
 * @throws IllegalStateException if [ClosedRange.endInclusive] is equal to [Int.MAX_VALUE]
 */
public inline val ClosedRange<Int>.end: Int
    get() {
        check(endInclusive != Int.MAX_VALUE) { "The exclusive end of $this is greater than Int.MAX_VALUE." }
        return endInclusive + 1
    }


// random --------------------------------------------------------------------------------------------------------------

/**
 * Returns a random element from this range,
 * or throws a [NoSuchElementException] if this range is empty.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun ClosedRange<Double>.random(): Double = random(Random)

/**
 * Returns a random element from this range using the specified source of randomness,
 * or throws a [NoSuchElementException] if this range is empty.
 */
public fun ClosedRange<Double>.random(random: Random): Double =
    if (isEmpty()) throw NoSuchElementException("Cannot get random in empty range: $this")
    else random.nextDouble(start, endInclusive)

/**
 * Returns a random element from this range,
 * or `null` if this range is empty.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun ClosedRange<Double>.randomOrNull(): Double? = randomOrNull(Random)

/**
 * Returns a random element from this range using the specified source of randomness,
 * or `null` if this range is empty.
 */
public fun ClosedRange<Double>.randomOrNull(random: Random): Double? =
    if (isEmpty()) null else random.nextDouble(start, endInclusive)


// asIterable ----------------------------------------------------------------------------------------------------------

/**
 * Creates an [Iterable] instance that wraps the original range returning
 * its elements when being iterated.
 *
 * Unless empty, the first element returned is [ClosedRange.start].
 * The remaining elements are computed by applying the specified [step] to
 * the most recently returned element.
 *
 * The iteration ends when the next element is no longer contained in this range.
 */
public fun <T : Comparable<T>> ClosedRange<T>.asIterable(step: (T) -> T): Iterable<T> =
    Iterable {
        var next: T = start
        iterator {
            while (contains(next)) {
                yield(next)
                next = step(next)
            }
        }
    }
