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
public abstract class BooleanBuilder<C> protected constructor(context: C) :
    StatelessBuilder.PostProcessing<C, BooleanValue, Boolean>(context, { booleanValue() }) {
    public fun interface BooleanValue {
        public fun booleanValue(): Boolean
    }

    /**
     * Builder to "build" booleans using semantic boolean `on` and `off`.
     *
     * ```kotlin
     * BooleanBuilder.OnOff { on }
     * BooleanBuilder.OnOff { off }
     * ```
     */
    public object OnOff : BooleanBuilder<OnOff.Context>(Context) {
        public object Context {
            public val on: BooleanValue = BooleanValue { true }
            public val off: BooleanValue = BooleanValue { false }
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
    public object ON_OFF : BooleanBuilder<ON_OFF.Context>(Context) {
        public object Context {
            public val ON: BooleanValue = BooleanValue { true }
            public val OFF: BooleanValue = BooleanValue { false }
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
    public object YesNo : BooleanBuilder<YesNo.Context>(Context) {
        public object Context {
            public val yes: BooleanValue = BooleanValue { true }
            public val no: BooleanValue = BooleanValue { false }
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
    public object YES_NO : BooleanBuilder<YES_NO.Context>(Context) {
        public object Context {
            public val YES: BooleanValue = BooleanValue { true }
            public val NO: BooleanValue = BooleanValue { false }
        }
    }

    /**
     * If an interface demands for a builder,
     * this one just accepts `true` and `false`.
     */
    public companion object : PseudoBuilder<Boolean>
}
