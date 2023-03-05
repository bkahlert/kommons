package com.bkahlert.kommons.time

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public actual fun Instant.toLocalDateString(): String = toLocalDateTime(TimeZone.currentSystemDefault()).date.toLocalDateString()

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public actual fun LocalDate.toLocalDateString(): String = buildString {
    append(month.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
    append(" ")
    append(dayOfMonth)
    append(", ")
    append(year)
}
