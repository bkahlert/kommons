package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.shouldBeBetween
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import kotlin.test.Test

class RangesKtTest {

    @Test fun map() = testAll {
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

    @Test fun normalize() = testAll {
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

    @Test fun scale() = testAll {
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

    @Test fun end() = testAll {
        (0..10).end shouldBe 11
        shouldThrow<IllegalStateException> { (0..Int.MAX_VALUE).end }.message shouldBe "The exclusive end of 0..2147483647 is greater than Int.MAX_VALUE."
    }

    @Test fun random() = testAll {
        (-4.2..42.0) should { range ->
            repeat(100) { range.random().shouldBeBetween(-4.2, 42.0, 0.1) }
            repeat(100) { range.random(Random(123)).shouldBeBetween(-4.2, 42.0, 0.1) }
        }
        @Suppress("EmptyRange")
        (42.0..-4.2) should { range ->
            shouldThrow<NoSuchElementException> { range.random().shouldBeNull() }
            shouldThrow<NoSuchElementException> { range.random(Random(123)).shouldBeNull() }
        }
    }

    @Test fun random_or_null() = testAll {
        (-4.2..42.0) should { range ->
            repeat(100) { range.randomOrNull().shouldNotBeNull().shouldBeBetween(-4.2, 42.0, 0.1) }
            repeat(100) { range.randomOrNull(Random(123)).shouldNotBeNull().shouldBeBetween(-4.2, 42.0, 0.1) }
        }
        @Suppress("EmptyRange")
        (42.0..-4.2) should { range ->
            range.randomOrNull().shouldBeNull()
            range.randomOrNull(Random(123)).shouldBeNull()
        }
    }

    @Test fun as_iterable() = testAll {
        (-4..42).asIterable { it + 9 }.map { it }.shouldContainExactly(-4, 5, 14, 23, 32, 41)
        (-4.2..42.0).asIterable { it + 9 }.map { it.toInt() }.shouldContainExactly(-4, 4, 13, 22, 31, 40)
        @Suppress("EmptyRange")
        (42.0..-4.2).asIterable { it + 9 }.map { it.toInt() }.shouldBeEmpty()
    }
}
