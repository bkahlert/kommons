package koodies.time

import java.nio.file.attribute.FileTime
import java.time.LocalDate
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.temporal.Temporal
import kotlin.time.Duration
import kotlin.time.toJavaDuration


/**
 * Returns an object of the same type [T] as this object with the specified [duration] added.
 *
 * **Important:** This operation does not work for date types like [LocalDate], [YearMonth] or [Year]
 * as they they don't differ in [Duration] but in [Period].
 */
public inline operator fun <reified T : Temporal> T.plus(duration: Duration): T =
    this.plus(duration.toJavaDuration()) as? T ?: error("broken contract of Temporal operations returning the same type")

/**
 * Returns this file time with with the specified [duration] added.
 */
public operator fun FileTime.plus(duration: Duration): FileTime =
    FileTime.from(toInstant().plus(duration))
