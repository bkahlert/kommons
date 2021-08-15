package com.bkahlert.kommons

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NativeTest {

    @Test
    fun test() {
        val expectedArray = arrayOf(1, 2, 3)
        val actualArray = Array(3) { it + 1 }

        val first: Any = actualArray[0]
        assertIs<Int>(first)
        // first is smart-cast to Int now
        println("${first + 1}")

        assertContentEquals(expectedArray, actualArray)
        assertContains(expectedArray, 2)

        val x = sin(PI)

        // precision parameter
        val tolerance = 0.000001

        assertEquals(0.0, x, tolerance)
    }
}
