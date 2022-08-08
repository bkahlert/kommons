package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
import org.junit.platform.engine.UniqueId.parse

class SimpleIdTest {

    @Test fun test_name(simpleId: SimpleId) = testAll {
        simpleId.toString() shouldBe listOf(
            "SimpleIdTest",
            "test_name",
        ).joinToString(".")
    }

    @Nested
    inner class NestedTest {

        @Test fun test_name(simpleId: SimpleId) = testAll {
            simpleId.toString() shouldBe listOf(
                "SimpleIdTest",
                "NestedTest",
                "test_name",
            ).joinToString(".")
        }
    }

    @Test fun `test name`(simpleId: SimpleId, @Suppress("UNUSED_PARAMETER") `with differing arguments`: TestInfo) {
        simpleId.toString() shouldBe "SimpleIdTest.test_name-TestInfo"
    }

    @Test fun `test name`(@Suppress("UNUSED_PARAMETER") `simple id goes second`: TestInfo, simpleId: SimpleId) {
        simpleId.toString() shouldBe "SimpleIdTest.test_name-TestInfo"
    }

    @TestFactory fun dynamic(simpleId: SimpleId): DynamicContainer =
        dynamicContainer("dynamic container", listOf(
            dynamicTest("dynamic test/container") {
                simpleId.toString() shouldBe "SimpleIdTest.dynamic"
            },
            dynamicTest("dynamic test") {
                simpleId.toString() shouldBe "SimpleIdTest.dynamic"
            }
        ))

    @Test fun from() = testAll {
        SimpleId.from(parse("[engine:junit-jupiter]/[class:com.bkahlert.kommons.test.SimpleIdTest]/[method:from()]"))
            .toString().shouldBe("SimpleIdTest.from")
        SimpleId.from(StackTraceElement(SimpleIdTest::class.java.name, "from", "SimpleIdTest.kt", 1135))
            .toString().shouldBe("SimpleIdTest.from")
    }
}
