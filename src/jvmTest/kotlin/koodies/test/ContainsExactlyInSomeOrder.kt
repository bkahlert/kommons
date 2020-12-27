package koodies.test

import koodies.builder.ElementGroupsBuilder
import koodies.collections.removeFirst
import org.junit.jupiter.api.fail
import strikt.api.Assertion
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder


/**
 * Asserts if this [Iterable] contains exactly the elements provided by [init].
 *
 * In contrast to [containsExactly] where the order of elements is strictly asserted
 * and [containsExactlyInAnyOrder] where the order of elements is ignored,
 * [containsExactlyInSomeOrder] expect an [Iterable] of groups. While the order
 * of groups is strictly asserted, the elements inside a group can be in any order.
 *
 * @sample [ContainsExactlyInSomeOrderKtTest.PassingAssertion]
 */
fun <T : Iterable<E>, E> Assertion.Builder<T>.containsExactlyInSomeOrder(init: ElementGroupsBuilder<E>.() -> Unit): Assertion.Builder<T> {
    val mutableListOf = mutableListOf<MutableList<E>>()
    return containsExactlyInSomeOrder(*mutableListOf.also { ElementGroupsBuilder(it).init() }.toTypedArray())
}

/**
 * Asserts if this [Iterable] contains exactly the provided elements groups.
 *
 * In contrast to [containsExactly] where the order of elements is strictly asserted
 * and [containsExactlyInAnyOrder] where the order of elements is ignored,
 * [containsExactlyInSomeOrder] expect an [Iterable] of groups. While the order
 * of groups is strictly asserted, the elements inside a group can be in any order.
 *
 * @sample [ContainsExactlyInSomeOrderKtTest.PassingAssertion]
 */
fun <T : Iterable<E>, E> Assertion.Builder<T>.containsExactlyInSomeOrder(vararg elementGroups: Iterable<E>): Assertion.Builder<T> =
    compose("contains exactly the elements %s in some order", elementGroups.toList()) { subject ->
        val original = subject.toList()
        if (original.isEmpty() && elementGroups.isNotEmpty()) fail("is empty but ${elementGroups.sumBy { it.count() }} elements expected")
        val remaining = subject.toMutableList()
        var i = 0
        elementGroups.forEach { elements ->
            assert("contains %s", elements) {
                if (remaining.removeFirst(elements.count()).containsAll(elements.toList())) {
                    pass()
                    val indices = (0 until elements.count()).map { it + i }.toList()
                    assert("…at indices $indices", elements) {
                        val subList = original.subList(indices.first(), indices.last() + 1)
                        when {
                            indices.any { it !in original.indices } -> fail("indices $indices are out of range")
                            subList.containsAll(elements.toList()) -> pass()
                            else -> fail(actual = subList)
                        }
                    }
                } else {
                    fail()
                }
            }
            i += elements.count()
        }
        assert("contains no further elements", emptyList<E>()) {
            if (remaining.isEmpty()) {
                pass()
            } else {
                fail(actual = remaining.toList())
            }
        }
    } then {
        if (allPassed) pass() else fail()
    }
