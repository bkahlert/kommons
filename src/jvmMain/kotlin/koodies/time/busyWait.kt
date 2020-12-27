package koodies.time

import kotlin.time.Duration
import kotlin.time.milliseconds

fun Duration.busyWait(sleepIntervals: Duration = 50.milliseconds) {
    val start = System.currentTimeMillis()
    @Suppress("ControlFlowWithEmptyBody")
    while (notPassedSince(start)) {
        try {
            sleepIntervals.sleep()
        } catch (e: InterruptedException) {
            if (passedSince(start)) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }
}
