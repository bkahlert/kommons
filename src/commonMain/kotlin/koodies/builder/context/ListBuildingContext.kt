package koodies.builder.context

import kotlin.DeprecationLevel.ERROR
import kotlin.experimental.ExperimentalTypeInference

@DslMarker
public annotation class ListBuildingDsl

/**
 * A context that uses the [unaryPlus] `+` to
 * model the semantics of "add element" respectively "add elements".
 *
 * Also a [Unit.plus] is provided to allow for the first element
 * of a line to also start with a `+`.
 */
@OptIn(ExperimentalTypeInference::class)
@ListBuildingDsl
public fun interface ListBuildingContext<E> {
    /**
     * Adds `this` element.
     */
    @BuilderInference
    public operator fun E.unaryPlus(): Unit = add()

    /**
     * Adds `this` element.
     */
    @BuilderInference
    public operator fun Unit.plus(element: E): Unit = element.add()

    /**
     * Adds `this` element.
     */
    @BuilderInference
    public fun E.add(): Unit = add(this)

    /**
     * Adds the specified [element] and if not empty also the specified [elements].
     */
    @BuilderInference
    public fun add(element: E, vararg elements: E)

    /**
     * Adds all elements of the specified [collection].
     */
    @BuilderInference
    public fun addAll(collection: Collection<E>): Unit = collection.forEach { add(it) }

    /**
     * Adds all elements of the specified [array].
     */
    @BuilderInference
    public fun addAll(array: Array<out E>): Unit = addAll(array.toList())

    /**
     * Adds all elements of the specified [sequence].
     */
    @BuilderInference
    public fun addAll(sequence: Sequence<E>): Unit = addAll(sequence.toList())

    /**
     * Iterates through `this` map and applies the specified [transform]
     * to each entry. Each element of the returned collection will be added.
     */
    public fun <T> Collection<T>.addAll(transform: T.() -> Collection<E>): Unit = forEach { addAll(it.transform()) }

    /**
     * Iterates through `this` map and applies the specified [transform]
     * to each entry. Each element of the returned collection will be added.
     */
    public fun <K, V> Map<K, V>.addAll(transform: Map.Entry<K, V>.() -> Collection<E>): Unit = forEach { addAll(it.transform()) }

    /**
     * Adds all elements of `this` collection.
     */
    @Deprecated("delete", level = ERROR)
    public operator fun List<E>.unaryPlus(): Unit = addAll(this)

    /**
     * Adds all elements of `this` array.
     */
    @Deprecated("delete", level = ERROR)
    public operator fun Array<out E>.unaryPlus(): Unit = addAll(this)

    /**
     * Adds all elements of `this` sequence.
     */
    @Deprecated("delete", level = ERROR)
    public operator fun Sequence<E>.unaryPlus(): Unit = addAll(this)
}
