package koodies.builder.context

import koodies.asString
import kotlin.experimental.ExperimentalTypeInference

/**
 * A [StatefulContext] of which the [state] is a map of all put elements.
 */
class StatefulMapBuildingContext<K, V> : StatefulContext<MapBuildingContext<K, V>, Map<K, V>> {

    override val state: MutableMap<K, V> = mutableMapOf()

    @OptIn(ExperimentalTypeInference::class)
    override val context: MapBuildingContext<K, V> = object : MapBuildingContext<K, V>, MutableMap<K, V> by state {
        
        @BuilderInference
        override fun K.to(value: V) {
            put(this, value)
        }
    }

    override fun toString(): String = asString(::state)
}
