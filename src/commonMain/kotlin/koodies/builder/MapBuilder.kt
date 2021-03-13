package koodies.builder

import koodies.asString
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.MapBuildingContext
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build maps with keys of type [K] and values of type [V].
 *
 * The most convenient way to actually build a map is using [buildMap].
 */
public open class MapBuilder<K, V> : Builder<Init<MapBuildingContext<K, V>>, Map<K, V>> {

    /**
     * A context to collection all elements added by means
     * of the [ListBuildingContext].
     */
    protected class BackedMapBuildingContext<K, V>(
        /**
         * The mutable map to which all context operations should be delegated.
         */
        public val map: MutableMap<K, V> = mutableMapOf(),
    ) : MapBuildingContext<K, V>, MutableMap<K, V> by map

    override fun invoke(init: Init<MapBuildingContext<K, V>>): Map<K, V> {
        return BackedMapBuildingContext<K, V>().apply(init).map
    }

    override fun toString(): String = asString()

    public companion object {

        /**
         * Builds a map with keys of type [K] and values of type [V].
         */
        @OptIn(ExperimentalTypeInference::class)
        public operator fun <K, V> invoke(@BuilderInference init: Init<MapBuildingContext<K, V>>): Map<K, V> = MapBuilder<K, V>().invoke(init)
    }
}

/**
 * Builds a map of with keys of type [K] and values of type [V].
 */
@OptIn(ExperimentalTypeInference::class)
public fun <K, V> buildMap(@BuilderInference init: Init<MapBuildingContext<K, V>>): Map<K, V> {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    return MapBuilder<K, V>().invoke(init)
}
