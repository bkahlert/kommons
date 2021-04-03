package koodies.concurrent.process

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isTrue
import kotlin.time.Duration
import kotlin.time.milliseconds

@Execution(CONCURRENT)
class ProcessExitMockTest {

    @Test
    fun `should provide exit code immediately`() {
        val processExit = ProcessExitMock.immediateExit(42)

        val exitValue = processExit()

        expectThat(exitValue).isEqualTo(42)
    }

    @Test
    fun `should delay exit`() {
        val exitDelay = 50.milliseconds
        val start = System.currentTimeMillis()
        val processExit = ProcessExitMock.delayedExit(42, exitDelay)

        val exitValue = processExit()

        expectThat(exitValue).isEqualTo(42)
        expectThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(exitDelay.toLongMilliseconds())
    }

    @Test
    fun `should return true on sufficient calculation time`() {
        val exitDelay = 50.milliseconds
        val processExit = ProcessExitMock.delayedExit(42, exitDelay)

        val returnValue = processExit(exitDelay * 2)

        expectThat(returnValue).isTrue()
    }

    @Test
    fun `should return false on insufficient calculation time`() {
        val exitDelay = 50.milliseconds
        val processExit = ProcessExitMock.delayedExit(42, exitDelay)

        val returnValue = processExit(exitDelay / 2)

        expectThat(returnValue).isFalse()
    }

    @Test
    fun `should never finish on deadlock`() {
        val processExit = ProcessExitMock.deadLock()
        expectThat(processExit.delay).isEqualTo(Duration.INFINITE)
    }
}
