package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DisplayNameTest {

    @Test fun display_name() = testAll {
        DisplayName(listOf("Engine", "Test", "Nested"), "method") should {
            it.ancestorDisplayNames.shouldContainExactly("Engine", "Test", "Nested")
            it.displayName shouldBe "method"
            it.toString() shouldBe "method"
            it.composedDisplayName shouldBe "Test ➜ Nested ➜ method"
        }
    }
}
