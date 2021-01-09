package koodies.collections

/**
 * Returns a new [LinkedHashMap] of all elements.
 *
 * In contrast to other maps this one keeps the order of the elements.
 */
fun <K, V> Iterable<Pair<K, V>>.toLinkedMap(): LinkedHashMap<K, V> =
    LinkedHashMap<K, V>().also { it.putAll(this) }
