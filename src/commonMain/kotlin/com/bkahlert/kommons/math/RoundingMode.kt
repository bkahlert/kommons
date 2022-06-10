package com.bkahlert.kommons.math

import kotlin.math.absoluteValue
import kotlin.math.roundToInt as kotlinRoundToInt
import kotlin.math.roundToLong as kotlinRoundToLong

/**
 * Modes of a generalized "rounding" operation.
 */
public enum class RoundingMode(private val calc: (Double, Double) -> Double) {

    /**
     * Rounding mode to round away from zero.  Always increments the
     * digit prior to a non-zero discarded fraction.  Note that this
     * rounding mode never decreases the magnitude of the calculated
     * value.
     */
    UP({ passedNumber, roundTo ->
        if (roundTo == 0.0) passedNumber else {
            if (passedNumber >= 0) CEILING.calc(passedNumber, roundTo)
            else FLOOR.calc(passedNumber, roundTo)
        }
    }),

    /**
     * Rounding mode to round towards zero.  Never increments the digit
     * prior to a discarded fraction (i.e., truncates).  Note that this
     * rounding mode never increases the magnitude of the calculated value.
     */
    DOWN({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (kotlin.math.truncate(passedNumber / roundTo) / (1.0 / roundTo)) }),

    /**
     * Rounding mode to round towards positive infinity.  If the
     * result is positive, behaves as for [UP];
     * if negative, behaves as for [DOWN].  Note
     * that this rounding mode never decreases the calculated value.
     */
    CEILING({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (kotlin.math.ceil(passedNumber / roundTo) / (1.0 / roundTo)) }),

    /**
     * Rounding mode to round towards negative infinity.  If the
     * result is positive, behave as for [DOWN];
     * if negative, behave as for [UP].  Note that
     * this rounding mode never increases the calculated value.
     */
    FLOOR({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (kotlin.math.floor(passedNumber / roundTo) / (1.0 / roundTo)) }),

    /**
     * Rounding mode to round towards &quot;nearest neighbor&quot;
     * unless both neighbors are equidistant, in which case round up.
     * Behaves as for [UP] if the discarded
     * fraction is  0.5; otherwise, behaves as for
     * [DOWN].  Note that this is the rounding
     * mode commonly taught at school.
     */
    HALF_UP({ passedNumber, roundTo ->
        if (roundTo == 0.0) passedNumber else {
            if (passedNumber.absoluteValue.mod(roundTo) >= roundTo / 2.0) UP.calc(passedNumber, roundTo)
            else DOWN.calc(passedNumber, roundTo)
        }
    }),

    /**
     * Rounding mode to round towards &quot;nearest neighbor&quot;
     * unless both neighbors are equidistant, in which case round
     * down.  Behaves as for [UP] if the discarded
     * fraction is &gt; 0.5; otherwise, behaves as for
     * [DOWN].
     */
    HALF_DOWN({ passedNumber, roundTo ->
        if (roundTo == 0.0) passedNumber else {
            if (passedNumber.absoluteValue.mod(roundTo) > roundTo / 2.0) UP.calc(passedNumber, roundTo)
            else DOWN.calc(passedNumber, roundTo)
        }
    }),

    /**
     * Rounding mode to round towards the &quot;nearest neighbor&quot;
     * unless both neighbors are equidistant, in which case, round
     * towards the even neighbor.  Behaves as for
     * [HALF_UP] if the digit to the left of the
     * discarded fraction is odd; behaves as for
     * [HALF_DOWN] if it's even.  Note that this
     * is the rounding mode that statistically minimizes cumulative
     * error when applied repeatedly over a sequence of calculations.
     * It is sometimes known as &quot;Banker&#39;s rounding,&quot; and is
     * chiefly used in the USA.  This rounding mode is analogous to
     * the rounding policy used for [Float] and [Double]
     * arithmetic in Java.
     */
    HALF_EVEN({ passedNumber, roundTo ->
        if (roundTo == 0.0) passedNumber
        else (kotlin.math.round(passedNumber / roundTo) / (1.0 / roundTo))
    }),

    /**
     * Rounding mode to assert that the requested operation has an exact
     * result, hence no rounding is necessary.  If this rounding mode is
     * specified on an operation that yields an inexact result, an
     * [ArithmeticException] is thrown.
     */
    UNNECESSARY({ _, _ -> throw ArithmeticException() });

    /**
     * Rounds the given [number] to multiples of [resolution] (default: 1.0) according to this [RoundingMode].
     */
    public operator fun invoke(number: Double, resolution: Double = 1.0): Double = calc(number, resolution)
}

/**
 * Rounds the [Double] value of this number up using the optional [resolution].
 *
 * @param resolution The resolution of this operation. `1` by default. Use `0.5` for example to round to halves.
 */
public fun Number.ceil(resolution: Double = 1.0): Double = RoundingMode.CEILING(toDouble(), resolution)

/**
 * Rounds the [Double] value of this number down using the optional [resolution].
 *
 * @param resolution The resolution of this operation. `1` by default. Use `0.5` for example to round to halves.
 */
public fun Number.floor(resolution: Double = 1.0): Double = RoundingMode.FLOOR(toDouble(), resolution)

/**
 * Rounds the [Double] value of this number using the optional [resolution].
 *
 * @param resolution The resolution of this operation. `1` by default. Use `0.5` for example to round to halves.
 */
public fun Number.round(resolution: Double = 1.0): Double = RoundingMode.HALF_EVEN(toDouble(), resolution)

/**
 * Rounds the [Double] value of this number. Ties are rounded towards the even neighbor.
 */
public fun Number.roundToInt(): Int = RoundingMode.HALF_EVEN(toDouble()).kotlinRoundToInt()

/**
 * Rounds the [Double] value of this number. Ties are rounded towards the even neighbor.
 */
public fun Number.roundToLong(): Long = RoundingMode.HALF_EVEN(toDouble()).kotlinRoundToLong()
