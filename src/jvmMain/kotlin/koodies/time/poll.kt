package koodies.time

import koodies.time.IntervalPolling.Polling
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * Checks once per [interval][Duration] if the [targetState] evaluates to `true`
 * [Polling.indefinitely] or [Polling.forAtMost] the specified [Duration].
 *
 * @sample PollSample.pollAtMostOneSecond
 */
fun Duration.poll(targetState: () -> Boolean): Polling {
    require(this > Duration.ZERO) { "Interval to sleep must be positive." }
    return IntervalPolling(targetState).every(this)
}

/**
 * Checks in once per [interval][IntervalPolling.every] if the [targetState] evaluates to `true`
 * [Polling.indefinitely] or [Polling.forAtMost] the specified [Duration].
 *
 * @sample PollSample.pollTargetState
 */
fun poll(targetState: () -> Boolean): IntervalPolling = IntervalPolling(targetState)

/**
 * Specifies the interval by which [targetState] should be polled.
 *
 * @sample PollSample.pollTargetState
 */
inline class IntervalPolling(private val targetState: () -> Boolean) {
    /**
     * Specifies the [pollInterval] by which [targetState] should be polled.
     *
     * @sample PollSample.pollAtMostOneSecond
     * @sample PollSample.pollTargetState
     */
    fun every(pollInterval: Duration): Polling {
        require(pollInterval > Duration.ZERO) { "Interval to sleep must be positive." }
        return Polling(pollInterval, targetState)
    }

    /**
     * Specifies the [Duration] the [targetState] should be polled for.
     *
     * @sample PollSample.pollAtMostOneSecond
     * @sample PollSample.pollTargetState
     */
    class Polling(private val pollInterval: Duration, private val targetState: () -> Boolean) {
        private var condition: () -> Boolean = { true }
        private var callback: (Duration) -> Unit = {}

        /**
         * Polls the [targetState] indefinitely.
         *
         * Returns whether the [targetState] was reached in time.
         * Since this method only returns in case of success, *if* this
         * method returns, the return value is always `true`.
         */
        fun indefinitely(): Boolean = poll()

        /**
         * Polls the [targetState] for at most the specified [timeout]
         * and calls [callback] if within that time the [targetState]
         * did not evaluate to `true`.
         *
         * Returns whether the [targetState] was reached in time.
         */
        fun forAtMost(timeout: Duration, callback: (Duration) -> Unit = {}): Boolean {
            val failAfter = Now.instant + timeout
            condition = { Now.instant <= failAfter }
            this.callback = callback
            return poll()
        }

        /**
         * Polls the [targetState] for as long as the specified [invariant]
         * evaluates to `true` and calls [callback] if [invariant] evaluates to `false`.
         *
         * Returns whether the [targetState] was reached.
         */
        fun forAsLongAs(invariant: () -> Boolean, callback: (Duration) -> Unit = {}): Boolean {
            condition = invariant
            this.callback = callback
            return poll()
        }

        /**
         * Polls the [targetState] for at most the specified [timeout]
         * and only as long as the [invariant] holds `true`. Calls [callback] if
         * either the time is up of the invariant no longer holds.
         *
         * Returns whether the [targetState] was reached in time and held invariant.
         */
        fun noLongerThan(invariant: () -> Boolean, timeout: Duration, callback: (Duration) -> Unit = {}): Boolean {
            val failAfter = Now.instant + timeout
            condition = { Now.instant <= failAfter && invariant() }
            this.callback = callback
            return poll()
        }

        private fun poll(): Boolean {
            val startTime = System.currentTimeMillis()
            while (!targetState() && condition()) {
                pollInterval.sleep()
            }
            return if (!targetState()) {
                callback(Now.passedSince(startTime))
                false
            } else {
                true
            }
        }
    }
}

private class PollSample {
    fun pollAtMostOneSecond() {
        // poll every 100 milliseconds for a condition to become true
        // for at most 1 second
        val condition: () -> Boolean = { listOf(true, false).random() }

        100.milliseconds.poll { condition() }.forAtMost(1.seconds) { passed ->
            throw TimeoutException("Condition did not become true within $passed")
        }
    }

    fun pollTargetState() {
        // poll every 100 milliseconds for a condition to become true
        // for at most 1 second
        val condition: () -> Boolean = { listOf(true, false).random() }

        poll { condition() }.every(100.milliseconds).forAtMost(1.seconds) { passed ->
            throw TimeoutException("Condition did not become true within $passed")
        }
    }
}
