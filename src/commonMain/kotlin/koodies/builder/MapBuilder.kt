package koodies.builder

import koodies.asString
import koodies.builder.MapBuilder.Companion.buildMap
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.MapBuildingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build maps with keys of type [K] and values of type [V].
 *
 * The most convenient way to actually build a map is using [buildMap].
 */
open class MapBuilder<K, V> : Builder<Init<MapBuildingContext<K, V>>, Map<K, V>> {

    /**
     * A context to collection all elements added by means
     * of the [ListBuildingContext].
     */
    protected class BackedMapBuildingContext<K, V>(
        /**
         * The mutable map to which all context operations should be delegated.
         */
        val map: MutableMap<K, V> = mutableMapOf(),
    ) : MapBuildingContext<K, V>, MutableMap<K, V> by map

    override fun invoke(init: Init<MapBuildingContext<K, V>>): Map<K, V> {
        return BackedMapBuildingContext<K, V>().apply(init).map
    }

    override fun toString(): String = asString()

    @OptIn(ExperimentalTypeInference::class)
    companion object {
        /**
         * Builds a map of with keys of type [K] and values of type [V].
         */
        fun <K, V> buildMap(@BuilderInference init: Init<MapBuildingContext<K, V>>) = MapBuilder<K, V>().invoke(init)

        /**
         * Builds a map with keys of type [K] and values of type [V].
         */
        operator fun <K, V> invoke(@BuilderInference init: Init<MapBuildingContext<K, V>>) = MapBuilder<K, V>().invoke(init)
    }
}
