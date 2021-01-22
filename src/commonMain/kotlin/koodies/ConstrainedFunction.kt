package koodies

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LazyFunction<T, V>(thisRef: T, block: T.() -> V) : ReadOnlyProperty<T, () -> V> {
    private val result by lazy { thisRef.block() }
    operator fun invoke(): V = result
    override fun getValue(thisRef: T, property: KProperty<*>): () -> V = this::invoke
}

inline fun <T, V> lazyFunction(noinline block: T.() -> V): PropertyDelegateProvider<T, LazyFunction<T, V>> =
    PropertyDelegateProvider { thisRef, _ -> LazyFunction(thisRef, block) }

inline fun <T, V> callableOnce(noinline block: T.() -> V) = lazyFunction(block)
