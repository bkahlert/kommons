package koodies.collections

fun <K, E> MutableMap<K, List<E>>.addElement(key: K, element: E): List<E> =
    merge(key, listOf(element), List<E>::plus) ?: emptyList()

fun <K, E> MutableMap<K, List<E>>.removeElement(key: K, element: E): List<E> =
    merge(key, listOf(element), List<E>::minus) ?: emptyList()
