package koodies.collections

import java.util.Collections

/**
 * Returns a **thread-safe** empty new [MutableSet].
 *
 * The returned set preserves the element iteration order.
 */
public fun <T> synchronizedSetOf(): MutableSet<T> =
    Collections.synchronizedSet(mutableSetOf())

/**
 * Returns a new **thread-safe** [MutableSet] with the given elements.
 *
 * Elements of the set are iterated in the order they were specified.
 */
public fun <T> synchronizedSetOf(vararg elements: T): MutableSet<T> =
    Collections.synchronizedSet(mutableSetOf(*elements))

/**
 * Returns an empty new **thread-safe** [MutableList].
 */
public fun <T> synchronizedListOf(): MutableList<T> =
    Collections.synchronizedList(mutableListOf())

/**
 * Returns a new **thread-safe** [MutableList] with the given elements.
 */
public fun <T> synchronizedListOf(vararg elements: T): MutableList<T> =
    Collections.synchronizedList(mutableListOf(*elements))

/**
 * Returns an empty new **thread-safe** [MutableMap].
 *
 * The returned map preserves the entry iteration order.
 */
public fun <K, V> synchronizedMapOf(): MutableMap<K, V> =
    Collections.synchronizedMap(mutableMapOf())

/**
 * Returns a new **thread-safe** [MutableMap] with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
public fun <K, V> synchronizedMapOf(vararg pairs: Pair<K, V>): MutableMap<K, V> =
    Collections.synchronizedMap(mutableMapOf(*pairs))
