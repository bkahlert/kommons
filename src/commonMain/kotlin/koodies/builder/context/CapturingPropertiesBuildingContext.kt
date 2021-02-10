package koodies.builder.context

import kotlin.reflect.KProperty

/**
 * A [PropertiesBuildingContext] that captures all created delegated properties with owner [T]
 * that are of type [P].
 */
interface CapturingPropertiesBuildingContext<T> : PropertiesBuildingContext {
    /**
     * Delegate provider that notifies [handleDelegate] of created delegates of type [T].
     */
    operator fun <D : T> D.provideDelegate(thisRef: Any?, property: KProperty<*>): D = also { handleDelegate(thisRef, property, it) }

    /**
     * Callback function that is notified of created delegates of type [T].
     */
    fun handleDelegate(thisRef: Any?, property: KProperty<*>, delegate: T)
}
