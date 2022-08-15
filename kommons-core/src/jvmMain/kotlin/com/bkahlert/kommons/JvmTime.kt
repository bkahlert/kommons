package com.bkahlert.kommons

import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

/** An instantaneous point on the time-line. */
public actual typealias Instant = java.time.Instant

/** A date without a time-zone in the [ISO-8601](https://en.wikipedia.org/wiki/ISO_8601) calendar system, such as 2007-12-03. */
public actual typealias LocalDate = java.time.LocalDate


/** The current date and time in Coordinated Universal Time (UTC). */
public actual inline val Now: Instant get() = Instant.now()

/** The current local date. */
public actual inline val Today: LocalDate get() = LocalDate.now()

/** The current local date but 1 day in the past. */
public actual inline val Yesterday: LocalDate get() = LocalDate.now().minusDays(1)

/** The current local date but 1 day in the future. */
public actual inline val Tomorrow: LocalDate get() = LocalDate.now().plusDays(1)

/** The passed seconds since 1970-01-01T00:00:00Z. */
public actual inline val Timestamp: Long get() = Instant.now().toEpochMilli()


@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [Instant] with the specified [duration] added. */
public actual inline operator fun Instant.plus(duration: Duration): Instant = this.plus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [Instant] with the specified [duration] subtracted. */
public actual inline operator fun Instant.minus(duration: Duration): Instant = this.minus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns the [Duration] between this and the specified [other]. */
public actual inline operator fun Instant.minus(other: Instant): Duration = (toEpochMilli() - other.toEpochMilli()).milliseconds


private val instantFormatter by lazy { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withZone(ZoneId.systemDefault()) }

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public actual fun Instant.toLocalDateString(): String {
    return instantFormatter.format(this)
}


@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [LocalDate] with the whole days of the specified [duration] added. */
public actual inline operator fun LocalDate.plus(duration: Duration): LocalDate = this.plusDays(duration.inWholeDays)

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [LocalDate] with the whole days of the specified [duration] subtracted. */
public actual inline operator fun LocalDate.minus(duration: Duration): LocalDate = this.minusDays(duration.inWholeDays)

@Suppress("NOTHING_TO_INLINE")
/** Returns the [Duration] between this and the specified [other]. */
public actual inline operator fun LocalDate.minus(other: LocalDate): Duration = (toEpochDay() - other.toEpochDay()).days


private val localDateFormatter by lazy { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public actual fun LocalDate.toLocalDateString(): String {
    return localDateFormatter.format(this)
}


@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [LocalTime] with the specified [duration] added. */
public inline operator fun LocalTime.plus(duration: Duration): LocalTime = this.plus(duration.toJavaDuration())


@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [LocalTime] with the specified [duration] subtracted. */
public inline operator fun LocalTime.minus(duration: Duration): LocalTime = this.minus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [LocalDateTime] with the specified [duration] added. */
public inline operator fun LocalDateTime.plus(duration: Duration): LocalDateTime = this.plus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [LocalDateTime] with the specified [duration] subtracted. */
public inline operator fun LocalDateTime.minus(duration: Duration): LocalDateTime = this.minus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [ZonedDateTime] with the specified [duration] added. */
public inline operator fun ZonedDateTime.plus(duration: Duration): ZonedDateTime = this.plus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [ZonedDateTime] with the specified [duration] subtracted. */
public inline operator fun ZonedDateTime.minus(duration: Duration): ZonedDateTime = this.minus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [OffsetDateTime] with the specified [duration] added. */
public inline operator fun OffsetDateTime.plus(duration: Duration): OffsetDateTime = this.plus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [OffsetDateTime] with the specified [duration] subtracted. */
public inline operator fun OffsetDateTime.minus(duration: Duration): OffsetDateTime = this.minus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [OffsetTime] with the specified [duration] added. */
public inline operator fun OffsetTime.plus(duration: Duration): OffsetTime = this.plus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [OffsetTime] with the specified [duration] subtracted. */
public inline operator fun OffsetTime.minus(duration: Duration): OffsetTime = this.minus(duration.toJavaDuration())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [FileTime] with the specified [duration] added. */
public inline operator fun FileTime.plus(duration: Duration): FileTime = FileTime.from(toInstant().plus(duration))


@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [FileTime] with the specified [duration] subtracted. */
public inline operator fun FileTime.minus(duration: Duration): FileTime = FileTime.from(toInstant().minus(duration))

@Suppress("NOTHING_TO_INLINE")
/** Returns a [FileTime] representing the same point of time value on the time-line as this instant. */
public fun Instant.toFileTime(): FileTime = FileTime.from(this)

/** The hour of the month according to universal time. */
public actual val Instant.utcHours: Int get() = atZone(ZoneOffset.UTC).hour

/** The minutes of the month according to universal time. */
public actual val Instant.utcMinutes: Int get() = atZone(ZoneOffset.UTC).minute
