package com.bkahlert.kommons.test.junit.launcher

import com.bkahlert.kommons.test.junit.launcher.KotlinDiscoverySelectors.select
import com.bkahlert.kommons.test.junit.launcher.KotlinDiscoverySelectors.selectKotlinClass
import com.bkahlert.kommons.test.junit.launcher.KotlinDiscoverySelectors.selectNestedKotlinMemberFunction
import com.bkahlert.kommons.test.junit.launcher.LaunchersTest.BarTest.BazTest
import com.bkahlert.kommons.test.junit.launcher.TestExecutionReporter.Companion.disableTestExecutionReporter
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod
import org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedClass
import org.junit.platform.engine.discovery.DiscoverySelectors.selectNestedMethod
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder

class LaunchersTest {

    @Test fun launch_tests() = testAll {
        launchTests(
            selectClass(FooTest::class.java),
            selectClass(BarTest::class.java),
        ) { request(testLauncherDiscoveryRequestInit) } should {
            it.testsSucceededCount shouldBe 3
            it.testsFailedCount shouldBe 0
        }

        launchTests(
            selectClass(FooTest::class.java),
            selectMethod(BarTest::class.java, "test_bar"),
        ) { request(testLauncherDiscoveryRequestInit) } should {
            it.testsSucceededCount shouldBe 2
            it.testsFailedCount shouldBe 0
        }

        launchTests(
            selectClass(FooTest::class.java),
            selectNestedClass(listOf(BarTest::class.java), BazTest::class.java),
        ) { request(testLauncherDiscoveryRequestInit) } should {
            it.testsSucceededCount shouldBe 2
            it.testsFailedCount shouldBe 0
        }

        launchTests(
            selectClass(FooTest::class.java),
            selectNestedMethod(listOf(BarTest::class.java), BazTest::class.java, "test_baz"),
        ) { request(testLauncherDiscoveryRequestInit) } should {
            it.testsSucceededCount shouldBe 2
            it.testsFailedCount shouldBe 0
        }

        launchTests(
            selectKotlinClass(FooTest::class),
            selectNestedKotlinMemberFunction(BazTest::test_baz, BazTest::class, BarTest::class),
        ) { request(testLauncherDiscoveryRequestInit) } should {
            it.testsSucceededCount shouldBe 2
            it.testsFailedCount shouldBe 0
        }


        launchTests(
            selectClass(FooTest::class.java),
            selectNestedMethod(listOf(BarTest::class.java), BazTest::class.java, "test_baz"),
        ) { request(testLauncherDiscoveryRequestInit) } should {
            it.testsSucceededCount shouldBe 2
            it.testsFailedCount shouldBe 0
        }

        launchTests(
            select(FooTest::class),
            select(BazTest::test_baz),
        ) { request(testLauncherDiscoveryRequestInit) } should {
            it.testsSucceededCount shouldBe 2
            it.testsFailedCount shouldBe 0
        }
    }

    internal companion object {
        val testLauncherDiscoveryRequestInit: LauncherDiscoveryRequestBuilder.() -> Unit = {
            disableTestExecutionReporter()
        }
    }

    internal class FooTest {
        @Test
        fun test_foo() {
            "foo" shouldBe "foo"
        }
    }

    internal class BarTest {
        @Test
        fun test_bar() {
            "bar" shouldBe "bar"
        }

        @Nested
        inner class BazTest {
            @Test
            fun test_baz() {
                "baz" shouldBe "baz"
            }
        }
    }
}
