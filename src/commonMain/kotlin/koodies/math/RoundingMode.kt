package koodies.math

public enum class RoundingMode {

    /**
     * Rounding mode to round away from zero.  Always increments the
     * digit prior to a non-zero discarded fraction.  Note that this
     * rounding mode never decreases the magnitude of the calculated
     * value.
     */
    UP,

    /**
     * Rounding mode to round towards zero.  Never increments the digit
     * prior to a discarded fraction (i.e., truncates).  Note that this
     * rounding mode never increases the magnitude of the calculated value.
     */
    DOWN,

    /**
     * Rounding mode to round towards positive infinity.  If the
     * result is positive, behaves as for `RoundingMode.UP`;
     * if negative, behaves as for `RoundingMode.DOWN`.  Note
     * that this rounding mode never decreases the calculated value.
     */
    CEILING,

    /**
     * Rounding mode to round towards negative infinity.  If the
     * result is positive, behave as for `RoundingMode.DOWN`;
     * if negative, behave as for `RoundingMode.UP`.  Note that
     * this rounding mode never increases the calculated value.
     */
    FLOOR,

    /**
     * Rounding mode to round towards &quot;nearest neighbor&quot;
     * unless both neighbors are equidistant, in which case round up.
     * Behaves as for `RoundingMode.UP` if the discarded
     * fraction is  0.5; otherwise, behaves as for
     * `RoundingMode.DOWN`.  Note that this is the rounding
     * mode commonly taught at school.
     */
    HALF_UP,

    /**
     * Rounding mode to round towards &quot;nearest neighbor&quot;
     * unless both neighbors are equidistant, in which case round
     * down.  Behaves as for `RoundingMode.UP` if the discarded
     * fraction is &gt; 0.5; otherwise, behaves as for
     * `RoundingMode.DOWN`.
     */
    HALF_DOWN,

    /**
     * Rounding mode to round towards the &quot;nearest neighbor&quot;
     * unless both neighbors are equidistant, in which case, round
     * towards the even neighbor.  Behaves as for
     * `RoundingMode.HALF_UP` if the digit to the left of the
     * discarded fraction is odd; behaves as for
     * `RoundingMode.HALF_DOWN` if it's even.  Note that this
     * is the rounding mode that statistically minimizes cumulative
     * error when applied repeatedly over a sequence of calculations.
     * It is sometimes known as &quot;Banker&#39;s rounding,&quot; and is
     * chiefly used in the USA.  This rounding mode is analogous to
     * the rounding policy used for `float` and `double`
     * arithmetic in Java.
     */
    HALF_EVEN,

    /**
     * Rounding mode to assert that the requested operation has an exact
     * result, hence no rounding is necessary.  If this rounding mode is
     * specified on an operation that yields an inexact result, an
     * `ArithmeticException` is thrown.
     */
    UNNECESSARY
}
