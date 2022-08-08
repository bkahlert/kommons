package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.platform.engine.UniqueId

class UniqueIdResolverTest {

    @Test fun test_name(uniqueId: UniqueId) = testAll {
        uniqueId.toString() shouldBe listOf(
            "[engine:junit-jupiter]",
            "[class:com.bkahlert.kommons.test.junit.UniqueIdResolverTest]",
            "[method:test_name(org.junit.platform.engine.UniqueId)]",
        ).joinToString("/")
    }

    @Nested
    inner class NestedTest {

        @Test fun test_name(uniqueId: UniqueId) = testAll {
            uniqueId.toString() shouldBe listOf(
                "[engine:junit-jupiter]",
                "[class:com.bkahlert.kommons.test.junit.UniqueIdResolverTest]",
                "[nested-class:NestedTest]",
                "[method:test_name(org.junit.platform.engine.UniqueId)]",
            ).joinToString("/")
        }
    }
}
