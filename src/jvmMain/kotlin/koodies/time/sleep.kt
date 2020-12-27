package koodies.time

import kotlin.time.Duration

/**
 * Make the current [Thread] sleep for this duration.
 */
fun Duration.sleep() {
    require(this >= Duration.ZERO) { "Duration to sleep must be 0 or more." }
    if (this > Duration.ZERO) Thread.sleep(toLongMilliseconds())
}
