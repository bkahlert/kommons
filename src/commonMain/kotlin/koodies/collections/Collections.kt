package koodies.collections

import koodies.Exceptions.NSEE
import kotlin.collections.dropLast as kotlinDropLast

/**
 * Returns a list containing all elements except last [n] elements.
 *
 * @throws IllegalArgumentException if [n] is negative.
 *
 * @see kotlinDropLast
 */
public fun <T> List<T>.dropLast(n: Int = 1): List<T> =
    kotlinDropLast(n)

/**
 * Returns a new [LinkedHashSet] of all elements.
 *
 * In contrast to most other sets, this one keeps the order of the elements.
 */
public fun <E> Iterable<E>.toLinkedSet(): LinkedHashSet<in E> =
    LinkedHashSet<E>().also { it.addAll(this) }

/**
 * Requires this iterable to contain exactly one element of the given type [E]
 * and returns it. Throws [NoSuchElementException] otherwise.
 */
public inline fun <reified E> Iterable<Any?>.requireContainsSingleOfType(): E =
    filterIsInstance<E>().singleOrNull() ?: throw NSEE("Exactly one element of type ${E::class.simpleName} is required.")
