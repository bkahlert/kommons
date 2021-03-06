package koodies

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * A [function] that cannot be called if it [isConstrained].
 *
 * If the [function] is called although constraint the result
 * of the last successful invocation is returned.
 */
public class ConstrainedFunction<T, V>(
    private val thisRef: T,
    private val function: T.() -> V?,
    private val isConstrained: () -> Boolean,
) : ReadOnlyProperty<T, () -> V?> {
    private var result: V? = null
    public operator fun invoke(): V? {
        if (!isConstrained()) {
            result = thisRef.function()
        }
        return result
    }

    override fun getValue(thisRef: T, property: KProperty<*>): () -> V? = this::invoke
}

/**
 * Constraints the specified [function] to be only invocable as long as [isConstrained] returns `false`.
 *
 * If the [function] is called although constraint the result
 * of the last successful invocation is returned.
 */
public fun <T, V> constrained(function: T.() -> V, isConstrained: () -> Boolean): PropertyDelegateProvider<T, ConstrainedFunction<T, V>> =
    PropertyDelegateProvider { thisRef, _ -> ConstrainedFunction(thisRef, function, isConstrained) }

/**
 * Wraps the specified [block] so it cannot be called more than specified by [atMost].
 *
 * On all further calls the last computation's result is returned.
 */
public fun <T, V> callable(atMost: Int, block: T.() -> V): PropertyDelegateProvider<T, ConstrainedFunction<T, V>> {
    var computationCount = 0
    return constrained(block) {
        val i = ++computationCount
        atMost < i
    }
}

/**
 * Wraps the specified [block] so it cannot be called more than once.
 *
 * On all further calls the last computation's result is returned.
 */
public fun <T, V> callableOnce(block: T.() -> V): PropertyDelegateProvider<T, ConstrainedFunction<T, V>> =
    callable(1, block)
