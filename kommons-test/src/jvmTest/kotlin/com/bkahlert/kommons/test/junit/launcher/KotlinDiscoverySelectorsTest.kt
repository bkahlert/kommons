package com.bkahlert.kommons.test.junit.launcher

import com.bkahlert.kommons.test.junit.launcher.KotlinDiscoverySelectorsTest.FooTest.NestedTest
import com.bkahlert.kommons.test.junit.launcher.KotlinDiscoverySelectorsTest.FooTest.NestedTest.DeepNestedTest
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors

class KotlinDiscoverySelectorsTest {

    @Test fun select_class() = testAll {
        KotlinDiscoverySelectors.selectKotlinClass(
            FooTest::class,
        ) shouldBe DiscoverySelectors.selectClass(
            FooTest::class.java,
        )
    }

    @Test fun select_method() = testAll {
        KotlinDiscoverySelectors.selectKotlinMemberFunction(
            FooTest::class,
            FooTest::test_foo,
        ) shouldBe DiscoverySelectors.selectMethod(
            FooTest::class.java,
            "test_foo"
        )
    }

    @Test fun select_nested_class() = testAll(
        KotlinDiscoverySelectors.selectNestedKotlinClass(
            listOf(FooTest::class),
            NestedTest::class,
        ),
        KotlinDiscoverySelectors.selectNestedKotlinClass(
            NestedTest::class,
            FooTest::class,
        ),
    ) {
        it shouldBe DiscoverySelectors.selectNestedClass(
            listOf(FooTest::class.java),
            NestedTest::class.java,
        )
    }

    @Test fun select_nested_method() = testAll(
        KotlinDiscoverySelectors.selectNestedKotlinMemberFunction(
            listOf(FooTest::class),
            NestedTest::class,
            NestedTest::test_nested,
        ),
        KotlinDiscoverySelectors.selectNestedKotlinMemberFunction(
            NestedTest::test_nested,
            NestedTest::class,
            FooTest::class,
        ),
    ) {
        it shouldBe DiscoverySelectors.selectNestedMethod(
            listOf(FooTest::class.java),
            NestedTest::class.java,
            "test_nested"
        )
    }

    @Test fun select() = testAll {
        KotlinDiscoverySelectors.select(
            FooTest::class,
        ) shouldBe DiscoverySelectors.selectClass(
            FooTest::class.java,
        )

        KotlinDiscoverySelectors.select(
            FooTest::test_foo,
        ) shouldBe DiscoverySelectors.selectMethod(
            FooTest::class.java,
            "test_foo"
        )

        KotlinDiscoverySelectors.select(
            NestedTest::class,
        ) shouldBe DiscoverySelectors.selectNestedClass(
            listOf(FooTest::class.java),
            NestedTest::class.java,
        )

        KotlinDiscoverySelectors.select(
            NestedTest::test_nested,
        ) shouldBe DiscoverySelectors.selectNestedMethod(
            listOf(FooTest::class.java),
            NestedTest::class.java,
            "test_nested"
        )

        KotlinDiscoverySelectors.select(
            DeepNestedTest::class,
        ) shouldBe DiscoverySelectors.selectNestedClass(
            listOf(FooTest::class.java, NestedTest::class.java),
            DeepNestedTest::class.java,
        )

        KotlinDiscoverySelectors.select(
            DeepNestedTest::test_deep_nested,
        ) shouldBe DiscoverySelectors.selectNestedMethod(
            listOf(FooTest::class.java, NestedTest::class.java),
            DeepNestedTest::class.java,
            "test_deep_nested"
        )
    }

    internal class FooTest {

        @Test
        fun test_foo() {
            "foo" shouldBe "foo"
        }

        @Nested
        inner class NestedTest {

            @Test
            fun test_nested() {
                "nested" shouldBe "nested"
            }

            @Nested
            inner class DeepNestedTest {
                @Test
                fun test_deep_nested() {
                    "deep_nested" shouldBe "deep_nested"
                }
            }
        }
    }
}
