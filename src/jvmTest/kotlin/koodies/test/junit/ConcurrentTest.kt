package koodies.test.junit

import koodies.collections.maxOrThrow
import koodies.collections.minOrThrow
import koodies.collections.synchronizedMapOf
import koodies.test.Slow
import koodies.time.poll
import koodies.time.seconds
import koodies.unit.milli
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.Assertion
import strikt.api.expectThat

@Isolated
@Slow
@Execution(CONCURRENT)
class ConcurrentTest {

    private enum class Tests { TEST1, TEST2, TEST3 }

    private val starts = synchronizedMapOf<Tests, Long>()
    private val ends = synchronizedMapOf<Tests, Long>()

    @TestFactory
    fun `should run concurrent tests`() = Tests.values().map { test ->
        DynamicTest.dynamicTest(test.name) {
            starts[test] = System.currentTimeMillis()
            poll { false }.every(1.seconds).forAtMost(2.seconds) {}
            ends[test] = System.currentTimeMillis()
        }
    }

    @Test
    fun `should run concurrent tests currently`() {
        poll { starts.size == Tests.values().size && ends.size == Tests.values().size }.every(100.milli.seconds).indefinitely()
        expectThat(Tests.values().toList()).ranConcurrently(starts, ends)
    }
}


fun <T : Enum<T>> Assertion.Builder<List<T>>.ranConcurrently(startTimes: Map<T, Long>, endTimes: Map<T, Long>): Assertion.Builder<List<T>> =
    assert("ran concurrently") { tests ->
        val relevantStartTimes = startTimes.filterKeys { it in tests }
        val relevantEndTimes = endTimes.filterKeys { it in tests }

        val startDifference = relevantStartTimes.values.run { maxOrThrow() - minOrThrow() }.milli.seconds
        val endDifference = relevantEndTimes.values.run { maxOrThrow() - minOrThrow() }.milli.seconds
        val overallDifference = (relevantEndTimes.values.maxOrThrow() - relevantStartTimes.values.minOrThrow()).milli.seconds
        when {
            startDifference < 0.5.seconds -> fail("$startDifference difference between start times")
            endDifference < 0.5.seconds -> fail("$endDifference difference between end times")
            overallDifference < 0.5.seconds -> fail("$overallDifference difference between first start and last end time")
            else -> pass()
        }
    }
