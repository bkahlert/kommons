package koodies.builder

/**
 * A context that uses the [unaryPlus] `+` to
 * model the semantics of "add element" respectively "add elements".
 *
 * Also a [Unit.plus] is provided to allow for the first element
 * of a line to also start with a `+`.
 */
interface ElementAddingContext<E> {
    /**
     * Adds `this` element.
     */
    operator fun E.unaryPlus()

    /**
     * Adds `this` element.
     */
    operator fun Unit.plus(element: E)

    /**
     * Adds all elements of `this` collection.
     */
    operator fun Collection<E>.unaryPlus()

    /**
     * Adds all elements of `this` array.
     */
    operator fun Array<out E>.unaryPlus()

    /**
     * Adds all elements of `this` sequence.
     */
    operator fun Sequence<E>.unaryPlus()
}
