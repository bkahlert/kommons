package koodies.time

import koodies.math.BigDecimal
import koodies.math.BigInteger
import koodies.math.isZero
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.SECONDS
import kotlin.time.toDuration

/** Returns a [Duration] representing `this` number of seconds. */
public val Int.seconds: Duration get() = if (isZero) Duration.ZERO else toDuration(SECONDS)

/** Returns a [Duration] representing `this` number of seconds. */
public val Long.seconds: Duration get() = if (isZero) Duration.ZERO else toDuration(SECONDS)

/** Returns a [Duration] representing `this` number of seconds. */
public val Double.seconds: Duration get() = if (isZero) Duration.ZERO else toDuration(SECONDS)

/** Returns a [Duration] representing `this` number of seconds. */
public val BigDecimal.seconds: Duration get() = if (isZero) Duration.ZERO else toDouble().toDuration(SECONDS)

/** Returns a [Duration] representing `this` number of seconds. */
public val BigInteger.seconds: Duration get() = if (isZero) Duration.ZERO else toLong().toDuration(SECONDS)


/** Returns a [Duration] representing `this` number of minutes. */
public val Int.minutes: Duration get() = if (isZero) Duration.ZERO else toDuration(MINUTES)

/** Returns a [Duration] representing `this` number of minutes. */
public val Long.minutes: Duration get() = if (isZero) Duration.ZERO else toDuration(MINUTES)

/** Returns a [Duration] representing `this` number of minutes. */
public val Double.minutes: Duration get() = if (isZero) Duration.ZERO else toDuration(MINUTES)

/** Returns a [Duration] representing `this` number of minutes. */
public val BigDecimal.minutes: Duration get() = if (isZero) Duration.ZERO else toDouble().toDuration(MINUTES)

/** Returns a [Duration] representing `this` number of minutes. */
public val BigInteger.minutes: Duration get() = if (isZero) Duration.ZERO else toDouble().toDuration(MINUTES)


/** Returns a [Duration] representing `this` number of hours. */
public val Int.hours: Duration get() = if (isZero) Duration.ZERO else toDuration(HOURS)

/** Returns a [Duration] representing `this` number of hours. */
public val Long.hours: Duration get() = if (isZero) Duration.ZERO else toDuration(HOURS)

/** Returns a [Duration] representing `this` number of hours. */
public val Double.hours: Duration get() = if (isZero) Duration.ZERO else toDuration(HOURS)

/** Returns a [Duration] representing `this` number of hours. */
public val BigDecimal.hours: Duration get() = if (isZero) Duration.ZERO else toDouble().toDuration(HOURS)

/** Returns a [Duration] representing `this` number of hours. */
public val BigInteger.hours: Duration get() = if (isZero) Duration.ZERO else toLong().toDuration(HOURS)


/** Returns a [Duration] representing `this` number of days. */
public val Int.days: Duration get() = if (isZero) Duration.ZERO else toDuration(DAYS)

/** Returns a [Duration] representing `this` number of days. */
public val Long.days: Duration get() = if (isZero) Duration.ZERO else toDuration(DAYS)

/** Returns a [Duration] representing `this` number of days. */
public val Double.days: Duration get() = if (isZero) Duration.ZERO else toDuration(DAYS)

/** Returns a [Duration] representing `this` number of days. */
public val BigDecimal.days: Duration get() = if (isZero) Duration.ZERO else toDouble().toDuration(DAYS)

/** Returns a [Duration] representing `this` number of days. */
public val BigInteger.days: Duration get() = if (isZero) Duration.ZERO else toLong().toDuration(DAYS)


/**
 * Returns the value of this duration expressed as a [Int] number of milliseconds.
 *
 * The value is coerced to the range of [Int] type, if it doesn't fit in that range, see the conversion [Double.toInt] for details.
 *
 * The range of durations that can be expressed as a `Int` number of milliseconds is approximately ±292 million years.
 */
public fun Duration.toIntMilliseconds(): Int = toLong(DurationUnit.MILLISECONDS).toInt()
