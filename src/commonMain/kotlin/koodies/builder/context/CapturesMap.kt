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
public class CapturesMap {

    private val defaults = mutableMapOf<String, Deferred<*>>()
    private val history = mutableListOf<Pair<String, Deferred<*>>>()

    public fun setDefault(property: KProperty<*>, deferred: Deferred<*>) {
        defaults[property.name] = deferred
    }

    public fun add(property: KProperty<*>, deferred: Deferred<*>) {
        history.add(property.name to deferred)
    }

    public fun mostRecent(property: KProperty<*>): Deferred<*>? = mostRecent(property.name)
    public fun mostRecent(propertyName: String): Deferred<*>? =
        history.lastOrNull { it.first == propertyName }?.second ?: defaults[propertyName]

    /**
     * Returns all [Deferred] evaluations for all properties matching the
     * specified [filter] or an empty list. Eventually set defaults are ignored.
     */
    public fun getAll(filter: Pair<String, Deferred<*>>.() -> Boolean = { true }): List<Deferred<*>> =
        history.filter { it.filter() }.map { it.second }

    /**
     * Returns all [Deferred] evaluations for the given [property] or
     * an empty list. An eventually set default is ignored.
     */
    public inline fun <reified R> getAll(property: KProperty<*>): List<Deferred<out R>> =
        getAll(property.name)

    /**
     * Returns all [Deferred] evaluations for the given [propertyName] or
     * an empty list. An eventually set default is ignored.
     */
    public inline fun <reified R> getAll(propertyName: String): List<Deferred<out R>> =
        getAll { first == propertyName }.map { it.letContent { this as R } }

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * given [property] or `null` if no invocation is stored.
     */
    public inline operator fun <reified R> get(property: KProperty<*>): Deferred<out R?> =
        mostRecent(property.name)?.letContent { this@letContent as R? } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property] or `null` if no invocation is stored.
     */
    public fun <R> get(property: KProperty<*>, cast: (Any?) -> R?): Deferred<out R?> =
        mostRecent(property.name)?.letContent { cast(this@letContent) } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName] or `null` if no invocation is stored.
     */
    public inline operator fun <reified R> get(propertyName: String): Deferred<out R?> =
        mostRecent(propertyName)?.letContent { this@letContent as R? } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName] or `null` if no invocation is stored.
     */
    public fun <R> get(propertyName: String, cast: (Any?) -> R?): Deferred<out R?> =
        mostRecent(propertyName)?.letContent { cast(this@letContent) } ?: deferNull()

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    public inline fun <reified R> getOrDefault(property: KProperty<*>, defaultValue: R): Deferred<out R> =
        getOrDefault(property.name, defaultValue)

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    public inline fun <reified R> getOrDefault(property: KProperty<*>, crossinline defaultValue: () -> R): Deferred<out R> =
        getOrDefault(property.name, defaultValue)

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    public inline fun <reified R> getOrDefault(propertyName: String, defaultValue: R): Deferred<out R> =
        get<R>(propertyName).letContent { this ?: defaultValue }

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName]. If no invocation [Deferred] evaluates to the specified [defaultValue].
     */
    public inline fun <reified R> getOrDefault(propertyName: String, crossinline defaultValue: () -> R): Deferred<out R> =
        get<R>(propertyName).letContent { this ?: defaultValue() }

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [property]. If no invocation is stored the optionally provided exception is thrown.
     */
    public inline fun <reified R> getOrThrow(property: KProperty<*>, noinline lazyException: (() -> Throwable)? = null): Deferred<out R> =
        getOrThrow(property.name, lazyException ?: { defaultException(property) })

    /**
     * Returns the [Deferred] evaluation of the most recent invocation of the
     * property with the given [propertyName]. If no invocation is stored the optionally provided exception is thrown.
     */
    public inline fun <reified R> getOrThrow(propertyName: String, noinline lazyException: (() -> Throwable)? = null): Deferred<out R> =
        get<R>(propertyName).letContent { if (this is R) this else throw (lazyException?.invoke() ?: defaultException(propertyName)) }


    /**
     * Evaluates all captures for all properties and returns all results
     * of type [T].
     *
     * If no captures are found, the returned list is empty.
     * An eventually defined default is ignored.
     */
    public inline fun <reified T> evalAll(): List<T> = getAll { true }.map { it.evaluate() }.filterIsInstance<T>()

    /**
     * Evaluates all captures for `this` property and returns them.
     *
     * If no captures are found, the returned list is empty.
     * An eventually defined default is ignored.
     */
    public inline fun <reified T> KProperty<*>.evalAll(): List<T> = getAll<T>(this).map { it.evaluate() }

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise returns `null`.
     */
    public inline fun <reified T> KProperty<*>.evalOrNull(): T? = get<T>(this).evaluate()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise returns the given [default].
     */
    public inline fun <reified T> KProperty<*>.evalOrDefault(default: T): T = getOrDefault(this@evalOrDefault, default).evaluate()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * applies the given [transform] to its evaluation and returns it.
     * Otherwise [transform] applied to the given [default] is returned.
     */
    public inline fun <reified T, reified U> KProperty<*>.evalOrDefault(default: T, transform: T.() -> U): U = evalOrDefault(default).transform()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise returns the result of the
     * given [default].
     */
    public inline fun <reified T> KProperty<*>.evalOrDefault(noinline default: () -> T): T = getOrDefault(this@evalOrDefault, default).evaluate()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * applies the given [transform] to its evaluation and returns it.
     * Otherwise [transform] applied to the result of the given [default] is returned.
     */
    public inline fun <reified T, reified U> KProperty<*>.evalOrDefault(noinline default: () -> T, transform: T.() -> U): U = evalOrDefault(default).transform()

    /**
     * Checks if a capture can be found for `this` property and if so,
     * evaluates and returns it. Otherwise throws a [NoSuchElementException].
     */
    public inline fun <reified T> KProperty<*>.eval(): T = getOrThrow<T>(this).evaluate()

    override fun toString(): String = asString(::defaults, ::history)

    public companion object {
        /**
         * Default exception for problems evaluating a deferred invocation for the given [property].
         */
        public fun defaultException(property: KProperty<*>): NoSuchElementException =
            NoSuchElementException("No deferred invocations found for ${property.toString().substringBefore(":")}")

        /**
         * Default exception for problems evaluating a deferred invocation for the given [property].
         */
        public fun defaultException(propertyName: String): NoSuchElementException =
            NoSuchElementException("No deferred invocations found for property $propertyName.")
    }
}
