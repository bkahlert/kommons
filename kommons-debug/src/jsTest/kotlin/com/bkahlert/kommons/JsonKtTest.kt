package com.bkahlert.kommons

import com.bkahlert.kommons.debug.entries
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class JsonKtTest {

    @Test fun json() = testAll {
        json(mapOf("foo" to "bar", "baz" to null)).entries shouldBe kotlin.js.json("foo" to "bar", "baz" to null).entries
    }
}
