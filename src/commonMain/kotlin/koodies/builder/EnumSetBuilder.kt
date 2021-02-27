@file:Suppress("PropertyName")

package koodies.builder

import koodies.asString
import koodies.builder.context.ListBuildingContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Builder of an [EnumSet] of any enum type [E].
 *
 * @see EnumSetBuilderSamples.directUse
 * @see EnumSetBuilderSamples.indirectUse
 */
open class EnumSetBuilder<E : Enum<E>> : Builder<Init<ListBuildingContext<E>>, Set<E>> {

    /**
     * A context to collection all elements added by means
     * of the [ListBuildingContext].
     */
    protected class BackedListBuildingContext<E>(
        /**
         * The mutable list to which all context operations should be delegated.
         */
        val list: MutableList<E> = mutableListOf(),
    ) : ListBuildingContext<E> {
        override fun add(element: E, vararg elements: E) {
            list.add(element)
            list.addAll(elements.toList())
        }
    }

    override fun invoke(init: Init<ListBuildingContext<E>>): Set<E> {
        return BackedListBuildingContext<E>().apply(init).list.toSet()
    }

    override fun toString(): String = asString()

    @OptIn(ExperimentalTypeInference::class)
    companion object {
        /**
         * Builds an enum set of enum type [E] as specified by [init].
         */
        fun <E : Enum<E>> buildEnumSet(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> = invoke(init)

        operator fun <E : Enum<E>> invoke(@BuilderInference init: Init<ListBuildingContext<E>>): Set<E> = EnumSetBuilder<E>().invoke(init)
    }
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
