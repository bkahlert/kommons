package com.bkahlert.kommons.math

import com.bkahlert.kommons.collections.too
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isEqualTo

class RoundingModeTest {

    @TestFactory
    fun `should round up`() = testEach(
        -4.6 to 1.0 too -5.0,
        -4.5 to 1.0 too -5.0,
        -5.5 to 1.0 too -6.0,
        -4.4 to 1.0 too -5.0,
        -4.0 to 1.0 too -4.0,

        4.6 to 1.0 too 5.0,
        4.5 to 1.0 too 5.0,
        5.5 to 1.0 too 6.0,
        4.4 to 1.0 too 5.0,
        4.0 to 1.0 too 4.0,

        -4.3 to 0.5 too -4.5,
        -4.25 to 0.5 too -4.5,
        -5.25 to 0.5 too -5.5,
        -4.2 to 0.5 too -4.5,
        -4.0 to 0.5 too -4.0,

        4.3 to 0.5 too 4.5,
        4.25 to 0.5 too 4.5,
        5.25 to 0.5 too 5.5,
        4.2 to 0.5 too 4.5,
        4.0 to 0.5 too 4.0,
    ) { (value, resolution, expected) ->
        expecting { RoundingMode.UP(value, resolution) } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should round down`() = testEach(
        -4.6 to 1.0 too -4.0,
        -4.5 to 1.0 too -4.0,
        -5.5 to 1.0 too -5.0,
        -4.4 to 1.0 too -4.0,
        -4.0 to 1.0 too -4.0,

        4.6 to 1.0 too 4.0,
        4.5 to 1.0 too 4.0,
        5.5 to 1.0 too 5.0,
        4.4 to 1.0 too 4.0,
        4.0 to 1.0 too 4.0,

        -4.3 to 0.5 too -4.0,
        -4.25 to 0.5 too -4.0,
        -5.25 to 0.5 too -5.0,
        -4.2 to 0.5 too -4.0,
        -4.0 to 0.5 too -4.0,

        4.3 to 0.5 too 4.0,
        4.25 to 0.5 too 4.0,
        5.25 to 0.5 too 5.0,
        4.2 to 0.5 too 4.0,
        4.0 to 0.5 too 4.0,
    ) { (value, resolution, expected) ->
        expecting { RoundingMode.DOWN(value, resolution) } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should round ceiling`() = testEach(
        -4.6 to 1.0 too -4.0,
        -4.5 to 1.0 too -4.0,
        -5.5 to 1.0 too -5.0,
        -4.4 to 1.0 too -4.0,
        -4.0 to 1.0 too -4.0,

        4.6 to 1.0 too 5.0,
        4.5 to 1.0 too 5.0,
        5.5 to 1.0 too 6.0,
        4.4 to 1.0 too 5.0,
        4.0 to 1.0 too 4.0,

        -4.3 to 0.5 too -4.0,
        -4.25 to 0.5 too -4.0,
        -5.25 to 0.5 too -5.0,
        -4.2 to 0.5 too -4.0,
        -4.0 to 0.5 too -4.0,

        4.3 to 0.5 too 4.5,
        4.25 to 0.5 too 4.5,
        5.25 to 0.5 too 5.5,
        4.2 to 0.5 too 4.5,
        4.0 to 0.5 too 4.0,
    ) { (value, resolution, expected) ->
        expecting { RoundingMode.CEILING(value, resolution) } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should round floor`() = testEach(
        -4.6 to 1.0 too -5.0,
        -4.5 to 1.0 too -5.0,
        -5.5 to 1.0 too -6.0,
        -4.4 to 1.0 too -5.0,
        -4.0 to 1.0 too -4.0,

        4.6 to 1.0 too 4.0,
        4.5 to 1.0 too 4.0,
        5.5 to 1.0 too 5.0,
        4.4 to 1.0 too 4.0,
        4.0 to 1.0 too 4.0,

        -4.3 to 0.5 too -4.5,
        -4.25 to 0.5 too -4.5,
        -5.25 to 0.5 too -5.5,
        -4.2 to 0.5 too -4.5,
        -4.0 to 0.5 too -4.0,

        4.3 to 0.5 too 4.0,
        4.25 to 0.5 too 4.0,
        5.25 to 0.5 too 5.0,
        4.2 to 0.5 too 4.0,
        4.0 to 0.5 too 4.0,
    ) { (value, resolution, expected) ->
        expecting { RoundingMode.FLOOR(value, resolution) } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should round half up`() = testEach(
        -4.6 to 1.0 too -5.0,
        -4.5 to 1.0 too -5.0,
        -5.5 to 1.0 too -6.0,
        -4.4 to 1.0 too -4.0,
        -4.0 to 1.0 too -4.0,

        4.6 to 1.0 too 5.0,
        4.5 to 1.0 too 5.0,
        5.5 to 1.0 too 6.0,
        4.4 to 1.0 too 4.0,
        4.0 to 1.0 too 4.0,

        -4.3 to 0.5 too -4.5,
        -4.25 to 0.5 too -4.5,
        -5.25 to 0.5 too -5.5,
        -4.2 to 0.5 too -4.0,
        -4.0 to 0.5 too -4.0,

        4.3 to 0.5 too 4.5,
        4.25 to 0.5 too 4.5,
        5.25 to 0.5 too 5.5,
        4.2 to 0.5 too 4.0,
        4.0 to 0.5 too 4.0,
    ) { (value, resolution, expected) ->
        expecting { RoundingMode.HALF_UP(value, resolution) } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should round half down`() = testEach(
        -4.6 to 1.0 too -5.0,
        -4.5 to 1.0 too -4.0,
        -5.5 to 1.0 too -5.0,
        -4.4 to 1.0 too -4.0,
        -4.0 to 1.0 too -4.0,

        4.6 to 1.0 too 5.0,
        4.5 to 1.0 too 4.0,
        5.5 to 1.0 too 5.0,
        4.4 to 1.0 too 4.0,
        4.0 to 1.0 too 4.0,

        -4.3 to 0.5 too -4.5,
        -4.25 to 0.5 too -4.0,
        -5.25 to 0.5 too -5.0,
        -4.2 to 0.5 too -4.0,
        -4.0 to 0.5 too -4.0,

        4.3 to 0.5 too 4.5,
        4.25 to 0.5 too 4.0,
        5.25 to 0.5 too 5.0,
        4.2 to 0.5 too 4.0,
        4.0 to 0.5 too 4.0,
    ) { (value, resolution, expected) ->
        expecting { RoundingMode.HALF_DOWN(value, resolution) } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should round half even`() = testEach(
        -4.6 to 1.0 too -5.0,
        -4.5 to 1.0 too -4.0,
        -5.5 to 1.0 too -6.0,
        -4.4 to 1.0 too -4.0,
        -4.0 to 1.0 too -4.0,

        4.6 to 1.0 too 5.0,
        4.5 to 1.0 too 4.0,
        5.5 to 1.0 too 6.0,
        4.4 to 1.0 too 4.0,
        4.0 to 1.0 too 4.0,

        -4.3 to 0.5 too -4.5,
        -4.25 to 0.5 too -4.0,
        -5.25 to 0.5 too -5.0,
        -4.2 to 0.5 too -4.0,
        -4.0 to 0.5 too -4.0,

        4.3 to 0.5 too 4.5,
        4.25 to 0.5 too 4.0,
        5.25 to 0.5 too 5.0,
        4.2 to 0.5 too 4.0,
        4.0 to 0.5 too 4.0,
    ) { (value, resolution, expected) ->
        expecting { RoundingMode.HALF_EVEN(value, resolution) } that { isEqualTo(expected) }
    }

    @Test
    fun `should throw if rounding unnecessary`() {
        expectThrows<ArithmeticException> { RoundingMode.UNNECESSARY(1.0, 1.0) }
    }

    @Nested
    inner class Extensions {

        @TestFactory
        fun `should round ceiling`() = testEach(2.25, 2.25f) {
            expecting { it.ceil(0.5) } that { isEqualTo(2.5) }
        }

        @TestFactory
        fun `should round floor`() = testEach(2.25, 2.25f) {
            expecting { it.floor(0.5) } that { isEqualTo(2.0) }
        }

        @TestFactory
        fun `should round half even`() = testEach(2.25, 2.25f) {
            expecting { it.round(0.5) } that { isEqualTo(2.0) }
        }
    }
}
