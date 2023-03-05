package com.bkahlert.kommons.time

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinInstant
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val instantFormatter by lazy { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withZone(ZoneId.systemDefault()) }

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public actual fun Instant.toLocalDateString(): String {
    return instantFormatter.format(toJavaInstant())
}


private val localDateFormatter by lazy { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public actual fun LocalDate.toLocalDateString(): String {
    return localDateFormatter.format(toJavaLocalDate())
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
public inline operator fun FileTime.plus(duration: Duration): FileTime = FileTime.from(toInstant().toKotlinInstant().plus(duration).toJavaInstant())

@Suppress("NOTHING_TO_INLINE")
/** Returns a copy of this [FileTime] with the specified [duration] subtracted. */
public inline operator fun FileTime.minus(duration: Duration): FileTime = FileTime.from(toInstant().toKotlinInstant().minus(duration).toJavaInstant())

@Suppress("NOTHING_TO_INLINE")
/** Returns a [FileTime] representing the same point of time value on the time-line as this instant. */
public inline fun Instant.toFileTime(): FileTime = FileTime.from(toJavaInstant())
