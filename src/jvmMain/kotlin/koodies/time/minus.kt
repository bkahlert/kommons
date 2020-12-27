package koodies.time

import java.nio.file.attribute.FileTime
import java.time.Duration.ofDays
import java.time.Duration.ofHours
import java.time.Duration.ofMinutes
import java.time.Duration.ofNanos
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.temporal.Temporal
import kotlin.time.Duration

/**
 * Returns an object of the same type [T] as this object with the specified [duration] subtracted.
 *
 * **Important:** This operation does not work for date types like [LocalDate], [YearMonth] or [Year]
 * as they they don't differ in [Duration] but in [Period].
 */
inline operator fun <reified T : Temporal> T.minus(duration: Duration): T {
    return duration.toComponents { days, hours, minutes, seconds, nanoseconds ->
        sequenceOf(
            ofDays(days.toLong()),
            ofHours(hours.toLong()),
            ofMinutes(minutes.toLong()),
            ofSeconds(seconds.toLong()),
            ofNanos(nanoseconds.toLong()),
        ).filter { !it.isZero }.fold(this) { temporal, adjuster ->
            if (adjuster.isZero) temporal
            else temporal.minus(adjuster) as? T ?: error("broken contract of Temporal operations returning the same type")
        }
    }
}


/**
 * Returns this file time with with the specified [duration] subtracted.
 */
operator fun FileTime.minus(duration: Duration): FileTime =
    FileTime.from(toInstant().minus(duration))
