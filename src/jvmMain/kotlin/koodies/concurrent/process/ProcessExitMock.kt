package koodies.concurrent.process

import koodies.time.busyWait
import kotlin.time.Duration
import kotlin.time.milliseconds

public open class ProcessExitMock(public val exitValue: Int, public val delay: Duration) {
    public open operator fun invoke(): Int {
        delay.busyWait()
        return exitValue
    }

    public open operator fun invoke(timeout: Duration): Boolean {
        delay.busyWait()
        return timeout > delay
    }

    public companion object {
        public fun immediateSuccess(): ProcessExitMock = ProcessExitMock(0, Duration.ZERO)
        public fun immediateExit(exitValue: Int): ProcessExitMock = ProcessExitMock(exitValue, Duration.ZERO)
        public fun computing(): ProcessExitMock = delayedExit(0, 1.milliseconds)
        public fun delayedExit(exitValue: Int, delay: Duration): ProcessExitMock = ProcessExitMock(exitValue, delay)
        public fun deadLock(): ProcessExitMock = ProcessExitMock(-1, Duration.INFINITE)
    }
}
