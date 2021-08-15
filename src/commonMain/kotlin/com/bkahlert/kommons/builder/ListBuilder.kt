package com.bkahlert.kommons.builder

import com.bkahlert.kommons.asString
import com.bkahlert.kommons.builder.context.ListBuildingContext
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * Builder to build lists of type [E].
 *
 * The most convenient way to actually build a list is using [buildList].
 */
public open class ListBuilder<E> : Builder<Init<ListBuildingContext<E>>, List<E>> {

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
            list += element
        }
    }

    override fun invoke(init: Init<ListBuildingContext<E>>): List<E> =
        BackedListBuildingContext<E>().apply(init).list

    override fun toString(): String = asString()

    public companion object {

        public operator fun <E> invoke(@BuilderInference init: Init<ListBuildingContext<E>>): List<E> =
            ListBuilder<E>().invoke(init)
    }
}

/**
 * Builds a list of type [E] as specified by [init].
 */
public fun <E> buildList(@BuilderInference init: Init<ListBuildingContext<E>>): List<E> {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    return ListBuilder(init)
}
