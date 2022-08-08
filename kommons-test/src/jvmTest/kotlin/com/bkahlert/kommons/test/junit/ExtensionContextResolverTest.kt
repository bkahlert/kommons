package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext

class ExtensionContextResolverTest {

    @Test fun test_name(extensionContext: ExtensionContext) = testAll {
        extensionContext.uniqueId shouldBe listOf(
            "[engine:junit-jupiter]",
            "[class:com.bkahlert.kommons.test.junit.ExtensionContextResolverTest]",
            "[method:test_name(org.junit.jupiter.api.extension.ExtensionContext)]",
        ).joinToString("/")
    }

    @Nested
    inner class NestedTest {

        @Test fun test_name(extensionContext: ExtensionContext) = testAll {
            extensionContext.uniqueId shouldBe listOf(
                "[engine:junit-jupiter]",
                "[class:com.bkahlert.kommons.test.junit.ExtensionContextResolverTest]",
                "[nested-class:NestedTest]",
                "[method:test_name(org.junit.jupiter.api.extension.ExtensionContext)]",
            ).joinToString("/")
        }
    }
}
