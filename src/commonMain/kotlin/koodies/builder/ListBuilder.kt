package koodies.builder

/**
 * Builder to build lists of type [E].
 *
 * The most convenient way to actually build a list is using [buildList].
 */
open class ListBuilder<E> : ElementAddingContext<E>, BuildingContextImpl<ListBuilder<E>, List<E>>({ list }) {

    protected val list: MutableList<E> = mutableListOf()

    companion object : DefaultProvider<ListBuilder<*>> {
        /**
         * Builds a list of type [E] as specified by [init].
         */
        inline fun <reified E> buildList(noinline init: Init<ListBuilder<E>>): List<E> = Builder.build(init) { ListBuilder() }
        override fun <E> provideDefault(): ListBuilder<E> = ListBuilder<E>()
    }

    override operator fun E.unaryPlus() {
        list.add(this)
    }

    override operator fun Unit.plus(element: E) {
        list.add(element)
    }

    override operator fun Collection<E>.unaryPlus() {
        list.addAll(this)
    }

    override operator fun Array<out E>.unaryPlus() {
        list.addAll(this)
    }

    override operator fun Sequence<E>.unaryPlus() {
        list.addAll(this)
    }

    override fun toString(): String = "ListBuilder[list=$list]"
}

interface DefaultProvider<T> {
    fun <E> provideDefault(): T
}
