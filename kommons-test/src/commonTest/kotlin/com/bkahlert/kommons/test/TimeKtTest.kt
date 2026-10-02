package com.bkahlert.kommons.test

import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

class TimeKtTest {

    @Test
    fun clock() = testAll(Instant.parse("2020-02-02T02:02:02Z")) {
        Clock.fixed(it).now() shouldBe it
    }
}
