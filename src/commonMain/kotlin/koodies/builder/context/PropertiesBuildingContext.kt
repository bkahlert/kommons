package koodies.builder.context

import koodies.builder.Builder
import koodies.builder.BuildingProperty
import koodies.builder.Provider
import koodies.builder.ProvidingProperty
import koodies.builder.StoringProperty

/**
 * A context that provides methods to create delegated properties with owner [T].
 */
interface PropertiesBuildingContext<T> {
    /**
     * Returns a [BuildingProperty] that accepts an [Init] with an instance of [BC] as the
     * receiver object.
     *
     * In other words: This property can be invoked that same way a typical builder
     * function with receiver object is called.
     */
    fun <C, R, V> building(builder: Provider<Builder<C, R, V>>) = BuildingProperty<T, C, R, V>(builder)

    /**
     * Returns a [ProvidingProperty] that accepts a [Provider] that returns instances of [T].
     */
    fun <V> providing() = ProvidingProperty<T, V>()

    /**
     * Returns a [StoringProperty] that accepts a [Provider] that returns instances of [T].
     */
    fun <V> storing() = StoringProperty<T, V>()
}
