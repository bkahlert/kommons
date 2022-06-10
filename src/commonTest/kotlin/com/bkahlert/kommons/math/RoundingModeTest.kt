package com.bkahlert.kommons.math

import com.bkahlert.kommons.math.RoundingMode.CEILING
import com.bkahlert.kommons.math.RoundingMode.DOWN
import com.bkahlert.kommons.math.RoundingMode.FLOOR
import com.bkahlert.kommons.math.RoundingMode.HALF_DOWN
import com.bkahlert.kommons.math.RoundingMode.HALF_EVEN
import com.bkahlert.kommons.math.RoundingMode.HALF_UP
import com.bkahlert.kommons.math.RoundingMode.UNNECESSARY
import com.bkahlert.kommons.math.RoundingMode.UP
import com.bkahlert.kommons.test.tests
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RoundingModeTest {

    @Test fun round_up() = tests {
        UP(-4.6, 1.0) shouldBe -5.0
        UP(-4.5, 1.0) shouldBe -5.0
        UP(-5.5, 1.0) shouldBe -6.0
        UP(-4.4, 1.0) shouldBe -5.0
        UP(-4.0, 1.0) shouldBe -4.0

        UP(4.6, 1.0) shouldBe 5.0
        UP(4.5, 1.0) shouldBe 5.0
        UP(5.5, 1.0) shouldBe 6.0
        UP(4.4, 1.0) shouldBe 5.0
        UP(4.0, 1.0) shouldBe 4.0

        UP(-4.3, 0.5) shouldBe -4.5
        UP(-4.25, 0.5) shouldBe -4.5
        UP(-5.25, 0.5) shouldBe -5.5
        UP(-4.2, 0.5) shouldBe -4.5
        UP(-4.0, 0.5) shouldBe -4.0

        UP(4.3, 0.5) shouldBe 4.5
        UP(4.25, 0.5) shouldBe 4.5
        UP(5.25, 0.5) shouldBe 5.5
        UP(4.2, 0.5) shouldBe 4.5
        UP(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_down() = tests {
        DOWN(-4.6, 1.0) shouldBe -4.0
        DOWN(-4.5, 1.0) shouldBe -4.0
        DOWN(-5.5, 1.0) shouldBe -5.0
        DOWN(-4.4, 1.0) shouldBe -4.0
        DOWN(-4.0, 1.0) shouldBe -4.0

        DOWN(4.6, 1.0) shouldBe 4.0
        DOWN(4.5, 1.0) shouldBe 4.0
        DOWN(5.5, 1.0) shouldBe 5.0
        DOWN(4.4, 1.0) shouldBe 4.0
        DOWN(4.0, 1.0) shouldBe 4.0

        DOWN(-4.3, 0.5) shouldBe -4.0
        DOWN(-4.25, 0.5) shouldBe -4.0
        DOWN(-5.25, 0.5) shouldBe -5.0
        DOWN(-4.2, 0.5) shouldBe -4.0
        DOWN(-4.0, 0.5) shouldBe -4.0

        DOWN(4.3, 0.5) shouldBe 4.0
        DOWN(4.25, 0.5) shouldBe 4.0
        DOWN(5.25, 0.5) shouldBe 5.0
        DOWN(4.2, 0.5) shouldBe 4.0
        DOWN(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_ceiling() = tests {
        CEILING(-4.6, 1.0) shouldBe -4.0
        CEILING(-4.5, 1.0) shouldBe -4.0
        CEILING(-5.5, 1.0) shouldBe -5.0
        CEILING(-4.4, 1.0) shouldBe -4.0
        CEILING(-4.0, 1.0) shouldBe -4.0

        CEILING(4.6, 1.0) shouldBe 5.0
        CEILING(4.5, 1.0) shouldBe 5.0
        CEILING(5.5, 1.0) shouldBe 6.0
        CEILING(4.4, 1.0) shouldBe 5.0
        CEILING(4.0, 1.0) shouldBe 4.0

        CEILING(-4.3, 0.5) shouldBe -4.0
        CEILING(-4.25, 0.5) shouldBe -4.0
        CEILING(-5.25, 0.5) shouldBe -5.0
        CEILING(-4.2, 0.5) shouldBe -4.0
        CEILING(-4.0, 0.5) shouldBe -4.0

        CEILING(4.3, 0.5) shouldBe 4.5
        CEILING(4.25, 0.5) shouldBe 4.5
        CEILING(5.25, 0.5) shouldBe 5.5
        CEILING(4.2, 0.5) shouldBe 4.5
        CEILING(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_floor() = tests {
        FLOOR(-4.6, 1.0) shouldBe -5.0
        FLOOR(-4.5, 1.0) shouldBe -5.0
        FLOOR(-5.5, 1.0) shouldBe -6.0
        FLOOR(-4.4, 1.0) shouldBe -5.0
        FLOOR(-4.0, 1.0) shouldBe -4.0

        FLOOR(4.6, 1.0) shouldBe 4.0
        FLOOR(4.5, 1.0) shouldBe 4.0
        FLOOR(5.5, 1.0) shouldBe 5.0
        FLOOR(4.4, 1.0) shouldBe 4.0
        FLOOR(4.0, 1.0) shouldBe 4.0

        FLOOR(-4.3, 0.5) shouldBe -4.5
        FLOOR(-4.25, 0.5) shouldBe -4.5
        FLOOR(-5.25, 0.5) shouldBe -5.5
        FLOOR(-4.2, 0.5) shouldBe -4.5
        FLOOR(-4.0, 0.5) shouldBe -4.0

        FLOOR(4.3, 0.5) shouldBe 4.0
        FLOOR(4.25, 0.5) shouldBe 4.0
        FLOOR(5.25, 0.5) shouldBe 5.0
        FLOOR(4.2, 0.5) shouldBe 4.0
        FLOOR(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_halp_up() = tests {
        HALF_UP(-4.6, 1.0) shouldBe -5.0
        HALF_UP(-4.5, 1.0) shouldBe -5.0
        HALF_UP(-5.5, 1.0) shouldBe -6.0
        HALF_UP(-4.4, 1.0) shouldBe -4.0
        HALF_UP(-4.0, 1.0) shouldBe -4.0

        HALF_UP(4.6, 1.0) shouldBe 5.0
        HALF_UP(4.5, 1.0) shouldBe 5.0
        HALF_UP(5.5, 1.0) shouldBe 6.0
        HALF_UP(4.4, 1.0) shouldBe 4.0
        HALF_UP(4.0, 1.0) shouldBe 4.0

        HALF_UP(-4.3, 0.5) shouldBe -4.5
        HALF_UP(-4.25, 0.5) shouldBe -4.5
        HALF_UP(-5.25, 0.5) shouldBe -5.5
        HALF_UP(-4.2, 0.5) shouldBe -4.0
        HALF_UP(-4.0, 0.5) shouldBe -4.0

        HALF_UP(4.3, 0.5) shouldBe 4.5
        HALF_UP(4.25, 0.5) shouldBe 4.5
        HALF_UP(5.25, 0.5) shouldBe 5.5
        HALF_UP(4.2, 0.5) shouldBe 4.0
        HALF_UP(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_half_down() = tests {
        HALF_DOWN(-4.6, 1.0) shouldBe -5.0
        HALF_DOWN(-4.5, 1.0) shouldBe -4.0
        HALF_DOWN(-5.5, 1.0) shouldBe -5.0
        HALF_DOWN(-4.4, 1.0) shouldBe -4.0
        HALF_DOWN(-4.0, 1.0) shouldBe -4.0

        HALF_DOWN(4.6, 1.0) shouldBe 5.0
        HALF_DOWN(4.5, 1.0) shouldBe 4.0
        HALF_DOWN(5.5, 1.0) shouldBe 5.0
        HALF_DOWN(4.4, 1.0) shouldBe 4.0
        HALF_DOWN(4.0, 1.0) shouldBe 4.0

        HALF_DOWN(-4.3, 0.5) shouldBe -4.5
        HALF_DOWN(-4.25, 0.5) shouldBe -4.0
        HALF_DOWN(-5.25, 0.5) shouldBe -5.0
        HALF_DOWN(-4.2, 0.5) shouldBe -4.0
        HALF_DOWN(-4.0, 0.5) shouldBe -4.0

        HALF_DOWN(4.3, 0.5) shouldBe 4.5
        HALF_DOWN(4.25, 0.5) shouldBe 4.0
        HALF_DOWN(5.25, 0.5) shouldBe 5.0
        HALF_DOWN(4.2, 0.5) shouldBe 4.0
        HALF_DOWN(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_half_even() = tests {
        HALF_EVEN(-4.6, 1.0) shouldBe -5.0
        HALF_EVEN(-4.5, 1.0) shouldBe -4.0
        HALF_EVEN(-5.5, 1.0) shouldBe -6.0
        HALF_EVEN(-4.4, 1.0) shouldBe -4.0
        HALF_EVEN(-4.0, 1.0) shouldBe -4.0

        HALF_EVEN(4.6, 1.0) shouldBe 5.0
        HALF_EVEN(4.5, 1.0) shouldBe 4.0
        HALF_EVEN(5.5, 1.0) shouldBe 6.0
        HALF_EVEN(4.4, 1.0) shouldBe 4.0
        HALF_EVEN(4.0, 1.0) shouldBe 4.0

        HALF_EVEN(-4.3, 0.5) shouldBe -4.5
        HALF_EVEN(-4.25, 0.5) shouldBe -4.0
        HALF_EVEN(-5.25, 0.5) shouldBe -5.0
        HALF_EVEN(-4.2, 0.5) shouldBe -4.0
        HALF_EVEN(-4.0, 0.5) shouldBe -4.0

        HALF_EVEN(4.3, 0.5) shouldBe 4.5
        HALF_EVEN(4.25, 0.5) shouldBe 4.0
        HALF_EVEN(5.25, 0.5) shouldBe 5.0
        HALF_EVEN(4.2, 0.5) shouldBe 4.0
        HALF_EVEN(4.0, 0.5) shouldBe 4.0
    }

    @Test fun rounding_unnecessary() = tests {
        shouldThrow<ArithmeticException> { UNNECESSARY(1.0, 1.0) }
    }

    @Test fun extensions() = tests {
        2.25.ceil(0.5) shouldBe 2.5
        2.25f.ceil(0.5) shouldBe 2.5
        2.25.floor(0.5) shouldBe 2.0
        2.25f.floor(0.5) shouldBe 2.0
        2.25.round(0.5) shouldBe 2.0
        2.25f.round(0.5) shouldBe 2.0
    }

    @Test fun regression() = tests {
        HALF_EVEN(0.7, 0.001) shouldBe 0.7
        0.7.round(0.001) shouldBe 0.7
    }
}
