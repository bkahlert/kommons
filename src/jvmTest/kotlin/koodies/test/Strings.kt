package koodies.test

import koodies.text.CodePoint
import koodies.text.GraphemeCluster
import koodies.unit.BinaryPrefix
import koodies.unit.Size
import strikt.api.Assertion
import strikt.api.DescribeableBuilder

infix fun <T> Assertion.Builder<T>.toStringIsEqualTo(expected: String): Assertion.Builder<T> =
    assert("is equal to %s", expected) {
        when (val actual = it.toString()) {
            expected -> pass()
            else -> fail(actual = actual)
        }
    }

infix fun <T> Assertion.Builder<T>.toStringContains(expected: String): Assertion.Builder<T> =
    assert("contains %s", expected) {
        when (val actual = it.toString().contains(expected)) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

fun <T> Assertion.Builder<T>.toStringContainsAll(vararg expected: String): Assertion.Builder<T> =
    if (expected.size == 1) toStringContains(expected.single())
    else compose("contains %s", expected.joinToString(", ")) {
        expected.forEach { toStringContains(it) }
    }.then { if (allPassed && passedCount > 0) pass() else fail() }


@Deprecated("use toStringIsEqualTo")
fun Assertion.Builder<*>.asString(trim: Boolean = true): DescribeableBuilder<String> {
    return this.get("asString") {
        val string = when (this) {
            is CodePoint -> this.string
            is GraphemeCluster -> this.asString
            is Size -> this.toString<BinaryPrefix>()
            else -> this.toString()
        }
        string.takeUnless { trim } ?: string.trim()
    }
}
