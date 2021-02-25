package koodies.builder.context

import kotlin.experimental.ExperimentalTypeInference

@DslMarker
annotation class MapBuildingDsl

/**
 * A context that supports the [MutableMap] API
 * and [to] to put entries in a to be built map.
 */
@OptIn(ExperimentalTypeInference::class)
@MapBuildingDsl
interface MapBuildingContext<K, V> : MutableMap<K, V> {

    /**
     * Adds `this` key and the specified [value] to the map.
     *
     * This method does **not** return a pair to not
     * accidentally put this pair twice which might have unexpected
     * effects, e.g. if the insertion order is important.
     */
    @BuilderInference
    infix fun K.to(value: V)
}
