package koodies.builder

import koodies.builder.ArrayBuilder.Companion
import koodies.builder.context.ElementAddingContext
import koodies.builder.context.StatefulContext
import koodies.builder.context.StatefulElementAddingContext

/**
 * Builder to creates instances of an [E] typed [Array].
 *
 * If you don't want to provide an instance of `Array<E>` yourself,
 * you can use [Companion.invoke] to create an instance of this builder
 * or [Companion.buildArray] to build an array right away.
 */
open class ArrayBuilder<E>(override val transform: List<E>.() -> Array<E>) : StatefulContextBuilder<ElementAddingContext<E>, List<E>, Array<E>> {

    override val statefulContext: StatefulContext<ElementAddingContext<E>, List<E>> = StatefulElementAddingContext()

    companion object {
        /**
         * Convenience method to build instances of an [E] typed [Array].
         */
        inline fun <reified E> buildArray(noinline init: Init<ElementAddingContext<E>, Unit>): Array<E> = invoke(init)

        /**
         * Constructor mocking inline factory method to allow for the construction
         * of arrays for type [E].
         */
        inline operator fun <reified E> invoke(noinline init: Init<ElementAddingContext<E>, Unit>) = ArrayBuilder<E> { toTypedArray() }.build(init)
    }

}
