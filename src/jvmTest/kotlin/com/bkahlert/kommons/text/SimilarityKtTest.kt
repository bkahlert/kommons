package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.test
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.unit.bytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import kotlin.time.measureTime

class SimilarityKtTest {

    @Nested
    inner class LevenshteinDistance {

        @Suppress("SpellCheckingInspection")
        @TestFactory
        fun `should return 0`() = testEach(
            "h1" to "h1",
            "gil" to "gil",
        ) { (from, to) ->
            from asserting { levenshteinDistance(to).isEqualTo(0) }
        }

        @Suppress("SpellCheckingInspection")
        @TestFactory
        fun `should return 1`() = testEach(
            "gil" to "gill",
            "waht" to "what",
            "waht" to "wait",
            "Damerau" to "Damreau",
        ) { (from, to) ->
            from asserting { levenshteinDistance(to).isEqualTo(1) }
        }

        @Suppress("SpellCheckingInspection")
        @TestFactory
        fun `should return 2`() = testEach(
            "ca" to "abc",
            "thaw" to "what",
            "Damerau" to "uameraD",
            "Damerau" to "Daremau",
            "waht" to "whit",
            "what" to "wtah",
        ) { (from, to) ->
            from asserting { levenshteinDistance(to).isEqualTo(2) }
        }
    }

    @Nested
    inner class FuzzyLevenshteinDistance {

        @TestFactory @Slow
        fun `should calculate fuzzy distance between similar strings`() = test(
            (HtmlFixture.text.repeat(200) + "abc") to ("xyz" + HtmlFixture.text.repeat(200))
        ) { (a, b) ->
            a asserting { fuzzyLevenshteinDistance(b).isLessThan(0.05) }
            expecting { measureTime { expectThat(a).fuzzyLevenshteinDistance(b) } } that { isLessThanOrEqualTo(8.seconds) }
        }

        @TestFactory @Slow
        fun `should calculate fuzzy distance between completely different strings`() = test(
            randomString(1000) to randomString(123)
        ) { (a, b) ->
            a asserting { fuzzyLevenshteinDistance(b).isGreaterThan(0.85) }
            expecting { measureTime { expectThat(a).fuzzyLevenshteinDistance(b) } } that { isLessThanOrEqualTo(8.seconds) }
        }
    }
}

/**
 * Computes the [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) between the
 * character sequence of `this` assertion and the given [other] one character sequence and returns an assertion on the distance.
 */
fun <T : CharSequence> Assertion.Builder<T>.levenshteinDistance(other: CharSequence): Assertion.Builder<Int> =
    get("Levenshtein distance") { levenshteinDistance(other) }

/**
 * Fuzzy variant of the [Levenshtein distance](https://en.wikipedia.org/wiki/Levenshtein_distance) algorithm
 * that only compares each string's first and last 500 characters for strings with more than 1000 characters.
 * For strings of length 1000 or less the result is the same as of [levenshteinDistance].
 */
fun <T : CharSequence> Assertion.Builder<T>.fuzzyLevenshteinDistance(other: CharSequence): Assertion.Builder<Double> =
    get("fuzzy Levenshtein distance %s: ${other.length.bytes}") {
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
