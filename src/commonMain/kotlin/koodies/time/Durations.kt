package koodies.time

import kotlin.time.Duration
import kotlin.time.DurationUnit


/**
 * Returns the value of this duration expressed as a [Int] number of milliseconds.
 *
 * The value is coerced to the range of [Int] type, if it doesn't fit in that range, see the conversion [Double.toInt] for details.
 *
 * The range of durations that can be expressed as a `Int` number of milliseconds is approximately ±292 million years.
 */
public fun Duration.toIntMilliseconds(): Int = toLong(DurationUnit.MILLISECONDS).toInt()
