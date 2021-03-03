package koodies.builder

import koodies.asString
import koodies.builder.ArrayBuilder.Companion
import koodies.builder.context.ListBuildingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to creates instances of an [E] typed [Array].
 *
 * *The primary constructor is made private so that new instances are created
 * using [Companion.invoke] which allows type variable [E] to be reified.*
 *
 * If you must you can call [Companion.createInstance] for a non-reifying
 * instantiation.
 */
public open class ArrayBuilder<E> private constructor(public val transform: List<E>.() -> Array<E>) :
    Builder<Init<ListBuildingContext<E>>, Array<E>> {

    /**
     * A context to collection all elements added by means
     * of the [ListBuildingContext].
     */
    protected class BackedListBuildingContext<E>(
        /**
         * The mutable list to which all context operations should be delegated.
         */
        public val list: MutableList<E> = mutableListOf(),
    ) : ListBuildingContext<E> {
        override fun add(element: E, vararg elements: E) {
            list.add(element)
            list.addAll(elements.toList())
        }
    }

    override fun invoke(init: Init<ListBuildingContext<E>>): Array<E> {
        return BackedListBuildingContext<E>().apply(init).list.transform()
    }

    override fun toString(): String = asString()

    @OptIn(ExperimentalTypeInference::class)
    public companion object {
        /**
         * Convenience method to build instances of an [E] typed [Array].
         */
        public inline fun <reified E> buildArray(@BuilderInference noinline init: Init<ListBuildingContext<E>>): Array<E> = invoke(init)

        /**
         * Non-reifying method to create new instances of this builder.
         */
        public fun <E> createInstance(transform: List<E>.() -> Array<E>): ArrayBuilder<E> = ArrayBuilder(transform)

        /**
         * Creates a new array builder with the array's type variable [E] reified.
         */
        public inline operator fun <reified E> invoke(): ArrayBuilder<E> =
            createInstance { toTypedArray() }

        /**
         * Builds a new [E] typed [Array].
         */
        public inline operator fun <reified E> invoke(@BuilderInference noinline init: Init<ListBuildingContext<E>>): Array<E> =
            createInstance<E> { toTypedArray() }(init)
    }
}
