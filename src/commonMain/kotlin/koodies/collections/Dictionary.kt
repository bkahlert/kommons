package koodies.collections

interface Dictionary<K, out V> : Map<K, V> {
    override operator fun get(key: K): V
}

class MapBasedDictionary<K, out V>(
    private val map: Map<K, V>,
    private val default: (K) -> V,
) : Dictionary<K, V>, Map<K, V> by map {
    override operator fun get(key: K): V = map.getOrElse(key, { default(key) })
}

fun <K, V> dictOf(map: Map<K, V>, default: (K) -> V) = MapBasedDictionary(map, default)
fun <K, V> dictOf(vararg pairs: Pair<K, V>, default: (K) -> V) = MapBasedDictionary(pairs.toMap(), default)
fun <K, V> Iterable<Pair<K, V>>.toDict(default: (K) -> V) = dictOf(toMap(), default)
