package koodies.builder

import koodies.asString
import koodies.builder.MapBuilder.Companion.buildMap
import koodies.builder.context.MapBuildingContext
import koodies.builder.context.StatefulContext
import koodies.builder.context.StatefulMapBuildingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build maps with keys of type [K] and values of type [V].
 *
 * The most convenient way to actually build a map is using [buildMap].
 */
open class MapBuilder<K, V> : StatefulContextBuilder<MapBuildingContext<K, V>, Map<K, V>, Map<K, V>> {  // TODO get rid of indermediate map<k,v>

    override val statefulContext: StatefulContext<MapBuildingContext<K, V>, Map<K, V>> get() = StatefulMapBuildingContext()

    override val transform: Map<K, V>.() -> Map<K, V> = { this }

    @OptIn(ExperimentalTypeInference::class)
    companion object {
        /**
         * Builds a map of with keys of type [K] and values of type [V].
         */
        fun <K, V> buildMap(@BuilderInference init: Init<MapBuildingContext<K, V>>) = MapBuilder<K, V>().build(init)

        /**
         * Builds a map with keys of type [K] and values of type [V].
         */
        operator fun <K, V> invoke(@BuilderInference init: Init<MapBuildingContext<K, V>>) = MapBuilder<K, V>().build(init)
    }

    override fun toString(): String = asString(::statefulContext)
}
