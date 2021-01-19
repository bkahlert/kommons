package koodies.number

/**
 * Variants of a generalized "rounding" operation.
 *
 * Not only can you round to integer but also fractional numbers (e.g. halves or thirds).
 */
enum class ApproximationMode(val calc: (Double, Double) -> Double) {
    /**
     * If the number is not already rounded the next valid number will be calculated.
     */
    Ceil({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (kotlin.math.ceil(passedNumber / roundTo) * roundTo) }),

    /**
     * If the number is not already rounded the previous valid number will be calculated.
     */
    Floor({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (kotlin.math.floor(passedNumber / roundTo) * roundTo) }),

    /**
     * If the number is not already rounded the valid number that is closer to the number.
     * If both candidates would be equally close, the even candidate will be returned.
     */
    Round({ passedNumber, roundTo -> if (roundTo == 0.0) passedNumber else (kotlin.math.round(passedNumber / roundTo) * roundTo) }),
    ;

    companion object {
        /**
         * Rounds the [Double] value of this number up using the optional [resolution].
         *
         * @param resolution The resolution of this operation. `1` by default. Use `0.5` for example to round to halves.
         */
        fun Number.ceil(resolution: Double = 1.0): Double = Ceil.calc(this.toDouble(), resolution)

        /**
         * Rounds the [Double] value of this number down using the optional [resolution].
         *
         * @param resolution The resolution of this operation. `1` by default. Use `0.5` for example to round to halves.
         */
        fun Number.floor(resolution: Double = 1.0): Double = Floor.calc(this.toDouble(), resolution)

        /**
         * Rounds the [Double] value of this number using the optional [resolution].
         *
         * @param resolution The resolution of this operation. `1` by default. Use `0.5` for example to round to halves.
         */
        fun Number.round(resolution: Double = 1.0): Double = Round.calc(this.toDouble(), resolution)
    }
}
