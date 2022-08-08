package com.bkahlert.kommons

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.SECONDS

/** An instantaneous point on the time-line. */
public expect class Instant

/** A date without a time-zone in the [ISO-8601](https://en.wikipedia.org/wiki/ISO_8601) calendar system, such as 2007-12-03. */
public expect class LocalDate


/** The current date and time. */
public expect val Now: Instant

/** The current date. */
public expect val Today: LocalDate

/** The current date but 1 day in the past. */
public expect val Yesterday: LocalDate

/** The current date but 1 day in the future. */
public expect val Tomorrow: LocalDate

/** The passed seconds since 1970-01-01T00:00:00Z. */
public expect val Timestamp: Long


/** Returns a copy of this [Instant] with the specified [duration] added. */
public expect operator fun Instant.plus(duration: Duration): Instant

/** Returns a copy of this [Instant] with the specified [duration] subtracted. */
public expect operator fun Instant.minus(duration: Duration): Instant

/** Returns the [Duration] between this and the specified [other]. */
public expect operator fun Instant.minus(other: Instant): Duration

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public expect fun Instant.toLocalDateString(): String


/** Returns a copy of this [LocalDate] with the whole days of the specified [duration] added. */
public expect operator fun LocalDate.plus(duration: Duration): LocalDate

/** Returns a copy of this [LocalDate] with the whole days of the specified [duration] subtracted. */
public expect operator fun LocalDate.minus(duration: Duration): LocalDate

/** Returns the [Duration] between this and the specified [other]. */
public expect operator fun LocalDate.minus(other: LocalDate): Duration

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public expect fun LocalDate.toLocalDateString(): String


/** The hour of the month according to universal time. */
public expect val Instant.utcHours: Int

/** The minutes of the month according to universal time. */
public expect val Instant.utcMinutes: Int


private fun Duration.describeMoment(moment: String, descriptive: Boolean): String =
    if (descriptive) {
        if (this > Duration.ZERO) "in $moment"
        else "$moment ago"
    } else {
        if (this > Duration.ZERO) moment
        else "-$moment"
    }

/**
 * Attempts to describe this duration like a human being would do,
 * for example "5m ago" instead of "367.232723s".
 *
 * Set [descriptive] to `false` to turn off the use of "ago" and "in".
 */
public fun Duration.toMomentString(descriptive: Boolean = true): String {
    val abs = absoluteValue
    return when {
        abs < .5.seconds -> "now"
        abs < 1.minutes -> describeMoment(abs.toString(SECONDS), descriptive)
        abs < 1.hours -> describeMoment(abs.toString(MINUTES), descriptive)
        abs < 6.hours -> describeMoment(buildString {
            val durationInHours = abs.inWholeHours.hours
            append(durationInHours.toString(HOURS))
            append(" ")
            append((abs - durationInHours).toString(MINUTES))
        }.removeSuffix(" 0m"), descriptive)
        abs < 23.5.hours -> describeMoment(abs.toString(HOURS), descriptive)
        abs < 1.days -> describeMoment("1d", descriptive)
        abs < 6.days -> describeMoment(buildString {
            val durationInDays = abs.inWholeDays.days
            append(durationInDays.toString(DAYS))
            append(" ")
            append((abs - durationInDays).toString(HOURS))
        }.removeSuffix(" 0h"), descriptive)
        abs < 30.days -> describeMoment(abs.toString(DAYS), descriptive)
        else -> (Now - this).toLocalDateString()
    }
}

/**
 * Attempts to describe this date like a human being would do,
 * for example "28 days ago" instead of "2021-22-23T24-25-26Z".
 *
 * Set [descriptive] to `false` to turn off the use of "ago" and "in".
 */
public fun Instant.toMomentString(descriptive: Boolean = true): String = (this - Now).toMomentString(descriptive)

/**
 * Attempts to describe this date like a human being would do,
 * for example "28 days ago" instead of "2021-22-23".
 *
 * Set [descriptive] to `false` to turn off the use of "ago" and "in".
 */
public fun LocalDate.toMomentString(descriptive: Boolean = true): String =
    when ((this - Today).inWholeDays) {
        -1L -> "yesterday"
        0L -> "today"
        +1L -> "tomorrow"
        else -> (this - Today).toMomentString(descriptive)
    }
