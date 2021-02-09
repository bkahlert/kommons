package koodies.builder.context

/**
 * A [StatefulContext] of which the [state] is a list of all added elements.
 */
class StatefulElementAddingContext<E> : StatefulContext<ElementAddingContext<E>, List<E>> {

    override val state: MutableList<E> = mutableListOf()

    override val context: ElementAddingContext<E> = object : ElementAddingContext<E> {
        override fun add(element: E, vararg elements: E) {
            state.add(element)
            if (elements.isNotEmpty()) addAll(elements)
        }

        override fun addAll(collection: Collection<E>) {
            state.addAll(collection)
        }
    }
}
