package com.bkahlert.kommons

import kotlin.js.Date
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/** An instantaneous point on the time-line. */
public actual typealias Instant = Date

/** A date without a time-zone in the [ISO-8601](https://en.wikipedia.org/wiki/ISO_8601) calendar system, such as 2007-12-03. */
public actual data class LocalDate(
    /** The year according to local time. */
    public val fullYear: Int,

    /** The month according to local time, where `0` represents January. */
    public val month: Int,

    /** The day of the month according to local time. */
    public val date: Int
) : Comparable<LocalDate> {
    /** This local date as an [Instant]. */
    public val instant: Instant = Date(fullYear, month, date, 0, 0, 0, 0)

    /**
     * The number of milliseconds since
     * [ECMAScript epoch](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Date#the_ecmascript_epoch_and_timestamps).
     */
    public val time: Double get() = instant.time

    override fun compareTo(other: LocalDate): Int = instant.compareTo(other.instant)
    override fun toString(): String {
        val month = instant.month + 1
        val date = instant.date
        return "${instant.fullYear}-${month.toString().padStart(2, '0')}-${date.toString().padStart(2, '0')}"
    }
}


/** The current date and time. */
public actual inline val Now: Date get() = Date()

/** The current date. */
public actual inline val Today: LocalDate get() = Now.run { LocalDate(fullYear, month, date) }

/** The current date but 1 day in the past. */
public actual inline val Yesterday: LocalDate get() = Now.run { LocalDate(fullYear, month, date - 1) }

/** The current date but 1 day in the future. */
public actual inline val Tomorrow: LocalDate get() = Now.run { LocalDate(fullYear, month, date + 1) }

/** The passed seconds since 1970-01-01T00:00:00Z. */
public actual inline val Timestamp: Long get() = Now.time.toLong()


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
public actual inline val Date.utcHours: Int get() = getUTCHours()

/** The minutes of the month according to local time. */
public inline val Date.minutes: Int get() = getMinutes()

/** The minutes of the month according to universal time. */
public actual inline val Date.utcMinutes: Int get() = getUTCMinutes()

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
public actual inline operator fun Date.plus(duration: Duration): Date = duration.toComponents { days, hours, minutes, seconds, nanoseconds ->
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
public actual inline operator fun Date.minus(duration: Duration): Date = Date(getTime().toLong() - duration.inWholeMilliseconds)

/** Returns the [Duration] between this and the specified [other]. */
public actual inline operator fun Date.minus(other: Date): Duration = (getTime().toLong() - other.getTime().toLong()).milliseconds


private val instantFormatterOptions by lazy { dateLocaleOptions { month = "long"; day = "numeric"; year = "numeric" } }

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public actual fun Instant.toLocalDateString(): String = toLocaleDateString(options = instantFormatterOptions)


/** Returns a copy of this [LocalDate] with the whole days of the specified [duration] added. */
public actual operator fun LocalDate.plus(duration: Duration): LocalDate = LocalDate(fullYear, month, date + duration.inWholeDays.toInt())

/** Returns a copy of this [LocalDate] with the specified [duration] subtracted. */
public actual operator fun LocalDate.minus(duration: Duration): LocalDate = LocalDate(fullYear, month, date - duration.inWholeDays.toInt())

/** Returns the [Duration] between this and the specified [other]. */
public actual operator fun LocalDate.minus(other: LocalDate): Duration = (time - other.time).milliseconds

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public actual fun LocalDate.toLocalDateString(): String = instant.toLocalDateString()
