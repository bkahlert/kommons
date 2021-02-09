package koodies.builder.context

import kotlin.reflect.KProperty

/**
 * A [PropertiesBuildingContext] that captures all created delegated properties with owner [T]
 * that are of type [P].
 */
interface CapturingPropertiesBuildingContext<T, P> : PropertiesBuildingContext<T> {
    /**
     * Delegate provider that notifies [handleDelegate] of created delegates of type [P].
     */
    operator fun <D : P> D.provideDelegate(thisRef: T, property: KProperty<*>): D = this.also { handleDelegate(thisRef, property, it) }

    /**
     * Callback function that is notified of created delegates of type [P].
     */
    fun handleDelegate(thisRef: T, property: KProperty<*>, delegate: P)
}
