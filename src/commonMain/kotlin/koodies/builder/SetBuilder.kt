package koodies.builder

import koodies.asString
import koodies.builder.SetBuilder.Companion.buildSet
import koodies.builder.context.ListBuildingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build sets of type [E].
 *
 * The most convenient way to actually build a set is using [buildSet].
 */
public open class SetBuilder<E> : Builder<Init<ListBuildingContext<E>>, Set<E>> {

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

    override fun invoke(init: Init<ListBuildingContext<E>>): Set<E> {
        return BackedListBuildingContext<E>().apply(init).list.toSet()
    }

    override fun toString(): String = asString()

    @OptIn(ExperimentalTypeInference::class)
    public companion object {
        /**
         * Builds a set of type [E] as specified by [init].
         */
        public fun <E> buildSet(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> = invoke(init)

        public operator fun <E> invoke(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> = SetBuilder<E>().invoke(init)
    }
}
