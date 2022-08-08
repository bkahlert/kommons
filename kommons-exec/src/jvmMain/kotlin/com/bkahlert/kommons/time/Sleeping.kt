package com.bkahlert.kommons.time

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.minus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

public fun Duration.busyWait(sleepIntervals: Duration = 50.milliseconds) {
    val start = Now
    @Suppress("ControlFlowWithEmptyBody")
    while (Now.minus(start) < this) {
        try {
            sleepIntervals.sleep()
        } catch (e: InterruptedException) {
            if (Now.minus(start) <= Duration.ZERO) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }
}
