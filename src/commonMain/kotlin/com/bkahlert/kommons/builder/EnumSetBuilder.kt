@file:Suppress("PropertyName")

package com.bkahlert.kommons.builder

import com.bkahlert.kommons.asString
import com.bkahlert.kommons.builder.context.ListBuildingContext
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * Builder of an [Enum] [Set] of any enum type [E].
 *
 * @see EnumSetBuilderSamples.directUse
 * @see EnumSetBuilderSamples.indirectUse
 */
public open class EnumSetBuilder<E : Enum<E>> : Builder<Init<ListBuildingContext<E>>, Set<E>> {

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

    override fun invoke(init: Init<ListBuildingContext<E>>): Set<E> {
        return BackedListBuildingContext<E>().apply(init).list.toSet()
    }

    override fun toString(): String = asString()

    public companion object {

        public operator fun <E : Enum<E>> invoke(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> = EnumSetBuilder<E>().invoke(init)
    }
}

/**
 * Builds an enum set of enum type [E] as specified by [init].
 */
public fun <E : Enum<E>> buildEnumSet(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> {
    contract { callsInPlace(init, EXACTLY_ONCE) }
    return EnumSetBuilder(init)
}

@Suppress("UNUSED_VARIABLE", "unused")
private object EnumSetBuilderSamples {

    enum class Features {
        FeatureA, FeatureB, FeatureC
    }

    fun directUse() {

        val features = EnumSetBuilder {
            +Features.FeatureA + Features.FeatureC
        }
    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: Init<ListBuildingContext<Features>>) {
            val enumSet = EnumSetBuilder(init)
            println("Et voilà, $enumSet")
        }

        val enumSet = builderAcceptingFunction { +Features.FeatureA + Features.FeatureC }
    }
}
