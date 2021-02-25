package koodies.builder.context

import koodies.Deferred
import koodies.asString
import koodies.deferNull
import koodies.letContent
import kotlin.reflect.KProperty

/**
 * Helper context to easy access the evaluation result of [Deferred] delegates using
 * the corresponding delegated property.
 */
class CapturesMap(val mappings: MutableMap<KProperty<*>, Deferred<*>> = mutableMapOf()) {

    operator fun Map<KProperty<*>, Deferred<*>>.get(propertyName: String): Deferred<*>? = mapKeys { (prop, _) -> prop.name }[propertyName]

    operator fun set(property: KProperty<*>, deferred: Deferred<*>) {
        mappings[property] = deferred
    }

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * given [property] or `null` if no invocation is stored.
     */
    inline operator fun <reified R> get(property: KProperty<*>): Deferred<out R?> = mappings[property.name]?.letContent { this@letContent as R? } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property] or `null` if no invocation is stored.
     */
    fun <R> get(property: KProperty<*>, cast: (Any?) -> R?): Deferred<out R?> = mappings[property.name]?.letContent { cast(this@letContent) } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName] or `null` if no invocation is stored.
     */
    inline operator fun <reified R> get(propertyName: String): Deferred<out R?> = mappings[propertyName]?.letContent { this@letContent as? R } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName] or `null` if no invocation is stored.
     */
    fun <R> get(propertyName: String, cast: (Any?) -> R?): Deferred<out R?> = mappings[propertyName]?.letContent { cast(this@letContent) } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    inline fun <reified R> getOrDefault(property: KProperty<*>, defaultValue: R): Deferred<out R> =
        getOrDefault(property.name, defaultValue)

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    inline fun <reified R> getOrDefault(property: KProperty<*>, crossinline defaultValue: () -> R): Deferred<out R> =
        getOrDefault(property.name, defaultValue)

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    inline fun <reified R> getOrDefault(propertyName: String, defaultValue: R): Deferred<out R> =
        get<R>(propertyName).letContent { this ?: defaultValue }

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    inline fun <reified R> getOrDefault(propertyName: String, crossinline defaultValue: () -> R): Deferred<out R> =
        get<R>(propertyName).letContent { this ?: defaultValue() }

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property]. If no invocation is stored the optionally provided exception is thrown.
     */
    inline fun <reified R> getOrThrow(property: KProperty<*>, noinline lazyException: (() -> Throwable)? = null): Deferred<out R> =
        getOrThrow(property.name, lazyException ?: { defaultException(property) })

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName]. If no invocation is stored the optionally provided exception is thrown.
     */
    inline fun <reified R> getOrThrow(propertyName: String, noinline lazyException: (() -> Throwable)? = null): Deferred<out R> =
        get<R>(propertyName).letContent { if (this is R) this else throw (lazyException?.invoke() ?: defaultException(propertyName)) }

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise returns `null`.
     */
    inline fun <reified T> KProperty<*>.evalOrNull(): T? = get<T>(this).evaluate()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise returns the given [default].
     */
    inline fun <reified T> KProperty<*>.evalOrDefault(default: T): T = getOrDefault(this@evalOrDefault, default).evaluate()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * applies the given [transform] to its evaluation and returns it.
     * Otherwise [transform] applied to the given [default] is returned.
     */
    inline fun <reified T, reified U> KProperty<*>.evalOrDefault(default: T, transform: T.() -> U): U = evalOrDefault(default).transform()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise returns the result of the
     * given [default].
     */
    inline fun <reified T> KProperty<*>.evalOrDefault(noinline default: () -> T): T = getOrDefault(this@evalOrDefault, default).evaluate()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * applies the given [transform] to its evaluation and returns it.
     * Otherwise [transform] applied to the result of the given [default] is returned.
     */
    inline fun <reified T, reified U> KProperty<*>.evalOrDefault(noinline default: () -> T, transform: T.() -> U): U = evalOrDefault(default).transform()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise throws a [NoSuchElementException].
     */
    inline fun <reified T> KProperty<*>.eval(): T = getOrThrow<T>(this).evaluate()

    override fun toString(): String = asString {
        mappings.map { (property, delegate) ->
            property.name to delegate
        }
    }

    companion object {
        /**
         * Default exception for problems evaluating a deferred invocation for the given [property].
         */
        fun defaultException(property: KProperty<*>): NoSuchElementException =
            NoSuchElementException("No deferred evaluation found for ${property.toString().substringBefore(":")}")

        /**
         * Default exception for problems evaluating a deferred invocation for the given [property].
         */
        fun defaultException(propertyName: String): NoSuchElementException =
            NoSuchElementException("No deferred evaluation found for property $propertyName.")
    }
}
