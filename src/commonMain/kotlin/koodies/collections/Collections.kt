package koodies.collections

import kotlin.collections.dropLast as kotlinDropLast

/**
 * Returns a list containing all elements except last [n] elements.
 *
 * @throws IllegalArgumentException if [n] is negative.
 *
 * @see kotlinDropLast
 */
fun <T> List<T>.dropLast(n: Int = 1): List<T> =
    kotlinDropLast(n)

/**
 * Returns a new [LinkedHashSet] of all elements.
 *
 * In contrast to most other sets, this one keeps the order of the elements.
 */
fun <E> Iterable<E>.toLinkedSet(): LinkedHashSet<in E> =
    LinkedHashSet<E>().also { it.addAll(this) }
