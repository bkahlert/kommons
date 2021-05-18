package koodies.unit

import koodies.math.BigDecimal
import koodies.math.BigInteger
import koodies.math.isZero
import kotlin.time.Duration

/** Returns a [Duration] representing `this` number of seconds. */
public val Int.seconds: Duration get() = if (isZero) Duration.ZERO else Duration.seconds(this)

/** Returns a [Duration] representing `this` number of seconds. */
public val Long.seconds: Duration get() = if (isZero) Duration.ZERO else Duration.seconds(this)

/** Returns a [Duration] representing `this` number of seconds. */
public val Double.seconds: Duration get() = if (isZero) Duration.ZERO else Duration.seconds(this)

/** Returns a [Duration] representing `this` number of seconds. */
public val BigDecimal.seconds: Duration get() = if (isZero) Duration.ZERO else Duration.seconds(toDouble())

/** Returns a [Duration] representing `this` number of seconds. */
public val BigInteger.seconds: Duration get() = if (isZero) Duration.ZERO else Duration.seconds(toLong())


/** Returns a [Duration] representing `this` number of minutes. */
public val Int.minutes: Duration get() = if (isZero) Duration.ZERO else Duration.minutes(this)

/** Returns a [Duration] representing `this` number of minutes. */
public val Long.minutes: Duration get() = if (isZero) Duration.ZERO else Duration.minutes(this)

/** Returns a [Duration] representing `this` number of minutes. */
public val Double.minutes: Duration get() = if (isZero) Duration.ZERO else Duration.minutes(this)

/** Returns a [Duration] representing `this` number of minutes. */
public val BigDecimal.minutes: Duration get() = if (isZero) Duration.ZERO else Duration.minutes(toDouble())

/** Returns a [Duration] representing `this` number of minutes. */
public val BigInteger.minutes: Duration get() = if (isZero) Duration.ZERO else Duration.minutes(toDouble())


/** Returns a [Duration] representing `this` number of hours. */
public val Int.hours: Duration get() = if (isZero) Duration.ZERO else Duration.hours(this)

/** Returns a [Duration] representing `this` number of hours. */
public val Long.hours: Duration get() = if (isZero) Duration.ZERO else Duration.hours(this)

/** Returns a [Duration] representing `this` number of hours. */
public val Double.hours: Duration get() = if (isZero) Duration.ZERO else Duration.hours(this)

/** Returns a [Duration] representing `this` number of hours. */
public val BigDecimal.hours: Duration get() = if (isZero) Duration.ZERO else Duration.hours(toDouble())

/** Returns a [Duration] representing `this` number of hours. */
public val BigInteger.hours: Duration get() = if (isZero) Duration.ZERO else Duration.hours(toLong())


/** Returns a [Duration] representing `this` number of days. */
public val Int.days: Duration get() = if (isZero) Duration.ZERO else Duration.days(this)

/** Returns a [Duration] representing `this` number of days. */
public val Long.days: Duration get() = if (isZero) Duration.ZERO else Duration.days(this)

/** Returns a [Duration] representing `this` number of days. */
public val Double.days: Duration get() = if (isZero) Duration.ZERO else Duration.days(this)

/** Returns a [Duration] representing `this` number of days. */
public val BigDecimal.days: Duration get() = if (isZero) Duration.ZERO else Duration.days(toDouble())

/** Returns a [Duration] representing `this` number of days. */
public val BigInteger.days: Duration get() = if (isZero) Duration.ZERO else Duration.days(toLong())
