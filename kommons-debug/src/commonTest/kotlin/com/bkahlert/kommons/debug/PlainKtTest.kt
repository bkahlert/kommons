package com.bkahlert.kommons.debug

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.asClue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PlainTest {

    @Test fun plain_collection() = testAll {
        setOf("foo", "bar").asClue { it.isPlain shouldBe true }
        mutableSetOf("foo", "bar").asClue { it.isPlain shouldBe true }
        listOf("foo", "bar").asClue { it.isPlain shouldBe true }
        mutableListOf("foo", "bar").asClue { it.isPlain shouldBe true }
        ListImplementingSingleton.asClue { it.isPlain shouldBe false }
        ListImplementingAnonymousSingleton.asClue { it.isPlain shouldBe false }
    }

    @Test fun plain_map() = testAll {
        mapOf("foo" to "bar", "baz" to null).asClue { it.isPlain shouldBe true }
        mutableMapOf("foo" to "bar", "baz" to null).asClue { it.isPlain shouldBe true }
        MapImplementingSingleton.asClue { it.isPlain shouldBe false }
        MapImplementingAnonymousSingleton.asClue { it.isPlain shouldBe false }
    }
}
