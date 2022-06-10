package com.bkahlert.kommons.math

import com.bkahlert.kommons.test.tests
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ValueRangesKtTest {

    @Test
    fun map() = tests {
        0.0.map(0.0..5.0, 0.0..1.0) shouldBe 0.0
        0.4.map(0.0..5.0, 0.0..1.0) shouldBe 2.0
        0.5.map(0.0..5.0, 0.0..1.0) shouldBe 2.5
        0.75.map(0.0..5.0, 0.0..1.0) shouldBe 3.75
        1.0.map(0.0..5.0, 0.0..1.0) shouldBe 5.0
        shouldThrow<IllegalArgumentException> { (-0.1).map(0.0..5.0, 0.0..1.0) }
        shouldThrow<IllegalArgumentException> { (1.1).map(0.0..5.0, 0.0..1.0) }

        0.4.map(5.0) shouldBe 2.0
        0.4.map(10.0) shouldBe 4.0
        0.4.map(5.0..10.00) shouldBe 7.0
        0.4.map((-5.0)..5.00) shouldBe -1.0
        0.4.map((-15.0)..(-10.00)) shouldBe (-13.0)
    }

    @Test
    fun normalize() = tests {
        0.0.normalize(0.0..5.0) shouldBe 0.0
        2.0.normalize(0.0..5.0) shouldBe 0.4
        2.5.normalize(0.0..5.0) shouldBe 0.5
        3.75.normalize(0.0..5.0) shouldBe 0.75
        5.0.normalize(0.0..5.0) shouldBe 1.0
        shouldThrow<IllegalArgumentException> { (-0.1).normalize(0.0..5.0) }
        shouldThrow<IllegalArgumentException> { (5.1).normalize(0.0..5.0) }

        2.0.normalize(5.0) shouldBe 0.4
        4.0.normalize(10.0) shouldBe 0.4
        7.0.normalize(5.0..10.00) shouldBe 0.4
        (-1.0).normalize((-5.0)..5.00) shouldBe 0.4
        (-13.0).normalize((-15.0)..(-10.00)) shouldBe 0.4
    }

    @Test
    fun scale() = tests {
        0.0.scale(-1.0, -1.0..4.0) shouldBe -1.0
        0.0.scale(-0.8, -1.0..4.0) shouldBe -0.8
        0.0.scale(0.0, -1.0..4.0) shouldBe 0.0
        0.0.scale(0.8, -1.0..4.0) shouldBe 3.2
        0.0.scale(1.0, -1.0..4.0) shouldBe 4.0
        shouldThrow<IllegalArgumentException> { 0.0.scale(-1.1, -1.0..4.0) }
        shouldThrow<IllegalArgumentException> { 0.0.scale(+1.1, -1.0..4.0) }

        2.0.scale(-1.0, -1.0..4.0) - 1.0
        2.0.scale(-0.8, -1.0..4.0).round(0.1) shouldBe -0.4
        2.0.scale(0.0, -1.0..4.0) shouldBe 2.0
        2.0.scale(0.8, -1.0..4.0) shouldBe 3.6
        2.0.scale(1.0, -1.0..4.0) shouldBe 4.0

        (-1.0).scale(0.8, -1.0..5.0).round(0.1) shouldBe 3.8
        2.0.scale(0.8, 5.0) shouldBe 4.4
        0.4.scale(0.8) shouldBe 0.88
        shouldThrow<IllegalArgumentException> { 10.0.scale(0.0, -1.0..5.0) }
    }
}
