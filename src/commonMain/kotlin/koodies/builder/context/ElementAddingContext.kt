package koodies.builder.context

import kotlin.experimental.ExperimentalTypeInference

@DslMarker
annotation class ElementAddingDsl

/**
 * A context that uses the [unaryPlus] `+` to
 * model the semantics of "add element" respectively "add elements".
 *
 * Also a [Unit.plus] is provided to allow for the first element
 * of a line to also start with a `+`.
 */
@OptIn(ExperimentalTypeInference::class)
@ElementAddingDsl
interface ElementAddingContext<E> {
    /**
     * Adds `this` element.
     */
    @BuilderInference
    @ElementAddingDsl
    operator fun E.unaryPlus(): Unit = add()

    /**
     * Adds `this` element.
     */
    @BuilderInference
    @ElementAddingDsl
    operator fun Unit.plus(element: E): Unit = element.add()

    /**
     * Adds `this` element.
     */
    @BuilderInference
    @ElementAddingDsl
    fun E.add(): Unit = add(this)

    /**
     * Adds the specified [element] and if not empty also the specified [elements].
     */
    @BuilderInference
    @ElementAddingDsl
    fun add(element: E, vararg elements: E)

    /**
     * Adds all elements of the specified [collection].
     */
    @BuilderInference
    @ElementAddingDsl
    fun addAll(collection: Collection<E>)

    /**
     * Adds all elements of the specified [array].
     */
    @BuilderInference
    @ElementAddingDsl
    fun addAll(array: Array<out E>): Unit = addAll(array.toList())

    /**
     * Adds all elements of the specified [sequence].
     */
    @BuilderInference
    @ElementAddingDsl
    fun addAll(sequence: Sequence<E>): Unit = addAll(sequence.toList())

    /**
     * Iterates through `this` map and applies the specified [transform]
     * to each entry. Each element of the returned collection will be added.
     */
    @ElementAddingDsl
    fun <T> Collection<T>.addAll(transform: T.() -> Collection<E>): Unit = forEach { addAll(it.transform()) }

    /**
     * Iterates through `this` map and applies the specified [transform]
     * to each entry. Each element of the returned collection will be added.
     */
    @ElementAddingDsl
    fun <K, V> Map<K, V>.addAll(transform: Map.Entry<K, V>.() -> Collection<E>): Unit = forEach { addAll(it.transform()) }

    /**
     * Adds all elements of `this` collection.
     */
    @Deprecated("use addAll", replaceWith = ReplaceWith("addAll(this)"))
    @ElementAddingDsl
    operator fun List<E>.unaryPlus(): Unit = addAll(this)

    /**
     * Adds all elements of `this` array.
     */
    @Deprecated("use addAll", replaceWith = ReplaceWith("addAll(this)"))
    @ElementAddingDsl
    operator fun Array<out E>.unaryPlus(): Unit = addAll(this)

    /**
     * Adds all elements of `this` sequence.
     */
    @Deprecated("use addAll", replaceWith = ReplaceWith("addAll(this)"))
    @ElementAddingDsl
    operator fun Sequence<E>.unaryPlus(): Unit = addAll(this)
}
