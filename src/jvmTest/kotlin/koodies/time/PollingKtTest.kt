package koodies.time

import koodies.unit.milli
import koodies.unit.seconds
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.measureTime

@Execution(CONCURRENT)
class PollingKtTest {

    @Nested
    inner class Succeeding {

        @Test
        fun `should succeed if target state reached`() {
            var counter = Random(12).nextInt(5, 10)
            expectThat(measureTime { 100.milli.seconds.poll { --counter <= 0 }.indefinitely() })
                .isLessThan(1.5.seconds)
        }

        @Test
        fun `should return true`() {
            expectThat(poll { true }.every(100.milli.seconds).indefinitely()).isTrue()
        }

        @Test
        fun `should not call callback`() {
            var callbackCalled = false
            100.milli.seconds.poll { true }.forAtMost(1.seconds) { callbackCalled = true }
            expectThat(callbackCalled).isFalse()
        }
    }

    @Nested
    inner class Checking {

        @Test
        fun `should check condition once per interval`() {
            var counter = 0
            100.milli.seconds.poll { counter++; false }.forAtMost(1.seconds) {}
            expectThat(counter).isGreaterThan(5).isLessThan(15)
        }

        @Test
        fun `should only accept positive interval`() {
            expectCatching { measureTime { Duration.ZERO.poll { true } } }
                .isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should poll using alternative syntax`() {
            var counter = 0
            poll { counter++; false }.every(100.milli.seconds).forAtMost(1.seconds) {}
            expectThat(counter).isGreaterThan(5).isLessThan(15)
        }
    }

    @Nested
    inner class Failing {

        @Test
        fun `should fail if condition becomes false`() {
            var counter = 5
            poll { false }.every(100.milli.seconds).forAsLongAs({ counter-- > 0 }) {}
            expectThat(counter).isLessThanOrEqualTo(0)
        }

        @Test
        fun `should fail if time is up`() {
            val timePassed = measureTime { 100.milli.seconds.poll { false }.forAtMost(1.seconds) {} }
            expectThat(timePassed).isGreaterThan(750.milli.seconds).isLessThan(1250.milli.seconds)
        }

        @Test
        fun `should fail if condition becomes false before time is up`() {
            var counter = 2
            val timePassed = measureTime {
                poll { false }.every(100.milli.seconds).noLongerThan({ counter-- > 0 }, 100.seconds) {}
            }
            expect {
                that(counter).isLessThanOrEqualTo(0)
                that(timePassed).isLessThan(10.seconds)
            }
        }

        @Test
        fun `should fail if time is up while condition still holds true`() {
            var counter = 1
            val timePassed = measureTime {
                poll { false }.every(100.milli.seconds).noLongerThan({ counter++ > 0 }, 0.1.seconds) {}
            }
            expect {
                that(counter).isGreaterThan(0)
                that(timePassed).isGreaterThan(0.1.seconds)
            }
        }

        @Test
        fun `should call callback only if timeout was specified`() {
            val timeout = 1.seconds
            var timePassed: Duration? = null
            100.milli.seconds.poll { false }.forAtMost(timeout) { timePassed = it }
            expectThat(timePassed).isNotNull().isGreaterThan(timeout)
        }

        @Test
        fun `should return false`() {
            expectThat(poll { false }.every(100.milli.seconds).forAtMost(1.seconds) {}).isFalse()
        }

        @Test
        fun `should call callback`() {
            var callbackCalled = false
            100.milli.seconds.poll { false }.forAtMost(1.seconds) { callbackCalled = true }
            expectThat(callbackCalled).isTrue()
        }
    }

    @Nested
    inner class PollCatching {

        @Test
        fun `should succeed if test does not throw`() {
            expectThat(pollCatching { }.every(100.milli.seconds).indefinitely()).isTrue()
        }

        @Test
        fun `should fail if test always throws`() {
            expectThat(pollCatching { throw RuntimeException("test") }.every(100.milli.seconds).forAtMost(1.seconds)).isFalse()
        }
    }
}
