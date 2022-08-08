package com.bkahlert.kommons.test.junit

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store

/**
 * List of [ExtensionContext] instances containing the parent hierarchy of this context,
 * with the first element being the parent of this context
 * and the last element being the root.
 */
public val ExtensionContext.ancestors: List<ExtensionContext>
    get() = parent.map { listOf(it) + it.ancestors }.orElseGet { emptyList() }


/**
 * Returns the [ExtensionContext.Store] that uses
 * the class of [T] as the key for the [Namespace] needed to access and scope the store.
 *
 * [additionalParts] can be provided to render the namespace more specific.
 * The order is significant and parts are compared with [Object.equals].
 */
public inline fun <reified T : Any> ExtensionContext.getStore(
    vararg additionalParts: Any,
): Store = getStore(Namespace.create(T::class.java, *additionalParts))

/**
 * Returns the [ExtensionContext.Store] that uses
 * the class of [T] and the current test as the keys for the [Namespace] needed to access and scope the store.
 *
 * [additionalParts] can be provided to render the namespace more specific.
 * The order is significant and parts are compared with [Object.equals].
 *
 * An exception is thrown if no test is current.
 */
public inline fun <reified T : Any> ExtensionContext.getTestStore(
    vararg additionalParts: Any,
): Store = getStore(Namespace.create(T::class.java, requiredTestMethod, *additionalParts))


/**
 * Get the value of the specified required type [V] that is stored under
 * the supplied [key].
 * @see get
 */
public inline fun <reified V> Store.getTyped(key: Any?): V? = get(key, V::class.java)

/**
 * Get the value of the specified required type that is stored under
 * the supplied [key], or the supplied [defaultValue] if no
 * value is found for the supplied [key] in this store or in an
 * ancestor.
 * @see getOrDefault
 */
public inline fun <reified V> Store.getTypedOrDefault(key: Any?, defaultValue: V): V =
    getOrDefault(key, V::class.java, defaultValue)

/**
 * Get the value of the specified required type [V] that is stored under the
 * supplied [key].
 *
 * If no value is stored in the current [ExtensionContext]
 * for the supplied [key], ancestors of the context will be queried
 * for a value with the same [key] in the [Namespace] used
 * to create this store. If no value is found for the supplied [key],
 * a new value will be computed by the [defaultCreator] (given
 * the [key] as input), stored, and returned.
 * @see getOrComputeIfAbsent
 */
public inline fun <K, reified V> Store.getTypedOrComputeIfAbsent(key: K, crossinline defaultCreator: (K) -> V): V =
    getOrComputeIfAbsent(key, { defaultCreator(it) }, V::class.java)

/**
 * Remove the value of the specified required type [V] that was previously stored
 * under the supplied [key].
 *
 * The value will only be removed in the current [ExtensionContext],
 * not in ancestors.
 *
 * @see remove
 */
public inline fun <reified V> Store.removeTyped(key: Any?): V =
    remove(key, V::class.java)
