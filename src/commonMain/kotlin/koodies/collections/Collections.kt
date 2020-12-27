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
