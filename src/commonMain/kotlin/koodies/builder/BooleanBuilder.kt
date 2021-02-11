package koodies.builder

import koodies.builder.BooleanBuilder.BooleanValue
import koodies.builder.BooleanBuilder.Companion
import koodies.builder.BooleanBuilder.ON_OFF
import koodies.builder.BooleanBuilder.OnOff

/**
 * Builder to "build" booleans, that is `true` and `false`.
 *
 * [Companion] implements a builder that simply accepts booleans:
 * ```kotlin
 * BooleanBuilder { true }
 * BooleanBuilder { false }
 * ```
 *
 * Furthermore the domain boolean builders [OnOff] and [ON_OFF] are
 * provided:
 * ```kotlin
 * BooleanBuilder.OnOff { on }
 * BooleanBuilder.OnOff { off }
 * ```
 * resp.
 * ```kotlin
 * BooleanBuilder.ON_OFF { ON }
 * BooleanBuilder.ON_OFF { OFF }
 * ```
 *
 * Like all builders boolean builders can be bypassed by providing the final result, e.g. `BooleanProvider(true)`.
 *
 * @see OnOff
 * @see ON_OFF
 */
abstract class BooleanBuilder<C> protected constructor(protected val contextProvider: () -> C) : SlipThroughBuilder<C, BooleanValue, Boolean> {
    fun interface BooleanValue {
        fun booleanValue(): Boolean
    }

    override val context: C get() = contextProvider()
    override val transform: BooleanValue.() -> Boolean = { booleanValue() }

    /**
     * Builder to "build" booleans using semantic boolean `on` and `off`.
     *
     * ```kotlin
     * BooleanBuilder.OnOff { on }
     * BooleanBuilder.OnOff { off }
     * ```
     */
    object OnOff : BooleanBuilder<OnOff.Context>({ Context }) {
        object Context {
            val on = BooleanValue { true }
            val off = BooleanValue { false }
        }
    }

    /**
     * Builder to "build" booleans using semantic boolean `ON` and `OFF`.
     *
     * ```kotlin
     * BooleanBuilder.ON_OFF { ON }
     * BooleanBuilder.ON_OFF { OFF }
     * ```
     */
    object ON_OFF : BooleanBuilder<ON_OFF.Context>({ Context }) {
        object Context {
            val ON = BooleanValue { true }
            val OFF = BooleanValue { false }
        }
    }

    /**
     * Builder to "build" booleans using semantic boolean `yes` and `no`.
     *
     * ```kotlin
     * BooleanBuilder.YesNo { yes }
     * BooleanBuilder.YesNo { no }
     * ```
     */
    object YesNo : BooleanBuilder<YesNo.Context>({ Context }) {
        object Context {
            val yes = BooleanValue { true }
            val no = BooleanValue { false }
        }
    }

    /**
     * Builder to "build" booleans using semantic boolean `YES` and `NO`.
     *
     * ```kotlin
     * BooleanBuilder.YES_NO { YES }
     * BooleanBuilder.YES_NO { NO }
     * ```
     */
    object YES_NO : BooleanBuilder<YES_NO.Context>({ Context }) {
        object Context {
            val YES = BooleanValue { true }
            val NO = BooleanValue { false }
        }
    }

    /**
     * If an interface demands for a builder this builder can be used to provide
     * to provide booleans `true` and `false`.
     */
    companion object : NoopBuilder<Boolean>
}
