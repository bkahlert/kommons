package koodies.builder

import koodies.builder.ArrayBuilder.Companion
import koodies.builder.Builder.Companion.build

/**
 * Builder to creates instances of an [E] typed [Array].
 *
 * If you don't want to provide an instance of `Array<E>` yourself,
 * you can use [Companion.invoke] to create an instance of this builder
 * or [Companion.buildArray] to build an array right away.
 */
open class ArrayBuilder<E>(transform: Collection<E>.() -> Array<E>) : ElementAddingContext<E>,
    BuildingContextImpl<ArrayBuilder<E>, Array<E>>({
        transform(list)
    }) {

    protected val list: MutableList<E> = mutableListOf()

    companion object {
        /**
         * Convenience method to build instances of an [E] typed [Array].
         */
        inline fun <reified E> buildArray(noinline init: Init<ArrayBuilder<E>>): Array<E> = build(init) { ArrayBuilder { toTypedArray() } }

        /**
         * Constructor mocking inline factory method to allow for the construction
         * of arrays for type [E].
         */
        inline operator fun <reified E> invoke() = ArrayBuilder<E> { toTypedArray() }
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

    override fun toString(): String = "ArrayBuilder[list=$list]"
}
