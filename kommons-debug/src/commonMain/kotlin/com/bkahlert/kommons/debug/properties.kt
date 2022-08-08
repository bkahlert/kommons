package com.bkahlert.kommons.debug

import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

/**
 * A [Map] of this object's properties with each [Map.Entry.key] representing
 * a property name and [Map.Entry.value] the corresponding value.
 */
public expect val Any.properties: Map<String, Any?>

/** Gets the value of this property or `null` otherwise. */
@Suppress("NOTHING_TO_INLINE")
public inline fun <V> KProperty<V>.getOrNull(receiver: Any? = null): V? = getOrElse(receiver) { null }

/** Gets the value of this property or the result of [onFailure]. */
public fun <V> KProperty<V>.getOrElse(receiver: Any? = null, onFailure: (Throwable) -> V): V {
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is KProperty0<*> -> (this as KProperty0<V>).getOrElse(onFailure)
        is KProperty1<*, *> -> (this as KProperty1<Any, V>).getOrElse(requireNotNull(receiver) { "receiver required for this property" }, onFailure)
        else -> onFailure(IllegalStateException("unsupported property type ${renderType()}"))
    }
}

internal data class PropertyAccessError(private val exception: Throwable) {
    override fun toString(): String = "<error:$exception>"
}

/** Gets the value of this property or the result of [onFailure]. */
public expect fun <V> KProperty0<V>.getOrElse(onFailure: (Throwable) -> V): V

/** Gets the value of this property or the result of [onFailure]. */
public expect fun <T, V> KProperty1<T, V>.getOrElse(receiver: T, onFailure: (Throwable) -> V): V
