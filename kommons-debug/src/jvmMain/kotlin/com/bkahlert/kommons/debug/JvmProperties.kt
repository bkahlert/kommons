package com.bkahlert.kommons.debug

import java.lang.reflect.AccessibleObject
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

/**
 * A [Map] of this object's properties with each [Map.Entry.key] representing
 * a property name and [Map.Entry.value] the corresponding value.
 */
public actual val Any.properties: Map<String, Any?>
    get() = this::class.members
        .reversed()
        .mapNotNull { prop: KCallable<*> ->
            @Suppress("UNCHECKED_CAST")
            when (prop) {
                is KProperty0<*> -> prop.name to prop.getOrElse { PropertyAccessError(it) }
                is KProperty1<*, *> -> prop.name to prop.getOrElse(this@properties) { PropertyAccessError(it) }
                else -> null
            }
        }.toMap()

/** Gets the value of this property or the result of [onFailure]. */
public actual fun <V> KProperty0<V>.getOrElse(onFailure: (Throwable) -> V): V =
    kotlin.runCatching { get() }.getOrElse(onFailure)

/** Gets the value of this property or the result of [onFailure]. */
public actual fun <T, V> KProperty1<T, V>.getOrElse(receiver: T, onFailure: (Throwable) -> V): V =
    kotlin.runCatching { get(receiver) }.getOrElse { originalException ->
        when (val getter = javaGetter) {
            null -> when (val field = javaField) {
                null -> onFailure(originalException)
                else -> field.value(onFailure) { it.get(receiver) }
            }
            else -> getter.value(onFailure) { it.invoke(receiver) }
        }
    }

private inline fun <T : AccessibleObject, V> T.value(
    noinline onFailure: (Throwable) -> V,
    get: (T) -> Any?,
): V {
    if (!accessible) accessible = true
    return kotlin.runCatching {
        val value = get(this)
        @Suppress("UNCHECKED_CAST")
        value as V
    }.getOrElse(handleNativeFailure(onFailure))
}

private fun <V> handleNativeFailure(onFailure: (Throwable) -> V): (exception: Throwable) -> V = {
    when (it) {
        is InvocationTargetException -> onFailure(it.targetException)
        else -> onFailure(it)
    }
}
