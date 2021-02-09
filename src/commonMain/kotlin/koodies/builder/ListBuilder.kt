package koodies.builder

import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.ElementAddingContext
import koodies.builder.context.StatefulContext
import koodies.builder.context.StatefulElementAddingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build lists of type [E].
 *
 * The most convenient way to actually build a list is using [buildList].
 */
open class ListBuilder<E> : StatefulContextBuilder<ElementAddingContext<E>, List<E>, List<E>> {
    override val statefulContext: StatefulContext<ElementAddingContext<E>, List<E>> = StatefulElementAddingContext()

    override val transform: List<E>.() -> List<E> = { this }

    @OptIn(ExperimentalTypeInference::class)
    companion object {
        /**
         * Builds a list of type [E] as specified by [init].
         */
        fun <E> buildList(@BuilderInference init: Init<ElementAddingContext<E>, Unit>): List<E> = invoke(init)

        operator fun <E> invoke(@BuilderInference init: Init<ElementAddingContext<E>, Unit>) = ListBuilder<E>().build(init)
    }
}
