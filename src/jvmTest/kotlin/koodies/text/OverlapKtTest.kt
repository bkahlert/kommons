package koodies.text

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class OverlapKtTest {
    @Suppress("SpellCheckingInspection")
    @TestFactory
    fun `should calculate right-wise overlap`() = listOf(
        "overlap" to 7,
        "verlap" to 6,
        "erlap" to 5,
        "rlap" to 4,
        "lap" to 3,
        "ap" to 2,
        "p" to 1,
        "" to 0,
        "o" to 0,
        "ov" to 0,
        "ove" to 0,
        "over" to 0,
        "overl" to 0,
        "overla" to 0,
        "different" to 0,
    ).map { (other, overlapLength) ->
        dynamicTest("'overlap' has $overlapLength with $other") {
            expectThat("overlap".overlap(other)).isEqualTo(overlapLength)
        }
    }

    @Suppress("SpellCheckingInspection")
    @TestFactory
    fun `should calculate left-wise overlap`() = listOf(
        "overlap" to 7,
        "overla" to 6,
        "overl" to 5,
        "over" to 4,
        "ove" to 3,
        "ov" to 2,
        "o" to 1,
        "" to 0,
        "p" to 0,
        "ap" to 0,
        "lap" to 0,
        "rlap" to 0,
        "erlap" to 0,
        "verlap" to 0,
        "different" to 0,
    ).map { (other, overlapLength) ->
        dynamicTest("$other has $overlapLength with 'overlap'") {
            expectThat(other.overlap("overlap")).isEqualTo(overlapLength)
        }
    }
}
