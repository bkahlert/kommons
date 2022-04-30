package com.bkahlert.kommons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SimpleStringTest {

    @Test
    fun testToSimpleString() {
        assertEquals("␀", (null as Any?).toSimpleString())
        assertEquals("42s", 42.seconds.toSimpleString())
        assertEquals("Duration", Duration::class.toSimpleString())
    }

    @Test
    fun testToSimpleClassName() {
        assertEquals("␀", (null as Any?).toSimpleClassName())
        assertEquals("Duration", 42.seconds.toSimpleClassName())
        assertTrue("must start with KClass") { Duration::class.toSimpleClassName().startsWith("KClass") }
    }
}
