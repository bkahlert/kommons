package com.bkahlert.kommons.builder

import com.bkahlert.kommons.asString
import com.bkahlert.kommons.builder.context.ListBuildingContext
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * Builder to build sets of type [E].
 *
 * The most convenient way to actually build a set is using [buildSet].
 */
public open class SetBuilder<E> : Builder<Init<ListBuildingContext<E>>, Set<E>> {

    /**
     * A context to collect all elements added by means
     * of the [ListBuildingContext].
     */
    protected class BackedListBuildingContext<E>(
        /**
         * The mutable list to which all context operations should be delegated.
         */
        public val list: MutableList<E> = mutableListOf(),
    ) : ListBuildingContext<E> {
        override fun add(element: E) {
            list.add(element)
        }
    }

    override fun invoke(init: Init<ListBuildingContext<E>>): Set<E> {
        return BackedListBuildingContext<E>().apply(init).list.toSet()
    }

    override fun toString(): String = asString()

    public companion object {

        public operator fun <E> invoke(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> = SetBuilder<E>().invoke(init)
    }
}

/**
 * Builds a set of type [E] as specified by [init].
 */
public fun <E> buildSet(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    return SetBuilder(init)
}
