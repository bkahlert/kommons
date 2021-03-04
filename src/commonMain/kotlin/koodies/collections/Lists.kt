package koodies.collections

import koodies.number.mod
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public val <T> List<T>.head: T
    get() = first()

public val <T> List<T>.tail: List<T>
    get() = drop(1)


/**
 * Returns `true` if the number of [predicates] is the same and each element matches its corresponding predicate.
 */
public fun <T> List<T>.pairwiseAll(vararg predicates: (T) -> Boolean): Boolean =
    size == predicates.size && (predicates.indices).all { i -> this[i].let(predicates[i]) }


/**
 * Returns `true` if the number of [predicates] is the same and each element matches its corresponding predicate.
 */
public fun <T> Array<T>.pairwiseAll(vararg predicates: (T) -> Boolean): Boolean =
    size == predicates.size && (predicates.indices).all { i -> this[i].let(predicates[i]) }

/**
 * Removes the [n] elements from this mutable list and returns those removed elements,
 * or throws [IllegalArgumentException] if this list contains fewer elements.
 */
public fun <T> MutableList<T>.removeFirst(n: Int): List<T> {
    require(n <= size) { "Attempted to remove first $n elements although only $size were present." }
    return (0 until n).map { removeFirst() }
}


/**
 * Returns the same list with two differences:
 * 1) Negative indices are supported and start from the end of this list (e.g. `this[-1]` returns the last element, `this[-2]` returns the second, and so on).
 * 2) Modulus operation is applied. E.g. `listOf("a","b","c").withNegativeIndices(4)` returns `a`. `this[-4]` would return `c`.
 */
public inline fun <reified T> List<T>.withNegativeIndices(): List<T> {
    return object : List<T> by this {
        override fun get(index: Int): T = this@withNegativeIndices[index.mod(size)]
    }
}

/**
 * Returns the same list providing function with two differences:
 * 1) Negative indices are supported and start from the end of this list (e.g. `this[-1]` returns the last element, `this[-2]` returns the second, and so on).
 * 2) Modulus operation is applied. E.g. `listOf("a","b","c").withNegativeIndices(4)` returns `a`. `this[-4]` would return `c`.
 */
@Suppress("unused")
public inline fun <reified T> (() -> List<T>).withNegativeIndices(): () -> List<T> = { this().withNegativeIndices() }

/**
 * Returns a [ReadOnlyProperty] that delegates all calls to [listProvider] unmodified with one exception:
 * The returned [List] is wrapped to allow negative indices:
 * 1) Negative indices are supported and start from the end of the list (e.g. `this[-1]` returns the last element, `this[-2]` returns the second, and so on).
 * 2) Modulus operation is applied. E.g. `listOf("a","b","c").withNegativeIndices(4)` returns `a`. `this[-4]` would return `c`.
 */
@Suppress("unused")
public inline fun <reified T, reified V> withNegativeIndices(noinline listProvider: () -> List<V>): ReadOnlyProperty<T, List<V>> =
    ReadOnlyProperty { _: T, _: KProperty<*> -> listProvider().withNegativeIndices() }
