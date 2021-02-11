package koodies.builder

import koodies.asString
import koodies.builder.ListBuilder.Companion.buildList
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.StatefulContext
import koodies.builder.context.StatefulListBuildingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder to build lists of type [E].
 *
 * The most convenient way to actually build a list is using [buildList].
 */
open class ListBuilder<E> : StatefulContextBuilder<ListBuildingContext<E>, List<E>, List<E>> {

    override val statefulContext: StatefulContext<ListBuildingContext<E>, List<E>> get() = StatefulListBuildingContext()

    override val transform: List<E>.() -> List<E> = { this }

    @OptIn(ExperimentalTypeInference::class)
    companion object {
        /**
         * Builds a list of type [E] as specified by [init].
         */
        fun <E> buildList(@BuilderInference init: Init<ListBuildingContext<E>>): List<E> = invoke(init)

        operator fun <E> invoke(@BuilderInference init: Init<ListBuildingContext<E>>): List<E> = ListBuilder<E>().invoke(init)
    }

    override fun toString(): String = asString(::statefulContext)
}
