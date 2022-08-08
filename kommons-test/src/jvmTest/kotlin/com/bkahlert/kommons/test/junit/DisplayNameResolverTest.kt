package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayNameGeneration(DisplayNameGenerator.Standard::class)
class DisplayNameResolverTest {

    @Test fun test_name(displayName: DisplayName) = testAll {
        displayName.ancestorDisplayNames.shouldContainExactly("JUnit Jupiter", "DisplayNameResolverTest")
        displayName.displayName shouldBe "test_name(DisplayName)"
    }

    @Nested
    inner class NestedTest {

        @Test fun test_name(displayName: DisplayName) = testAll {
            displayName.ancestorDisplayNames.shouldContainExactly("JUnit Jupiter", "DisplayNameResolverTest", "NestedTest")
            displayName.displayName shouldBe "test_name(DisplayName)"
        }
    }
}
