package com.bkahlert.kommons

import kotlin.test.Test
import kotlin.test.assertEquals

class ResultsKtTest {

    @Test
    fun get_or_exception() {
        assertEquals(42 to null, kotlin.runCatching { 42 }.getOrException())
        val ex = IllegalStateException()
        assertEquals(null to ex, kotlin.runCatching { throw ex }.getOrException<Nothing?>())
    }
}
