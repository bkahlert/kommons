package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.docker.Docker
import com.bkahlert.kommons.docker.DockerRequiring
import com.bkahlert.kommons.test.allContainerJavaClasses
import com.bkahlert.kommons.test.allTestJavaMethods
import com.bkahlert.kommons.test.withAnnotation
import com.bkahlert.kommons.text.ANSI.FilteringFormatter.Companion.fromScratch
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.ANSI.ansiRemoved
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.styling.draw
import com.bkahlert.kommons.text.styling.wrapWithBorder
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.toSimpleString
import com.bkahlert.kommons.unit.milli
import org.junit.jupiter.api.extension.TestWatcher
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.Method
import kotlin.properties.Delegates

class TestExecutionReporter : TestExecutionListener, TestWatcher {

    private var startTimestamp by Delegates.notNull<Long>()
    private var failedTestsCount: Int = 0

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        startTimestamp = System.currentTimeMillis()
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.status == TestExecutionResult.Status.FAILED) failedTestsCount++
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        val timeNeeded = (System.currentTimeMillis() - startTimestamp).milli.seconds
        checkSkippedDockerTests(testPlan)

        if (failedTestsCount == 0) {
            val allTestClasses = testPlan.allContainerJavaClasses
            val concurrentTestClasses = testPlan.allContainerJavaClasses.withAnnotation<Execution> { it.value == CONCURRENT }

            val quantity = "All".takeUnless { concurrentTestClasses.size < allTestClasses.size }
                ?: "${concurrentTestClasses.size}/${allTestClasses.size}"

            listOf(
                "Done. All tests passed within ${timeNeeded.formattedAs.debug}.".ansi.bold,
                "$quantity test containers run concurrently."
            )
                .joinLinesToString()
                .draw.border.rounded(padding = 2, margin = 0, formatter = fromScratch { green })
                .also { println(it) }
        } else {
            listOf(
                "Done. BUT $failedTestsCount tests failed!".ansi.bold,
            )
                .joinLinesToString()
                .draw.border.rounded(padding = 2, margin = 0, fromScratch { red })
                .also { println(it) }
        }
    }

    private fun checkSkippedDockerTests(testPlan: TestPlan) {
        if (Docker.engineRunning) return

        val skipped = testPlan.allTestJavaMethods.withAnnotation<DockerRequiring>(ancestorsIgnored = false)
        if (skipped.isNotEmpty()) {
            val groupBy = skipped.groupBy { testElement: Any ->
                val container = (testElement as? Method)?.declaringClass ?: testElement
                container.toSimpleString().ansiRemoved.split(".")[0]
            }.map { (group, elements) -> "$group: ${elements.size}" }

            listOf(
                "Docker is not running: ${skipped.size} tests skipped!".formattedAs.warning,
                *groupBy.toTypedArray(),
            )
                .wrapWithBorder(padding = 2, margin = 0, formatter = fromScratch { yellow })
                .also { println(it) }
        }
    }
}
