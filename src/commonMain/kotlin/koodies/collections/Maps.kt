package koodies.collections

/**
 * Returns a new [LinkedHashMap] of all elements.
 *
 * In contrast to other maps this one keeps the order of the elements.
 */
fun <K, V> Iterable<Pair<K, V>>.toLinkedMap(): LinkedHashMap<K, V> =
    LinkedHashMap<K, V>().also { it.putAll(this) }

/**
 * Returns a view on `this` map that matches keys by matching the result
 * of the specified [transform] applied to them.
 */
fun <K, V, `K'`> Map<K, V>.matchKeysBy(transform: K.() -> `K'`) = object : Map<K, V> by this {
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
