package com.bkahlert.kommons

import com.bkahlert.kommons.RoundingMode.Ceiling
import com.bkahlert.kommons.RoundingMode.Down
import com.bkahlert.kommons.RoundingMode.Floor
import com.bkahlert.kommons.RoundingMode.HalfDown
import com.bkahlert.kommons.RoundingMode.HalfEven
import com.bkahlert.kommons.RoundingMode.HalfUp
import com.bkahlert.kommons.RoundingMode.Unnecessary
import com.bkahlert.kommons.RoundingMode.Up
import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RoundingModeTest {

    @Test fun round_up() = testAll {
        Up(-4.6, 1.0) shouldBe -5.0
        Up(-4.5, 1.0) shouldBe -5.0
        Up(-5.5, 1.0) shouldBe -6.0
        Up(-4.4, 1.0) shouldBe -5.0
        Up(-4.0, 1.0) shouldBe -4.0

        Up(4.6, 1.0) shouldBe 5.0
        Up(4.5, 1.0) shouldBe 5.0
        Up(5.5, 1.0) shouldBe 6.0
        Up(4.4, 1.0) shouldBe 5.0
        Up(4.0, 1.0) shouldBe 4.0

        Up(-4.3, 0.5) shouldBe -4.5
        Up(-4.25, 0.5) shouldBe -4.5
        Up(-5.25, 0.5) shouldBe -5.5
        Up(-4.2, 0.5) shouldBe -4.5
        Up(-4.0, 0.5) shouldBe -4.0

        Up(4.3, 0.5) shouldBe 4.5
        Up(4.25, 0.5) shouldBe 4.5
        Up(5.25, 0.5) shouldBe 5.5
        Up(4.2, 0.5) shouldBe 4.5
        Up(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_down() = testAll {
        Down(-4.6, 1.0) shouldBe -4.0
        Down(-4.5, 1.0) shouldBe -4.0
        Down(-5.5, 1.0) shouldBe -5.0
        Down(-4.4, 1.0) shouldBe -4.0
        Down(-4.0, 1.0) shouldBe -4.0

        Down(4.6, 1.0) shouldBe 4.0
        Down(4.5, 1.0) shouldBe 4.0
        Down(5.5, 1.0) shouldBe 5.0
        Down(4.4, 1.0) shouldBe 4.0
        Down(4.0, 1.0) shouldBe 4.0

        Down(-4.3, 0.5) shouldBe -4.0
        Down(-4.25, 0.5) shouldBe -4.0
        Down(-5.25, 0.5) shouldBe -5.0
        Down(-4.2, 0.5) shouldBe -4.0
        Down(-4.0, 0.5) shouldBe -4.0

        Down(4.3, 0.5) shouldBe 4.0
        Down(4.25, 0.5) shouldBe 4.0
        Down(5.25, 0.5) shouldBe 5.0
        Down(4.2, 0.5) shouldBe 4.0
        Down(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_ceiling() = testAll {
        Ceiling(-4.6, 1.0) shouldBe -4.0
        Ceiling(-4.5, 1.0) shouldBe -4.0
        Ceiling(-5.5, 1.0) shouldBe -5.0
        Ceiling(-4.4, 1.0) shouldBe -4.0
        Ceiling(-4.0, 1.0) shouldBe -4.0

        Ceiling(4.6, 1.0) shouldBe 5.0
        Ceiling(4.5, 1.0) shouldBe 5.0
        Ceiling(5.5, 1.0) shouldBe 6.0
        Ceiling(4.4, 1.0) shouldBe 5.0
        Ceiling(4.0, 1.0) shouldBe 4.0

        Ceiling(-4.3, 0.5) shouldBe -4.0
        Ceiling(-4.25, 0.5) shouldBe -4.0
        Ceiling(-5.25, 0.5) shouldBe -5.0
        Ceiling(-4.2, 0.5) shouldBe -4.0
        Ceiling(-4.0, 0.5) shouldBe -4.0

        Ceiling(4.3, 0.5) shouldBe 4.5
        Ceiling(4.25, 0.5) shouldBe 4.5
        Ceiling(5.25, 0.5) shouldBe 5.5
        Ceiling(4.2, 0.5) shouldBe 4.5
        Ceiling(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_floor() = testAll {
        Floor(-4.6, 1.0) shouldBe -5.0
        Floor(-4.5, 1.0) shouldBe -5.0
        Floor(-5.5, 1.0) shouldBe -6.0
        Floor(-4.4, 1.0) shouldBe -5.0
        Floor(-4.0, 1.0) shouldBe -4.0

        Floor(4.6, 1.0) shouldBe 4.0
        Floor(4.5, 1.0) shouldBe 4.0
        Floor(5.5, 1.0) shouldBe 5.0
        Floor(4.4, 1.0) shouldBe 4.0
        Floor(4.0, 1.0) shouldBe 4.0

        Floor(-4.3, 0.5) shouldBe -4.5
        Floor(-4.25, 0.5) shouldBe -4.5
        Floor(-5.25, 0.5) shouldBe -5.5
        Floor(-4.2, 0.5) shouldBe -4.5
        Floor(-4.0, 0.5) shouldBe -4.0

        Floor(4.3, 0.5) shouldBe 4.0
        Floor(4.25, 0.5) shouldBe 4.0
        Floor(5.25, 0.5) shouldBe 5.0
        Floor(4.2, 0.5) shouldBe 4.0
        Floor(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_half_up() = testAll {
        HalfUp(-4.6, 1.0) shouldBe -5.0
        HalfUp(-4.5, 1.0) shouldBe -5.0
        HalfUp(-5.5, 1.0) shouldBe -6.0
        HalfUp(-4.4, 1.0) shouldBe -4.0
        HalfUp(-4.0, 1.0) shouldBe -4.0

        HalfUp(4.6, 1.0) shouldBe 5.0
        HalfUp(4.5, 1.0) shouldBe 5.0
        HalfUp(5.5, 1.0) shouldBe 6.0
        HalfUp(4.4, 1.0) shouldBe 4.0
        HalfUp(4.0, 1.0) shouldBe 4.0

        HalfUp(-4.3, 0.5) shouldBe -4.5
        HalfUp(-4.25, 0.5) shouldBe -4.5
        HalfUp(-5.25, 0.5) shouldBe -5.5
        HalfUp(-4.2, 0.5) shouldBe -4.0
        HalfUp(-4.0, 0.5) shouldBe -4.0

        HalfUp(4.3, 0.5) shouldBe 4.5
        HalfUp(4.25, 0.5) shouldBe 4.5
        HalfUp(5.25, 0.5) shouldBe 5.5
        HalfUp(4.2, 0.5) shouldBe 4.0
        HalfUp(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_half_down() = testAll {
        HalfDown(-4.6, 1.0) shouldBe -5.0
        HalfDown(-4.5, 1.0) shouldBe -4.0
        HalfDown(-5.5, 1.0) shouldBe -5.0
        HalfDown(-4.4, 1.0) shouldBe -4.0
        HalfDown(-4.0, 1.0) shouldBe -4.0

        HalfDown(4.6, 1.0) shouldBe 5.0
        HalfDown(4.5, 1.0) shouldBe 4.0
        HalfDown(5.5, 1.0) shouldBe 5.0
        HalfDown(4.4, 1.0) shouldBe 4.0
        HalfDown(4.0, 1.0) shouldBe 4.0

        HalfDown(-4.3, 0.5) shouldBe -4.5
        HalfDown(-4.25, 0.5) shouldBe -4.0
        HalfDown(-5.25, 0.5) shouldBe -5.0
        HalfDown(-4.2, 0.5) shouldBe -4.0
        HalfDown(-4.0, 0.5) shouldBe -4.0

        HalfDown(4.3, 0.5) shouldBe 4.5
        HalfDown(4.25, 0.5) shouldBe 4.0
        HalfDown(5.25, 0.5) shouldBe 5.0
        HalfDown(4.2, 0.5) shouldBe 4.0
        HalfDown(4.0, 0.5) shouldBe 4.0
    }

    @Test fun round_half_even() = testAll {
        HalfEven(-4.6, 1.0) shouldBe -5.0
        HalfEven(-4.5, 1.0) shouldBe -4.0
        HalfEven(-5.5, 1.0) shouldBe -6.0
        HalfEven(-4.4, 1.0) shouldBe -4.0
        HalfEven(-4.0, 1.0) shouldBe -4.0

        HalfEven(4.6, 1.0) shouldBe 5.0
        HalfEven(4.5, 1.0) shouldBe 4.0
        HalfEven(5.5, 1.0) shouldBe 6.0
        HalfEven(4.4, 1.0) shouldBe 4.0
        HalfEven(4.0, 1.0) shouldBe 4.0

        HalfEven(-4.3, 0.5) shouldBe -4.5
        HalfEven(-4.25, 0.5) shouldBe -4.0
        HalfEven(-5.25, 0.5) shouldBe -5.0
        HalfEven(-4.2, 0.5) shouldBe -4.0
        HalfEven(-4.0, 0.5) shouldBe -4.0

        HalfEven(4.3, 0.5) shouldBe 4.5
        HalfEven(4.25, 0.5) shouldBe 4.0
        HalfEven(5.25, 0.5) shouldBe 5.0
        HalfEven(4.2, 0.5) shouldBe 4.0
        HalfEven(4.0, 0.5) shouldBe 4.0
    }

    @Test fun rounding_unnecessary() = testAll {
        shouldThrow<ArithmeticException> { Unnecessary(1.0, 1.0) }
    }

    @Test fun extensions() = testAll {
        2.25.ceil(0.5) shouldBe 2.5
        2.25f.ceil(0.5) shouldBe 2.5
        2.25.floor(0.5) shouldBe 2.0
        2.25f.floor(0.5) shouldBe 2.0
        2.25.round(0.5) shouldBe 2.0
        2.25f.round(0.5) shouldBe 2.0
    }

    @Test fun regression() = testAll {
        HalfEven(0.7, 0.001) shouldBe 0.7
        0.7.round(0.001) shouldBe 0.7
    }


    @Test fun is_odd() = testAll(-1, 1) {
        it.toByte().isOdd shouldBe true
        (it + 1).toByte().isOdd shouldBe false
        it.toShort().isOdd shouldBe true
        (it + 1).toShort().isOdd shouldBe false
        it.isOdd shouldBe true
        (it + 1).isOdd shouldBe false
        it.toLong().isOdd shouldBe true
        (it + 1).toLong().isOdd shouldBe false

        it.toUByte().isOdd shouldBe true
        (it + 1).toUByte().isOdd shouldBe false
        it.toUShort().isOdd shouldBe true
        (it + 1).toUShort().isOdd shouldBe false
        it.toUInt().isOdd shouldBe true
        (it + 1).toUInt().isOdd shouldBe false
        it.toULong().isOdd shouldBe true
        (it + 1).toULong().isOdd shouldBe false
    }

    @Test fun is_even() = testAll(-1, 1) {
        it.toByte().isEven shouldBe false
        (it + 1).toByte().isEven shouldBe true
        it.toShort().isEven shouldBe false
        (it + 1).toShort().isEven shouldBe true
        it.isEven shouldBe false
        (it + 1).isEven shouldBe true
        it.toLong().isEven shouldBe false
        (it + 1).toLong().isEven shouldBe true

        it.toUByte().isEven shouldBe false
        (it + 1).toUByte().isEven shouldBe true
        it.toUShort().isEven shouldBe false
        (it + 1).toUShort().isEven shouldBe true
        it.toUInt().isEven shouldBe false
        (it + 1).toUInt().isEven shouldBe true
        it.toULong().isEven shouldBe false
        (it + 1).toULong().isEven shouldBe true
    }
}
