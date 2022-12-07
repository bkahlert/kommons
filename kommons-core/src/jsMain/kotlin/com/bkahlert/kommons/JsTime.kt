package com.bkahlert.kommons

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJSDate
import kotlin.js.Date


private val instantFormatterOptions by lazy { dateLocaleOptions { month = "long"; day = "numeric"; year = "numeric" } }

/** Returns this [Instant] formatted as a local date (e.g. May 15, 1984). */
public actual fun Instant.toLocalDateString(): String = toJSDate().toLocaleDateString(options = instantFormatterOptions)

/** Returns this [LocalDate] formatted as a local date (e.g. May 15, 1984). */
public actual fun LocalDate.toLocalDateString(): String = toJSDate().toLocaleDateString(options = instantFormatterOptions)

private fun LocalDate.toJSDate() = Date(year, monthNumber - 1, dayOfMonth, 0, 0, 0, 0)
