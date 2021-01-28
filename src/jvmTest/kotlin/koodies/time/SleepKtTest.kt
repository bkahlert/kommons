package koodies.time

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class SleepKtTest {
    @Test
    fun `should sleep on positive duration`() {
        expectThat(measureTime { 1.seconds.sleep() }).isGreaterThan(900.milliseconds).isLessThan(1100.milliseconds)
    }

    @Test
    fun `should sleep and call block on positive duration`() {
        expectThat(1.seconds.sleep { 42 }).isEqualTo(42)
    }

    @Test
    fun `should not sleep on zero duration`() {
        expectThat(measureTime { Duration.ZERO.sleep() }).isLessThanOrEqualTo(100.milliseconds)
    }

    @Test
    fun `should not sleep and call block on zero duration`() {
        expectThat(Duration.ZERO.sleep { 42 }).isEqualTo(42)
    }

    @Test
    fun `should throw on negative duration`() {
        expectCatching { measureTime { (-1).seconds.sleep() } }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw and not run block on negative duration`() {
        expectCatching { (-1).seconds.sleep { throw RuntimeException("error") } }.isFailure().isA<IllegalArgumentException>()
    }
}
