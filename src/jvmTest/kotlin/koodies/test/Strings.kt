package koodies.test

import koodies.debug.debug
import koodies.regex.countMatches
import koodies.text.ANSI.ansiRemoved
import koodies.text.CodePoint
import koodies.text.LineSeparators
import koodies.text.quoted
import koodies.unit.BinaryPrefix
import koodies.unit.Size
import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder
import strikt.assertions.hasSize

fun <T> Builder<T>.toStringIsEqualTo(expected: String, removeEscapeSequences: Boolean = true): Builder<T> =
    if (removeEscapeSequences) with({ toString().ansiRemoved }) { toStringIsEqualTo(expected, false) }
    else assert("is equal to %s", expected) {
        when (val actual = it.toString()) {
            expected -> pass()
            else -> fail(actual = actual)
        }
    }

inline val <T> Builder<T>.string: Builder<String>
    get() =
        get("to string") { toString() }

infix fun <T> Builder<T>.toStringContains(expected: String): Builder<T> =
    assert("contains %s", expected) {
        when (val actual = it.toString().contains(expected)) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

fun <T> Builder<T>.toStringContainsAll(vararg expected: String): Builder<T> =
    if (expected.size == 1) toStringContains(expected.single())
    else compose("contains %s", expected.joinToString(", ")) {
        expected.forEach { toStringContains(it) }
    }.then { if (allPassed && passedCount > 0) pass() else fail() }


@Deprecated("use toStringIsEqualTo")
fun Builder<*>.asString(trim: Boolean = true): DescribeableBuilder<String> {
    return this.get("asString") {
        val string = when (this) {
            is CodePoint -> this.string
            is Size -> this.toString<BinaryPrefix>()
            else -> this.toString()
        }
        string.takeUnless { trim } ?: string.trim()
    }
}

fun <T : CharSequence> Builder<T>.isEqualToByteWise(other: CharSequence) =
    assert("is equal to byte-wise") { value ->
        val thisString = value.toList()
        val otherString = other.toList()
        when (thisString.containsAll(otherString)) {
            true -> pass()
            else -> fail("\nwas        ${otherString.debug}" +
                "\ninstead of ${thisString.debug}.")
        }
    }

@Suppress("unused")
fun <T : CharSequence> Builder<T>.containsAtLeast(value: CharSequence, lowerLimit: Int = 1) =
    assert("contains ${value.quoted} at least ${lowerLimit}x") {
        val actual = Regex.fromLiteral("$value").countMatches(it)
        if (actual >= lowerLimit) pass()
        else fail("but actually contains it only ${actual}x")
    }

fun <T : CharSequence> Builder<T>.containsAtMost(value: CharSequence, limit: Int = 1) =
    assert("contains ${value.quoted} at most ${limit}x") {
        val actual = Regex.fromLiteral(value.toString()).countMatches(it)
        if (actual <= limit) pass()
        else fail("but actually contains it even ${actual}x")
    }

fun <T : CharSequence> Builder<T>.containsExactly(value: CharSequence, expectedCount: Int) =
    assert("contains ${value.quoted} exactly ${expectedCount}x") {
        val actual = Regex.fromLiteral(value.toString()).countMatches(it)
        if (actual == expectedCount) pass()
        else fail("but actually contains it ${actual}x")
    }

fun <T : CharSequence> Builder<T>.notContainsLineSeparator() =
    assert("contains line separator") { value ->
        val matchedSeparators = LineSeparators.filter { value.contains(it) }
        if (matchedSeparators.isEmpty()) pass()
        else fail("but the following have been found: $matchedSeparators")
    }

fun Builder<String>.prefixes(value: String) =
    assert("prefixed by $value") { prefix ->
        if (value.startsWith(prefix)) pass()
        else fail("$value is not prefixed by ${prefix.debug}")
    }

fun <T> Builder<List<T>>.single(assertion: Builder<T>.() -> Unit) {
    hasSize(1).and { get { this[0] }.run(assertion) }
}
