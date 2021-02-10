package koodies.builder.context

import koodies.builder.Builder
import koodies.builder.BuildingProperty
import koodies.builder.Init
import koodies.builder.ListBuilder
import koodies.builder.Provider
import koodies.builder.ProvidingProperty
import koodies.builder.StoringProperty

/**
 * A context that provides methods to create delegated properties.
 */
interface PropertiesBuildingContext {
    /**
     * Returns a [BuildingProperty] that accepts an [Init] with an instance of [BC] as the
     * receiver object.
     *
     * In other words: This property can be invoked that same way a typical builder
     * function with receiver object is called.
     */
    fun <C, T> building(initialValue: T, builder: Provider<Builder<C, Unit, T>>) = BuildingProperty(initialValue, builder)
    fun <C, T> building(builder: Provider<Builder<C, Unit, T?>>) = BuildingProperty(null, builder)
    fun <C, T> listBuilding(builder: Provider<Builder<C, Unit, List<T>>>) = BuildingProperty(emptyList(), builder)
    fun <T> listBuilding() = BuildingProperty(emptyList()) { ListBuilder<T>() }

    /**
     * Returns a [ProvidingProperty] that accepts a [Provider] that returns instances of [T].
     */
    fun <T> providing(initialValue: T) = ProvidingProperty(initialValue)
    fun <T> providing() = ProvidingProperty<T?>(null)

    /**
     * Returns a [StoringProperty] that accepts a [Provider] that returns instances of [T].
     */
    fun <T> storing(initialValue: T) = StoringProperty(initialValue)
    fun <T> storing() = StoringProperty<T?>(null)
}
