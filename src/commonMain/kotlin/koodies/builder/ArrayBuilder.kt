package koodies.builder

import koodies.asString
import koodies.builder.ArrayBuilder.Companion
import koodies.builder.context.ListBuildingContext
import koodies.builder.context.StatefulContext
import koodies.builder.context.StatefulListBuildingContext
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
open class ArrayBuilder<E> private constructor(override val transform: List<E>.() -> Array<E>) :
    StatefulContextBuilder<ListBuildingContext<E>, List<E>, Array<E>> {

    override val statefulContext: StatefulContext<ListBuildingContext<E>, List<E>> get() = StatefulListBuildingContext()

    @OptIn(ExperimentalTypeInference::class)
    companion object {
        /**
         * Convenience method to build instances of an [E] typed [Array].
         */
        inline fun <reified E> buildArray(@BuilderInference noinline init: Init<ListBuildingContext<E>>): Array<E> = invoke(init)

        /**
         * Non-reifying method to create new instances of this builder.
         */
        fun <E> createInstance(transform: List<E>.() -> Array<E>): ArrayBuilder<E> = ArrayBuilder(transform)

        /**
         * Creates a new array builder with the array's type variable [E] reified.
         */
        inline operator fun <reified E> invoke(): ArrayBuilder<E> =
            createInstance { toTypedArray() }

        /**
         * Builds a new [E] typed [Array].
         */
        inline operator fun <reified E> invoke(@BuilderInference noinline init: Init<ListBuildingContext<E>>): Array<E> =
            createInstance<E> { toTypedArray() }(init)
    }

    override fun toString(): String = asString(::statefulContext)
}
