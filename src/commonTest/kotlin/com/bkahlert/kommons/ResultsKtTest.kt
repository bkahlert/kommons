package com.bkahlert.kommons

import com.bkahlert.kommons.test.tests
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ResultsKtTest {

    @Test
    fun get_or_exception() = tests {
        kotlin.runCatching { 42 }.getOrException() shouldBe (42 to null)
        val ex = IllegalStateException()
        kotlin.runCatching { throw ex }.getOrException<Nothing?>() shouldBe (null to ex)
    }
}
