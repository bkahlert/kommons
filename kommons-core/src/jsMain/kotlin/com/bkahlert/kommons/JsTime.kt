package com.bkahlert.kommons

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJSDate
import kotlinx.datetime.toKotlinInstant
import kotlin.js.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * The number of milliseconds since
 * [ECMAScript epoch](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date#the_ecmascript_epoch_and_timestamps).
 */
public inline val Date.time: Double get() = getTime()

/** The difference between this date as evaluated in the universal time zone, and the same date as evaluated in the local time zone. */
public inline val Date.timezoneOffset: Duration get() = getTimezoneOffset().minutes

/** The year according to local time. */
public inline val Date.fullYear: Int get() = getFullYear()

/** The year according to universal time. */
public inline val Date.utcFullYear: Int get() = getUTCFullYear()

/** The month according to local time, where `0` represents January. */
public inline val Date.month: Int get() = getMonth()

/** The month according to universal time, where `0` represents January. */
public inline val Date.utcMonth: Int get() = getUTCMonth()

/** The day of the month according to local time. */
public inline val Date.date: Int get() = getDate()

/** The day of the month according to universal time. */
public inline val Date.utcDate: Int get() = getUTCDate()

/** The day of the week according to local time, where `0` represents Sunday. */
public inline val Date.day: Int get() = getDay()

/** The day of the week according to universal time, where `0` represents Sunday. */
public inline val Date.utcDay: Int get() = getUTCDay()

/** The hour of the month according to local time. */
public inline val Date.hours: Int get() = getHours()

/** The hour of the month according to universal time. */
public inline val Date.utcHours: Int get() = getUTCHours()

/** The minutes of the month according to local time. */
public inline val Date.minutes: Int get() = getMinutes()

/** The minutes of the month according to universal time. */
public inline val Date.utcMinutes: Int get() = getUTCMinutes()

/** The seconds according to local time. */
public inline val Date.seconds: Int get() = getSeconds()

/** The seconds according to universal time. */
public inline val Date.utcSeconds: Int get() = getUTCSeconds()

/** The milliseconds according to local time. */
public inline val Date.milliseconds: Int get() = getMilliseconds()

/** The milliseconds according to universal time. */
public inline val Date.utcMilliseconds: Int get() = getUTCMilliseconds()

/** The current date and time. */
public inline operator fun Date.compareTo(other: Date): Int = time.compareTo(other.time)


/** Returns a copy of this [Date] with the specified [duration] added. */
public inline operator fun Date.plus(duration: Duration): Date = duration.toComponents { days, hours, minutes, seconds, nanoseconds ->
    Date(
        year = this@plus.fullYear,
        month = this@plus.month,
        day = this@plus.date + days.toInt(),
        hour = this@plus.hours + hours,
        minute = this@plus.minutes + minutes,
        second = this@plus.seconds + seconds,
        millisecond = this@plus.milliseconds + (nanoseconds / 1_000_000)
    )
}

/** Returns a copy of this [Date] with the specified [duration] subtracted. */
public inline operator fun Date.minus(duration: Duration): Date = Date(getTime().toLong() - duration.inWholeMilliseconds)

/** Returns the [Duration] between this and the specified [other]. */
public inline operator fun Date.minus(other: Date): Duration = (getTime().toLong() - other.getTime().toLong()).milliseconds


private val dateFormatterOptions by lazy { dateLocaleOptions { month = "long"; day = "numeric"; year = "numeric" } }
private fun LocalDate.toJSDate() = Date(year, monthNumber - 1, dayOfMonth, 0, 0, 0, 0)

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public actual fun Instant.toLocalDateString(): String = toJSDate().toLocaleDateString(options = dateFormatterOptions)

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public actual fun LocalDate.toLocalDateString(): String = toJSDate().toLocaleDateString(options = dateFormatterOptions)

/** Returns this [Date] formatted as a local date (e.g. May 15, 1984). */
public fun Date.toLocalDateString(): String = toLocaleDateString(options = dateFormatterOptions)


/**
 * Attempts to describe this date like a human being would do,
 * for example "28 days ago" instead of "2021-22-23T24-25-26Z".
 *
 * Set [descriptive] to `false` to turn off the use of "ago" and "in".
 */
public fun Date.toMomentString(descriptive: Boolean = true): String = toKotlinInstant().toMomentString(descriptive)
