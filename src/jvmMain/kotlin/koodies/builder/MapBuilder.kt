package koodies.builder


/**
 * Builds a map [K], [V] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`.
 *
 * @return the build instance
 */
inline fun <reified B : (B) -> Map<K, V>, reified K : Any, reified V> (B.() -> Unit).buildMap(): Map<K, V> {
    val zeroArgConstructors = B::class.java.declaredConstructors.filter { it.parameterCount == 0 }
    val builder: B = zeroArgConstructors.singleOrNull()?.newInstance() as? B
        ?: throw IllegalArgumentException("${B::class.simpleName} has no zero-arg constructor and cannot be used to create a map of ${K::class.simpleName} and ${V::class.simpleName}.")
    return builder.apply(this).let { it.invoke(it) }
}

/**
 * Builds a map of [K], [V] and adds it to [target] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`.
 *
 * @return the build instance
 */
inline fun <reified B : (B) -> Map<K, V>, reified K : Any, reified V> (B.() -> Unit).buildMapTo(target: MutableMap<in K, in V>): Map<K, V> =
    buildMap().also { target.putAll(it) }

/**
 * Builds a map of [K], [V] and [transform]s it to [U] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`
 * 4) applying [transform].
 *
 * @return the transformed instance
 */
inline fun <reified B : (B) -> Map<K, V>, reified K : Any, reified V, reified U> (B.() -> Unit).buildMap(transform: Map.Entry<K, V>.() -> U): List<U> =
    buildMap().map(transform)

/**
 * Builds a map of [K], [V] and adds the to [U] [transform]ed instance to [target] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`
 * 4) applying [transform].
 *
 * @return the transformed instance
 */
inline fun <reified B : (B) -> Map<K, V>, reified K : Any, reified V, reified U> (B.() -> Unit).buildMapTo(
    target: MutableCollection<in U>,
    transform: Map.Entry<K, V>.() -> U,
): List<U> = buildMap(transform).also { target.addAll(it) }



/**
 * Convenience type to easier use [buildMap] accepts.
 */
typealias MapBuilderInit<K, V> = MapBuilder<K, V>.() -> Unit

open class MapBuilder<K, V> : (MapBuilder<K, V>) -> Map<K, V> by { it.map } {

    protected val map: MutableMap<K, V> = mutableMapOf()

    companion object {
        inline fun <reified B : MapBuilder<K, V>, reified K, reified V> build(init: B.() -> Unit): Map<K, V> = init.build()
    }

    infix fun K.to(value: V) {
        map[this] = value
    }

    operator fun Map<K, V>.unaryPlus() {
        map.putAll(this)
    }

    operator fun Map<K, V>.unaryMinus() {
        forEach { (key, value) -> map.remove(key, value) }
    }
}
