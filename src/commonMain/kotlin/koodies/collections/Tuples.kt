package koodies.collections

/**
 * Creates a tuple of type [Triple] from `this` [Pair] and [that].
 *
 * @sample tripleFromTo
 */
public infix fun <A, B, C> Pair<A, B>.to(that: C): Triple<A, B, C> =
    Triple(first, second, that)

private fun tripleFromTo() {
    "first" to "second" to "third"
}

/**
 * Returns a pair containing the results of applying the given [transform] function
 * to each element in the original pair.
 */
public fun <T, R> Pair<T, T>.map(transform: (T) -> R): Pair<R, R> =
    Pair(transform(first), transform(second))
