package koodies

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Delegates {

    /**
     * Returns a property delegate for a read/write property that stores set values of type [T] in a [StoredValueHistory].
     *
     * @param initialValue the initial value of the property.
     * @param onChange if specified, will be called with a [StoredValueHistory] of so far set values and the new value.
     * If [onChange] returns `true` the new value will be added to the history. Otherwise it will be discarded.
     */
    inline fun <T> storing(
        initialValue: T,
        crossinline onChange: (property: KProperty<*>, history: StoredValueHistory<T>, newValue: T) -> Boolean = { _, _, _ -> true },
    ): ReadWriteProperty<Any?, T> = object : StoringProperty<T>(initialValue) {
        override fun onChange(property: KProperty<*>, history: StoredValueHistory<T>, newValue: T): Boolean = onChange(property, history, newValue)
    }

    /**
     * Returns a property delegate for a function that accepts values of type [T] and stores them in a [StoredValueHistory].
     *
     * @param initialValue the initially stored value of the function-like property.
     * @param onChange if specified, will be called with a [StoredValueHistory] of so far set values and the new value.
     * If [onChange] returns `true` the new value will be added to the history. Otherwise it will be discarded.
     */
    inline fun <T> storingFunction(
        initialValue: T,
        crossinline onChange: (property: KProperty<*>, history: StoredValueHistory<T>, newValue: T) -> Boolean = { _, _, _ -> true },
    ): ReadOnlyProperty<Any?, Function1<T, Unit>> = object : StoringFunction<T>(initialValue) {
        override fun onChange(property: KProperty<*>, history: StoredValueHistory<T>, newValue: T): Boolean = onChange(property, history, newValue)
    }
}

/**
 * A history of stored values. The last element is the [mostRecentValue].
 */
open class StoredValueHistory<T>(private val initialValue: T, backingList: MutableList<T> = mutableListOf()) : List<T> by backingList {
    private val backingList = backingList.apply { add(initialValue) }

    /**
     * Adds the specified [value]
     */
    fun push(value: T): StoredValueHistory<T> = also { backingList.add(value) }

    /**
     * The most recently stored value or the [initialValue]
     * if no value has been stored yet.
     */
    val mostRecentValue: T get() = backingList.last()
}

/**
 * A read-write property that stores set values of type [T] in a [StoredValueHistory].
 *
 * If specified, [onChange] will be called with a [StoredValueHistory] of so far set values and the new value.
 * If [onChange] returns `true` the new value will be added to the history. Otherwise it will be discarded.
 */
abstract class StoringProperty<T>(
    initialValue: T,
) : ReadWriteProperty<Any?, T> {
    private val history: StoredValueHistory<T> = StoredValueHistory(initialValue)

    /**
     * Callback that decides if the [newValue] is added to the [history].
     */
    abstract fun onChange(property: KProperty<*>, history: StoredValueHistory<T>, newValue: T): Boolean

    /**
     * Returns the most recently stored value.
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = history.mostRecentValue

    /**
     * Adds the specified [value] to the [history].
     */
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (onChange(property, history, value)) history.push(value)
    }
}


/**
 * A function property that accepts values of type [T] and stores them in a [StoredValueHistory].
 *
 * If specified, [onChange] will be called with a [StoredValueHistory] of so far set values and the new value.
 * If [onChange] returns `true` the new value will be added to the history. Otherwise it will be discarded.
 */
abstract class StoringFunction<T>(
    initialValue: T,
) : ReadOnlyProperty<Any?, Function1<T, Unit>> {
    private val history: StoredValueHistory<T> = StoredValueHistory(initialValue)

    /**
     * Callback that decides if the [newValue] is added to the [history].
     */
    abstract fun onChange(property: KProperty<*>, history: StoredValueHistory<T>, newValue: T): Boolean

    /**
     * Returns a function that accepts an instance of [T].
     *
     * If this function is called, the provided argument is added to the [history].
     */
    override fun getValue(thisRef: Any?, property: KProperty<*>): Function1<T, Unit> = { value ->
        if (onChange(property, history, value)) history.push(value)
    }
}
