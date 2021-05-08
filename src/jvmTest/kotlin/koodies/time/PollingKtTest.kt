package koodies.time

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
            expectThat(measureTime { Duration.milliseconds(100).poll { --counter <= 0 }.indefinitely() })
                .isLessThan(Duration.seconds(1.5))
        }

        @Test
        fun `should return true`() {
            expectThat(poll { true }.every(Duration.milliseconds(100)).indefinitely()).isTrue()
        }

        @Test
        fun `should not call callback`() {
            var callbackCalled = false
            Duration.milliseconds(100).poll { true }.forAtMost(Duration.seconds(1)) { callbackCalled = true }
            expectThat(callbackCalled).isFalse()
        }
    }

    @Nested
    inner class Checking {

        @Test
        fun `should check condition once per interval`() {
            var counter = 0
            Duration.milliseconds(100).poll { counter++; false }.forAtMost(Duration.seconds(1)) {}
            expectThat(counter).isGreaterThan(5).isLessThan(15)
        }

        @Test
        fun `should only accept positive interval`() {
            expectCatching { measureTime { Duration.milliseconds(0).poll { true } } }
                .isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should poll using alternative syntax`() {
            var counter = 0
            poll { counter++; false }.every(Duration.milliseconds(100)).forAtMost(Duration.seconds(1)) {}
            expectThat(counter).isGreaterThan(5).isLessThan(15)
        }
    }

    @Nested
    inner class Failing {

        @Test
        fun `should fail if condition becomes false`() {
            var counter = 5
            poll { false }.every(Duration.milliseconds(100)).forAsLongAs({ counter-- > 0 }) {}
            expectThat(counter).isLessThanOrEqualTo(0)
        }

        @Test
        fun `should fail if time is up`() {
            val timePassed = measureTime { Duration.milliseconds(100).poll { false }.forAtMost(Duration.seconds(1)) {} }
            expectThat(timePassed).isGreaterThan(Duration.milliseconds(750)).isLessThan(Duration.milliseconds(1250))
        }

        @Test
        fun `should fail if condition becomes false before time is up`() {
            var counter = 2
            val timePassed = measureTime {
                poll { false }.every(Duration.milliseconds(100)).noLongerThan({ counter-- > 0 }, Duration.seconds(100)) {}
            }
            expect {
                that(counter).isLessThanOrEqualTo(0)
                that(timePassed).isLessThan(Duration.seconds(10))
            }
        }

        @Test
        fun `should fail if time is up while condition still holds true`() {
            var counter = 1
            val timePassed = measureTime {
                poll { false }.every(Duration.milliseconds(100)).noLongerThan({ counter++ > 0 }, Duration.seconds(0.1)) {}
            }
            expect {
                that(counter).isGreaterThan(0)
                that(timePassed).isGreaterThan(Duration.seconds(0.1))
            }
        }

        @Test
        fun `should call callback only if timeout was specified`() {
            val timeout = Duration.seconds(1)
            var timePassed: Duration? = null
            Duration.milliseconds(100).poll { false }.forAtMost(timeout) { timePassed = it }
            expectThat(timePassed).isNotNull().isGreaterThan(timeout)
        }

        @Test
        fun `should return false`() {
            expectThat(poll { false }.every(Duration.milliseconds(100)).forAtMost(Duration.seconds(1)) {}).isFalse()
        }

        @Test
        fun `should call callback`() {
            var callbackCalled = false
            Duration.milliseconds(100).poll { false }.forAtMost(Duration.seconds(1)) { callbackCalled = true }
            expectThat(callbackCalled).isTrue()
        }
    }

    @Nested
    inner class PollCatching {

        @Test
        fun `should succeed if test does not throw`() {
            expectThat(pollCatching { }.every(Duration.milliseconds(100)).indefinitely()).isTrue()
        }

        @Test
        fun `should fail if test always throws`() {
            expectThat(pollCatching { throw RuntimeException("test") }.every(Duration.milliseconds(100)).forAtMost(Duration.seconds(1))).isFalse()
        }
    }
}
