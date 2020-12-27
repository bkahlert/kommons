@file:Suppress("PropertyName")

package koodies.builder

import java.util.EnumSet

/**
 * Builder of an [EnumSet] of any enum type [T].
 *
 * @see EnumSetStatefulBuilderSamples.directUse
 * @see EnumSetStatefulBuilderSamples.indirectUse
 */
class EnumSetStatefulBuilder<T : Enum<T>>(private val picked: MutableSet<T>) {
    companion object {
        fun <T : Enum<T>> build(init: EnumSetStatefulBuilderInit<T>): EnumSet<T> =
            mutableSetOf<T>().also { EnumSetStatefulBuilder(it).init() }.let { EnumSet.copyOf(it) }
    }

    val T.picked get() = this@EnumSetStatefulBuilder.picked.add(this)
    fun T.pick() = this@EnumSetStatefulBuilder.picked.add(this)
}

/**
 * Type [EnumSetStatefulBuilder.build] accepts.
 */
typealias EnumSetStatefulBuilderInit<T> = EnumSetStatefulBuilder<T>.() -> Unit

@Suppress("UNUSED_VARIABLE", "unused")
private object EnumSetStatefulBuilderSamples {

    enum class Features {
        FeatureA, FeatureB, FeatureC
    }

    fun directUse() {

        val pair = EnumSetStatefulBuilder.build<Features> {
            Features.FeatureA.picked
            Features.FeatureC.pick()
        }

    }

    fun indirectUse() {

        fun statefulBuilderAcceptingFunction(init: EnumSetStatefulBuilderInit<Features>) {
            val enumSet = EnumSetStatefulBuilder.build(init)
            println("Et voilà, $enumSet")
        }

        val enumSet = statefulBuilderAcceptingFunction {
            Features.FeatureA.picked
            Features.FeatureC.pick()
        }

    }
}
