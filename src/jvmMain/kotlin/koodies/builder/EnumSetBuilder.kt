@file:Suppress("PropertyName")

package koodies.builder

import java.util.EnumSet

/**
 * Builder of an [EnumSet] of any enum type [T].
 *
 * @see EnumSetBuilderSamples.directUse
 * @see EnumSetBuilderSamples.indirectUse
 */
class EnumSetBuilder<T : Enum<T>> {
    companion object {
        inline fun <reified T : Enum<T>> build(init: EnumSetBuilderInit<T>): EnumSet<T> =
            EnumSetBuilder<T>().init().let { EnumSet.copyOf(it) }

        inline fun <reified T : Enum<T>> buildArray(init: EnumSetBuilderInit<T>): Array<T> =
            EnumSetBuilder<T>().init().let { it.toTypedArray() }
    }

    inline operator fun <reified T> T.unaryPlus(): Set<T> = setOf(this)
    inline operator fun <reified T> Set<T>.plus(element: T): Set<T> = this.union(setOf(element))
    inline operator fun <reified T> Set<T>.plus(elements: Iterable<T>): Set<T> = this.union(elements)
}

/**
 * Type [EnumSetBuilder.build] accepts.
 */
typealias EnumSetBuilderInit<T> = EnumSetBuilder<T>.() -> Set<T>

@Suppress("UNUSED_VARIABLE", "unused")
private object EnumSetBuilderSamples {

    enum class Features {
        FeatureA, FeatureB, FeatureC
    }

    fun directUse() {

        val features = EnumSetBuilder.build<Features> {
            +Features.FeatureA + Features.FeatureC
        }

    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: EnumSetBuilderInit<Features>) {
            val enumSet = EnumSetBuilder.build(init)
            println("Et voilà, $enumSet")
        }

        val enumSet = builderAcceptingFunction { +Features.FeatureA + Features.FeatureC }

    }
}
