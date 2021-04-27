package koodies.builder

import koodies.asString
import koodies.builder.ArrayBuilder.Companion
import koodies.builder.context.ListBuildingContext
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
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
     * A context to collect all elements added by means
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

    public companion object {

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
        @OptIn(ExperimentalTypeInference::class)
        public inline operator fun <reified E> invoke(@BuilderInference noinline init: Init<ListBuildingContext<E>>): Array<E> =
            createInstance<E> { toTypedArray() }(init)
    }
}

/**
 * Convenience method to build instances of an [E] typed [Array].
 */
@OptIn(ExperimentalTypeInference::class)
public inline fun <reified E> buildArray(@BuilderInference noinline init: Init<ListBuildingContext<E>>): Array<E> {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    return ArrayBuilder(init)
}
