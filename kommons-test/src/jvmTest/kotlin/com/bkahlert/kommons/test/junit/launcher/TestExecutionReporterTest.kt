package com.bkahlert.kommons.test.junit.launcher

import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.ansiRemoved
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Test
import org.junit.platform.engine.ConfigurationParameters
import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestDescriptor.Type
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.TestTag
import org.junit.platform.engine.UniqueId
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.util.Optional

class TestExecutionReporterTest {

    @Test fun no_tests() = testAll {
        testExecutionReporterOutput(0, 0, 0, 0) shouldMatch """
            ⁉︎ no tests executed
        """.trimIndent()
    }

    @Test fun successful_tests() = testAll {
        testExecutionReporterOutput(1, 0, 0, 0) shouldMatch """
            1 test within \d+m?s: ✔︎ all passed
        """.trimIndent()
        testExecutionReporterOutput(2, 0, 0, 0) shouldMatch """
            2 tests within \d+m?s: ✔︎ all passed
        """.trimIndent()
    }

    @Test fun aborted_tests() = testAll {
        testExecutionReporterOutput(0, 0, 1, 0) shouldMatch """
            1 test within \d+m?s: ϟ all crashed
        """.trimIndent()
        testExecutionReporterOutput(0, 0, 2, 0) shouldMatch """
            2 tests within \d+m?s: ϟ all crashed
        """.trimIndent()
        testExecutionReporterOutput(1, 0, 2, 0) shouldMatch """
            3 tests within \d+m?s: ϟ 2 crashed, ✔︎ 1 passed
        """.trimIndent()
    }

    @Test fun failed_tests() = testAll {
        testExecutionReporterOutput(0, 1, 0, 0) shouldMatch """
            1 test within \d+m?s: ✘︎ all failed
        """.trimIndent()
        testExecutionReporterOutput(0, 2, 0, 0) shouldMatch """
            2 tests within \d+m?s: ✘︎ all failed
        """.trimIndent()
        testExecutionReporterOutput(1, 2, 0, 0) shouldMatch """
            3 tests within \d+m?s: ✘︎ 2 failed, ✔︎ 1 passed
        """.trimIndent()
    }

    @Test fun failed_and_failed_tests() = testAll {
        testExecutionReporterOutput(1, 2, 1, 0) shouldMatch """
            4 tests within \d+m?s: ✘︎ 2 failed, ϟ 1 crashed, ✔︎ 1 passed
        """.trimIndent()
        testExecutionReporterOutput(1, 1, 2, 0) shouldMatch """
            4 tests within \d+m?s: ✘︎ 1 failed, ϟ 2 crashed, ✔︎ 1 passed
        """.trimIndent()
    }

    @Test fun skipped_tests() = testAll {
        testExecutionReporterOutput(1, 1, 1, 2) shouldMatch """
            3 tests within \d+m?s: ✘︎ 1 failed, ϟ 1 crashed, ✔︎ 1 passed, ◍ 2 ignored
        """.trimIndent()
        testExecutionReporterOutput(2, 2, 2, 2) shouldMatch """
            6 tests within \d+m?s: ✘︎ 2 failed, ϟ 2 crashed, ✔︎ 2 passed, ◍ 2 ignored
        """.trimIndent()
        testExecutionReporterOutput(2, 0, 0, 2) shouldMatch """
            2 tests within \d+m?s: ✔︎ all passed, ◍ all ignored
        """.trimIndent()
    }
}

private fun testExecutionReporterOutput(
    passed: Int,
    failed: Int,
    aborted: Int,
    skipped: Int,
    sanitize: Boolean = true,
): String {
    val testPlan = TestPlan.from(emptyList(), configurationParameters())
    val lines = mutableListOf<String>()
    TestExecutionReporter { lines.add(it) }.apply {
        testPlanExecutionStarted(testPlan)
        repeat(passed) {
            executionStarted(testIdentifier())
            executionFinished(testIdentifier(), TestExecutionResult.successful())
        }
        repeat(failed) {
            executionStarted(testIdentifier())
            executionFinished(testIdentifier(), TestExecutionResult.failed(RuntimeException()))
        }
        repeat(aborted) {
            executionStarted(testIdentifier())
            executionFinished(testIdentifier(), TestExecutionResult.aborted(RuntimeException()))
        }
        repeat(skipped) {
            executionSkipped(testIdentifier(), null)
        }
        testPlanExecutionFinished(testPlan)
    }
    return lines.joinToString("\n").let { if (sanitize) it.ansiRemoved.trim() else it }
}

private fun configurationParameters(vararg entries: Pair<String?, String>) =
    object : ConfigurationParameters {
        override fun get(key: String?): Optional<String> =
            Optional.ofNullable(entries.firstOrNull { it.first == key }?.second)

        override fun getBoolean(key: String?): Optional<Boolean> =
            get(key).flatMap { Optional.ofNullable(it.toBoolean()) }

        @Deprecated("use keySet", ReplaceWith("keySet.size()"))
        override fun size(): Int = entries.size
        override fun keySet(): MutableSet<String> = entries.mapNotNull { it.first }.toMutableSet()
    }

private fun testIdentifier(testDescriptor: TestDescriptor = testDescriptor()): TestIdentifier =
    TestIdentifier.from(testDescriptor)

private fun testDescriptor(): TestDescriptor =
    object : TestDescriptor {
        override fun getUniqueId(): UniqueId =
            UniqueId.parse("[class:foo.FooTest]/[method:bar(baz.Baz)]")

        override fun getDisplayName(): String = "bar(Baz)"
        override fun getTags(): MutableSet<TestTag> = mutableSetOf()
        override fun getSource(): Optional<TestSource> = Optional.empty()
        override fun getParent(): Optional<TestDescriptor> = Optional.empty()
        override fun setParent(parent: TestDescriptor?) = throw RuntimeException("not implemented")
        override fun getChildren(): MutableSet<out TestDescriptor> = mutableSetOf()
        override fun addChild(descriptor: TestDescriptor?) = throw RuntimeException("not implemented")
        override fun removeChild(descriptor: TestDescriptor?) = throw RuntimeException("not implemented")
        override fun removeFromHierarchy() = throw RuntimeException("not implemented")
        override fun getType(): Type = Type.TEST
        override fun findByUniqueId(uniqueId: UniqueId?): Optional<out TestDescriptor> = throw RuntimeException("not implemented")
    }
