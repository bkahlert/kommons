package com.bkahlert.kommons

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.DurationUnit.SECONDS

/**
 * Returns a [Clock] with its [Clock.now]
 * using the specified [now].
 */
public operator fun Clock.Companion.invoke(now: () -> Instant): Clock = object : Clock {
    override fun now(): Instant = now()
}

/** The current date and time. */
public val Now: Instant get() = Clock.System.now()

/** The current date. */
public val Today: LocalDate get() = Clock.System.todayIn(TimeZone.currentSystemDefault())

/** The current date but 1 day in the past. */
public val Yesterday: LocalDate get() = Today - 1.days

/** The current date but 1 day in the future. */
public val Tomorrow: LocalDate get() = Today + 1.days

/** The passed seconds since 1970-01-01T00:00:00Z. */
public val Timestamp: Long get() = Now.toEpochMilliseconds()

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public expect fun Instant.toLocalDateString(): String


/** Returns a copy of this [LocalDate] with the whole days of the specified [duration] added. */
public operator fun LocalDate.plus(duration: Duration): LocalDate = plus(duration.inWholeDays, DateTimeUnit.DAY)

/** Returns a copy of this [LocalDate] with the whole days of the specified [duration] subtracted. */
public operator fun LocalDate.minus(duration: Duration): LocalDate = plus(-duration)

/** Returns the [Duration] between this and the specified [other]. */
public operator fun LocalDate.minus(other: LocalDate): Duration = other.daysUntil(this).days

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public expect fun LocalDate.toLocalDateString(): String


/** The hour of the month according to universal time. */
public val Instant.utcHours: Int get() = toLocalDateTime(FixedOffsetTimeZone(UtcOffset.ZERO)).hour

/** The minutes of the month according to universal time. */
public val Instant.utcMinutes: Int get() = toLocalDateTime(FixedOffsetTimeZone(UtcOffset.ZERO)).minute


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
