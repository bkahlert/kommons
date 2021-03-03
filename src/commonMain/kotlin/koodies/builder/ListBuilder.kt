package koodies.builder

import koodies.asString
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.ListBuildingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build lists of type [E].
 *
 * The most convenient way to actually build a list is using [buildList].
 */
public open class ListBuilder<E> : Builder<Init<ListBuildingContext<E>>, List<E>> {

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

    override fun invoke(init: Init<ListBuildingContext<E>>): List<E> {
        return BackedListBuildingContext<E>().apply(init).list
    }

    override fun toString(): String = asString()

    @OptIn(ExperimentalTypeInference::class)
    public companion object {
        /**
         * Builds a list of type [E] as specified by [init].
         */
        public fun <E> buildList(@BuilderInference init: Init<ListBuildingContext<E>>): List<E> = invoke(init)

        /**
         * Builds a list of type [E] as specified by [init].
         */
        public fun <P1, E> buildList(p1: P1, @BuilderInference init: Init1<P1, ListBuildingContext<E>>): List<E> = invoke(p1, init)

        public operator fun <E> invoke(@BuilderInference init: Init<ListBuildingContext<E>>): List<E> = ListBuilder<E>().invoke(init)

        public operator fun <P1, E> invoke(p1: P1, @BuilderInference init: Init1<P1, ListBuildingContext<E>>): List<E> = ListBuilder<E>().build { init(p1) }
    }
}
