package com.bkahlert.kommons.collections

public interface Dictionary<K, out V> : Map<K, V> {
    override operator fun get(key: K): V
}

public class MapBasedDictionary<K, out V>(
    private val map: Map<K, V>,
    private val default: (K) -> V,
) : Dictionary<K, V>, Map<K, V> by map {
    override operator fun get(key: K): V = map.getOrElse(key, { default(key) })
}

public fun <K, V> dictOf(map: Map<K, V>, default: (K) -> V): Dictionary<K, V> = MapBasedDictionary(map, default)
public fun <K, V> dictOf(vararg pairs: Pair<K, V>, default: (K) -> V): Dictionary<K, V> = MapBasedDictionary(pairs.toMap(), default)
public fun <K, V> Iterable<Pair<K, V>>.toDict(default: (K) -> V): Dictionary<K, V> = dictOf(toMap(), default)
