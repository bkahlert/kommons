package koodies.text

import koodies.test.HtmlFile
import koodies.test.Slow
import koodies.test.test
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import kotlin.system.measureTimeMillis
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(SAME_THREAD)
class LevenshteinDistanceKtTest {

    @Suppress("SpellCheckingInspection")
    @TestFactory
    fun `should calc Levenshtein distance 0`() = listOf(
        "h1" to "h1",
        "gil" to "gil",
    ).map { (from, to) ->
        dynamicTest("${from.quoted} ↔︎ ${to.quoted}") {
            expectThat(from).levenshteinDistance(to).isEqualTo(0)
        }
    }

    @Suppress("SpellCheckingInspection")
    @TestFactory
    fun `should calc Levenshtein distance 1`() = listOf(
        "gil" to "gill",
        "waht" to "what",
        "waht" to "wait",
        "Damerau" to "Damreau",
    ).map { (from, to) ->
        dynamicTest("${from.quoted} ↔︎ ${to.quoted}") {
            expectThat(from).levenshteinDistance(to).isEqualTo(1)
        }
    }

    @Suppress("SpellCheckingInspection")
    @TestFactory
    fun `should calc Levenshtein distance 2`() = listOf(
        "ca" to "abc",
        "thaw" to "what",
        "Damerau" to "uameraD",
        "Damerau" to "Daremau",
        "waht" to "whit",
        "what" to "wtah",
    ).map { (from, to) ->
        dynamicTest("${from.quoted} ↔︎ ${to.quoted}") {
            expectThat(from).levenshteinDistance(to).isEqualTo(2)
        }
    }

    @Nested
    inner class Fuzzy {
        @TestFactory @Slow
        fun `should calculate fuzzy distance between similar strings`() = test(
            (HtmlFile.text.repeat(200) + "abc") to ("xyz" + HtmlFile.text.repeat(200))
        ) { (a, b) ->
            expect { a }.that { fuzzyLevenshteinDistance(b).isLessThan(0.05) }
            expect { measureTimeMillis { expectThat(a).fuzzyLevenshteinDistance(b) }.milliseconds }.that { isLessThanOrEqualTo(5.seconds) }
        }

        @TestFactory @Slow
        fun `should calculate fuzzy distance between completely different strings`() = test(
            randomString(1000) to randomString(123)
        ) { (a, b) ->
            expect { a }.that { fuzzyLevenshteinDistance(b).isGreaterThan(0.85) }
            expect { measureTimeMillis { expectThat(a).fuzzyLevenshteinDistance(b) }.milliseconds }.that { isLessThanOrEqualTo(5.seconds) }
        }
    }
}

/**
 * Computes the [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) between the
 * char sequence of `this` assertion and the given [other] one char sequence and returns an assertion on the distance.
 */
fun <T : CharSequence> Assertion.Builder<T>.levenshteinDistance(other: CharSequence): Assertion.Builder<Int> =
    get("Levenshtein distance") { levenshteinDistance(other) }

/**
 * Fuzzy variant of the [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) algorithm
 * that only compares each string's first and last 500 characters for strings with more than 1000 characters.
 * For strings of length 1000 or less the result is the same as of [levenshteinDistance].
 */
fun <T : CharSequence> Assertion.Builder<T>.fuzzyLevenshteinDistance(other: CharSequence): Assertion.Builder<Double> =
    get("fuzzy Levenshtein distance %s") {
        val thisString = "$this"
        val otherString = "$other"
        when {
            thisString.length == otherString.length && thisString == otherString ->
                0.0
            thisString.length > 1000 && otherString.length > 1000 ->
                (thisString.take(500).levenshteinDistance(otherString.take(500)) +
                    thisString.takeLast(500).levenshteinDistance(otherString.takeLast(500))) / 1000.0
            else ->
                this.levenshteinDistance(other).toDouble() / maxOf(thisString.length, otherString.length).toDouble()
        }
    }



