package com.bkahlert.kommons.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.asserter

class ValueRangesKtTest {

    @Test
    fun map() {
        assertEquals(0.0, 0.0.map(0.0..5.0, 0.0..1.0))
        assertEquals(2.0, 0.4.map(0.0..5.0, 0.0..1.0))
        assertEquals(2.5, 0.5.map(0.0..5.0, 0.0..1.0))
        assertEquals(3.75, 0.75.map(0.0..5.0, 0.0..1.0))
        assertEquals(5.0, 1.0.map(0.0..5.0, 0.0..1.0))
        assertFailsWith<IllegalArgumentException> { (-0.1).map(0.0..5.0, 0.0..1.0) }
        assertFailsWith<IllegalArgumentException> { (1.1).map(0.0..5.0, 0.0..1.0) }

        assertEquals(2.0, 0.4.map(5.0))
        assertEquals(4.0, 0.4.map(10.0))
        assertEquals(7.0, 0.4.map(5.0..10.00))
        assertEquals(-1.0, 0.4.map((-5.0)..5.00))
        assertEquals((-13.0), 0.4.map((-15.0)..(-10.00)))
    }

    @Test
    fun normalize() {
        assertEquals(0.0, 0.0.normalize(0.0..5.0))
        assertEquals(0.4, 2.0.normalize(0.0..5.0))
        assertEquals(0.5, 2.5.normalize(0.0..5.0))
        assertEquals(0.75, 3.75.normalize(0.0..5.0))
        assertEquals(1.0, 5.0.normalize(0.0..5.0))
        assertFailsWith<IllegalArgumentException> { (-0.1).normalize(0.0..5.0) }
        assertFailsWith<IllegalArgumentException> { (5.1).normalize(0.0..5.0) }

        assertEquals(0.4, 2.0.normalize(5.0))
        assertEquals(0.4, 4.0.normalize(10.0))
        assertEquals(0.4, 7.0.normalize(5.0..10.00))
        assertEquals(0.4, (-1.0).normalize((-5.0)..5.00))
        assertEquals(0.4, (-13.0).normalize((-15.0)..(-10.00)))
    }

    @Test
    fun scale() {
        assertEquals(-1.0, 0.0.scale(-1.0, -1.0..4.0))
        assertEquals(-0.8, 0.0.scale(-0.8, -1.0..4.0))
        assertEquals(0.0, 0.0.scale(0.0, -1.0..4.0))
        assertEquals(3.2, 0.0.scale(0.8, -1.0..4.0))
        assertEquals(4.0, 0.0.scale(1.0, -1.0..4.0))
        assertFailsWith<IllegalArgumentException> { 0.0.scale(-1.1, -1.0..4.0) }
        assertFailsWith<IllegalArgumentException> { 0.0.scale(+1.1, -1.0..4.0) }

        assertEquals(-1.0, 2.0.scale(-1.0, -1.0..4.0))
        assertRoundedEquals(-0.4, 2.0.scale(-0.8, -1.0..4.0))
        assertEquals(2.0, 2.0.scale(0.0, -1.0..4.0))
        assertEquals(3.6, 2.0.scale(0.8, -1.0..4.0))
        assertEquals(4.0, 2.0.scale(1.0, -1.0..4.0))

        assertRoundedEquals(3.8, (-1.0).scale(0.8, -1.0..5.0))
        assertEquals(4.4, 2.0.scale(0.8, 5.0))
        assertEquals(0.88, 0.4.scale(0.8))
        assertFailsWith<IllegalArgumentException> { 10.0.scale(0.0, -1.0..5.0) }
    }
}

/** Asserts that the [expected] value is equal to the [actual] value, with an optional [message]. */
fun assertRoundedEquals(expected: Double, actual: Double, message: String? = null, resolution: Double = 0.1) {
    asserter.assertEquals(message, expected.round(resolution), actual.round(resolution))
}
