package com.bkahlert.kommons.debug

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PlainTestJvm {

    @Test fun plain_native_collection() = testAll {
        java.util.LinkedHashSet(listOf("foo", "bar")).asClue { it.isPlain shouldBe true }
    }

    @Test fun plain_native_map() = testAll {
        java.util.LinkedHashMap(mapOf("foo" to "bar", "baz" to null)).asClue { it.isPlain shouldBe true }
    }
}
