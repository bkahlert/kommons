package koodies.builder.context

import koodies.asString

/**
 * A [StatefulContext] of which the [state] is a list of all added elements.
 */
class StatefulListBuildingContext<E> : StatefulContext<ListBuildingContext<E>, List<E>> {

    override val state: MutableList<E> = mutableListOf()

    override val context: ListBuildingContext<E> = object : ListBuildingContext<E> {
        override fun add(element: E, vararg elements: E) {
            state.add(element)
            if (elements.isNotEmpty()) addAll(elements)
        }

        override fun addAll(collection: Collection<E>) {
            state.addAll(collection)
        }
    }

    override fun toString(): String = asString(::state)
}
