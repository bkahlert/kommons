package com.bkahlert.kommons.time

import kotlin.js.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.expect

class NowKtTest {

    @Test
    fun shouldReturnNow() {
        assertEquals(Date.now(), Now.getTime())
    }

    @Test
    fun shouldAddDate() {
        expect(8.seconds) { Date(5000) + Date(3000) }
    }

    @Test
    fun shouldSubtractDate() {
        expect(8.seconds) { Date(11000) - Date(3000) }
    }

    @Test
    fun shouldAddDuration() {
        expect(Date(8000).getTime()) { (Date(5000) + 3.seconds).getTime() }
    }

    @Test
    fun shouldSubtractDuration() {
        expect(Date(8000).getTime()) { (Date(11000) - 3.seconds).getTime() }
    }
}
