package koodies.builder.context

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
    public operator fun E.unaryPlus(): Unit = add(this)

    /**
     * Adds `this` element.
     */
    public operator fun Unit.plus(@BuilderInference element: E): Unit = add(element)

    /**
     * Adds the specified [element].
     */
    public fun add(@BuilderInference element: E)

    /**
     * Adds the specified elements.
     */
    public fun add(element1: E, element2: E, vararg elements: E) {
        add(element1)
        add(element2)
        addAll(elements)
    }

    /**
     * Adds all elements of the specified [collection].
     */
    public fun addAll(@BuilderInference collection: Collection<E>): Unit = collection.forEach { add(it) }

    /**
     * Adds all elements of the specified [array].
     */
    public fun addAll(@BuilderInference array: Array<out E>): Unit = addAll(array.toList())

    /**
     * Adds all elements of the specified [sequence].
     */
    public fun addAll(@BuilderInference sequence: Sequence<E>): Unit = addAll(sequence.toList())

    /**
     * Iterates through `this` map and applies the specified [transform]
     * to each entry. Each element of the returned collection will be added.
     */
    public fun <T> Collection<T>.addAll(@BuilderInference transform: T.() -> Collection<E>): Unit = forEach { addAll(it.transform()) }

    /**
     * Iterates through `this` map and applies the specified [transform]
     * to each entry. Each element of the returned collection will be added.
     */
    public fun <K, V> Map<K, V>.addAll(@BuilderInference transform: Map.Entry<K, V>.() -> Collection<E>): Unit = forEach { addAll(it.transform()) }
}
