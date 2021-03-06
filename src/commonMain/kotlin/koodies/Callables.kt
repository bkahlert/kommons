package koodies

import koodies.builder.Builder
import kotlin.reflect.KProperty

/**
 * Returns a callable property that delegates all invocations to the
 * referenced callable and returns the result.
 *
 * This type of delegation in particular useful to make existing functionality
 * re-usable without disclosing the origin resp. without providing a too large
 * scope of functionality.
 *
 * **Example**
 *
 * *Implementation*
 * ```kotlin
 * val function by callable(::someFunction)
 *
 *      👉 reference to function or callable property
 *
 *
 * val memberFunction by callable(SomeClass()::someMemberFunction)
 *
 *      👉 reference to member function / member callable property
 *         of instance of SomeClass
 *
 * ```
 *
 * *Caller Perspective*
 * ```kotlin
 * function(p1, p2)          ➜ someFunction(p1, p2)
 *
 * memberFunction(p1) { … }  ➜ [SomeObject instance].someMemberFunction(p1) { … }
 * ```
 *
 * ***Note:** The [callable] is optional. Shorthand: **`val function by ::someFunction`***
 *
 * ***Note:** References to functions and callable properties can be used,
 *            as are both are function types.*
 *
 * @see <a href="https://kotlinlang.org/spec/overload-resolution.html#resolving-callable-references">Resolving
 * callable references</a>
 */
public fun <T : Function<*>> callable(function: T): CallableProperty<Any?, T> = CallableProperty { _, _ -> function }

/**
 * Creates a callable property that delegates all invocations to `this` function
 * reference:
 *
 * ```kotlin
 *     val function by ::someFunction
 *     val function by SomeClass()::someFunction
 * ```
 *
 * The above code snippet corresponds to:
 *
 * ```kotlin
 *     val function by callable(::someFunction)
 *     val function by callable(SomeClass()::someFunction)
 * ```
 *
 * @see callable
 */
public operator fun <T : Function<*>> T.provideDelegate(thisRef: Any?, property: KProperty<*>): CallableProperty<Any?, T> = callable(this)

/**
 * Returns a callable property that delegates all invocations to the
 * referenced [Builder] and returns the built result.
 *
 * This type of delegation in particular useful to make existing functionality
 * re-usable without disclosing the origin resp. without providing a too large
 * scope of functionality.
 *
 * **Example**
 *
 * *Implementation*
 * ```kotlin
 * val build by callable(ListBuilder<String>())
 * ```
 *
 * *Caller Perspective*
 * ```kotlin
 * val list = build {
 *    +"abc"
 *    add("123")
 *    addAll(iterable)
 * }
 * ```
 *
 * ***Note:** The [callable] is optional. Shorthand: **`val build by ListBuilder<String>()`***
 */
public fun <T : Function<*>, R> callable(builder: Builder<T, R>): CallableProperty<Any?, (T) -> R> =
    CallableProperty { _, _ -> { init: T -> builder.invoke(init) } }

/**
 * Creates a callable property that delegates all invocations to `this` builder:
 *
 * ```kotlin
 *     val build by ListBuilder<String>()
 * ```
 *
 * The above code snippet corresponds to:
 *
 * ```kotlin
 *     val build by callable(ListBuilder<String>())
 * ```
 *
 * @see callable
 */
public operator fun <T : Function<*>, R> Builder<T, R>.provideDelegate(thisRef: Any?, property: KProperty<*>): CallableProperty<Any?, (T) -> R> =
    callable(this@provideDelegate)

/**
 * Convenience interface that can be used for implementing
 * [property like callable][https://kotlinlang.org/spec/overload-resolution.html#callables-and-invoke-convention]
 * delegates that effectively behave like functions.
 *
 * @param T the type of object which owns the delegated functional property.
 * @param R the type of the callable.
 */
public fun interface CallableProperty<in T, out R> {
    /**
     * Returns the functional value of the property for the given object.
     * @param thisRef the object for which the functional value is requested.
     * @param property the metadata for the property.
     * @return the functional property value.
     */
    public operator fun getValue(thisRef: T, property: KProperty<*>): R
}
