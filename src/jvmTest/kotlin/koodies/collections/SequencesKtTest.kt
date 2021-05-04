package koodies.collections

import strikt.api.Assertion.Builder

/**
 * Maps an assertion on a sequence to an assertion on its size.
 *
 * @see Sequence.size
 */
val <T : Sequence<*>> Builder<T>.size: Builder<Int>
    get() = get { count() }

/**
 * Asserts that all elements of the subject pass the assertions in [predicate].
 */
infix fun <T : Sequence<E>, E> Builder<T>.all(predicate: Builder<E>.() -> Unit): Builder<T> =
    compose("all elements match:") { subject ->
        subject.forEach { element ->
            get("%s") { element }.apply(predicate)
        }
    } then {
        if (allPassed) pass() else fail()
    }

/**
 * Asserts that _at least one_ element of the subject pass the assertions in
 * [predicate].
 */
infix fun <T : Sequence<E>, E> Builder<T>.any(predicate: Builder<E>.() -> Unit): Builder<T> =
    compose("at least one element matches:") { subject ->
        subject.forEach { element ->
            get("%s") { element }.apply(predicate)
        }
    } then {
        if (anyPassed) pass() else fail()
    }

/**
 * Asserts that _no_ elements of the subject pass the assertions in [predicate].
 */
infix fun <T : Sequence<E>, E> Builder<T>.none(predicate: Builder<E>.() -> Unit): Builder<T> =
    compose("no elements match:") { subject ->
        subject.forEach { element ->
            get("%s") { element }.apply(predicate)
        }
    } then {
        if (allFailed) pass() else fail()
    }
