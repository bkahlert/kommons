package koodies.collections

import koodies.text.toLowerCase

/**
 * Returns a new [LinkedHashMap] of all elements.
 *
 * In contrast to other maps this one keeps the order of the elements.
 */
public fun <K, V> Iterable<Pair<K, V>>.toLinkedMap(): LinkedHashMap<K, V> =
    LinkedHashMap<K, V>().also { it.putAll(this) }

/**
 * Returns a view on `this` map that matches keys by matching the result
 * of the specified [transform] applied to them.
 */
public fun <K, V, `K'`> Map<K, V>.matchKeysBy(transform: K.() -> `K'`): Map<K, V> = object : Map<K, V> by this {
    override fun get(key: K): V? {
        val transformedKey = key.transform()
        val entries = filterKeys { it.transform() == transformedKey }.entries
        return when (entries.size) {
            0 -> null
            1 -> entries.first().value
            else -> error("$key is not unique as it matches $entries")
        }
    }
}

public fun <V> Map<String?, V>.matchKeysByIgnoringCase(): Map<String?, V> {
    val transform: (String?) -> String? = { it?.toLowerCase() }
    val delegate = mapKeys { transform(it.key) }
    return object : Map<String?, V> by this {
        override fun containsKey(key: String?): Boolean = delegate.containsKey(transform(key))
        override fun get(key: String?): V? = delegate[transform(key)]
        override val keys: Set<String?> = delegate.keys
        override val entries: Set<Map.Entry<String?, V>> = delegate.entries
        override fun toString(): String = entries.toString()
    }
}

public fun <K, E> MutableMap<K, List<E>>.addElement(key: K, element: E): List<E> {
    val newList: List<E> = get(key)?.let { it + element } ?: listOf(element)
    put(key, newList)
    return newList
}

public fun <K, E> MutableMap<K, List<E>>.removeElement(key: K, element: E): List<E> {
    val newList: List<E> = get(key)?.let { it - element } ?: emptyList()
    if (newList.isEmpty()) remove(key)
    else put(key, newList)
    return newList
}
