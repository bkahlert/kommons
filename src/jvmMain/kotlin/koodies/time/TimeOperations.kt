package koodies.time

import koodies.jvm.currentThread
import koodies.unit.milli
import koodies.unit.seconds
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.temporal.Temporal
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Make the current [Thread] sleep for this duration.
 */
public fun Duration.sleep() {
    require(this >= Duration.ZERO) { "Duration to sleep must be 0 or more." }
    if (this > Duration.ZERO) Thread.sleep(inWholeMilliseconds)
}

/**
 * Make the current [Thread] sleep for this duration
 * before calling the specified [block] and returning its result.
 */
public fun <R> Duration.sleep(block: () -> R): R {
    sleep()
    return block()
}

public fun Duration.busyWait(sleepIntervals: Duration = 50.milli.seconds) {
    val start = System.currentTimeMillis()
    @Suppress("ControlFlowWithEmptyBody")
    while (notPassedSince(start)) {
        try {
            sleepIntervals.sleep()
        } catch (e: InterruptedException) {
            if (passedSince(start)) {
                currentThread.interrupt()
                break
            }
        }
    }
}

public fun Duration.passedSince(): Long = System.currentTimeMillis() - inWholeMilliseconds
public fun Duration.passedSince(instant: Long): Boolean = passedSince() >= instant
public fun Duration.notPassedSince(instant: Long): Boolean = passedSince() < instant

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


/**
 * Returns an object of the same type [T] as this object with the specified [duration] subtracted.
 *
 * **Important:** This operation does not work for date types like [LocalDate], [YearMonth] or [Year]
 * as they they don't differ in [Duration] but in [Period].
 */
public inline operator fun <reified T : Temporal> T.minus(duration: Duration): T {
    return duration.toComponents { days, hours, minutes, seconds, nanoseconds ->
        sequenceOf(
            java.time.Duration.ofDays(days.toLong()),
            java.time.Duration.ofHours(hours.toLong()),
            java.time.Duration.ofMinutes(minutes.toLong()),
            java.time.Duration.ofSeconds(seconds.toLong()),
            java.time.Duration.ofNanos(nanoseconds.toLong()),
        ).filter { !it.isZero }.fold(this) { temporal, adjuster ->
            if (adjuster.isZero) temporal
            else temporal.minus(adjuster) as? T ?: error("broken contract of Temporal operations returning the same type")
        }
    }
}


/**
 * Returns this file time with with the specified [duration] subtracted.
 */
public operator fun FileTime.minus(duration: Duration): FileTime =
    FileTime.from(toInstant().minus(duration))

/**
 * Returns a [FileTime] representing the same point of time value
 * on the time-line as this instant.
 */
public fun Instant.toFileTime(): FileTime = FileTime.from(this)
