package koodies.test.junit

import koodies.test.Debug
import koodies.test.allContainerJavaClasses
import koodies.test.allTestJavaMethods
import koodies.test.withAnnotation
import koodies.test.withoutAnnotation
import koodies.text.ANSI.Formatter.Companion.fromScratch
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import koodies.text.styling.draw
import koodies.text.styling.wrapWithBorder
import org.junit.jupiter.api.extension.TestWatcher
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.AnnotatedElement
import kotlin.properties.Delegates
import kotlin.time.milliseconds

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
        val timeNeeded = (System.currentTimeMillis() - startTimestamp).milliseconds
        checkDebug(testPlan)

        if (failedTestsCount == 0) {
            val allTestClasses = testPlan.allContainerJavaClasses
            val notAnnotatedTestClasses = testPlan.allContainerJavaClasses.withoutAnnotation<Execution>()
            val nonConcurrentTestClasses = testPlan.allContainerJavaClasses.withAnnotation<Execution> { it.value != CONCURRENT }

            if (notAnnotatedTestClasses.isEmpty()) {
                val quantity =
                    if (nonConcurrentTestClasses.isEmpty()) "All" else "${allTestClasses.size - nonConcurrentTestClasses.size}/${allTestClasses.size}"
                listOf(
                    "Done. All tests passed within ${timeNeeded.formattedAs.debug}.".ansi.bold,
                    "$quantity test containers run concurrently."
                )
                    .joinToString(LF)
                    .wrapWithBorder(padding = 2, margin = 1, formatter = fromScratch { green })
                    .also { println(it) }
            } else {
                listOf(
                    "Done. All tests passed within ${timeNeeded.formattedAs.warning}".ansi.bold,
                    "",
                    "Non-concurrent test classes: ".ansi.bold + format(nonConcurrentTestClasses),
                    "",
                    "Missing @Execution annotation: ".ansi.bold + format(notAnnotatedTestClasses),
                )
                    .joinToString(LF)
                    .draw.border.double(padding = 2, margin = 1, fromScratch { yellow })
                    .also { println(it) }
            }
        } else {
            listOf(
                "Done. BUT $failedTestsCount tests failed!".ansi.bold,
            )
                .joinToString(LF)
                .draw.border.spikedOutward(padding = 2, margin = 1, fromScratch { red })
                .also { println(it) }
        }
    }

    private fun checkDebug(testPlan: TestPlan) {
        val debugAnnotatedMethods = testPlan.allTestJavaMethods.withAnnotation<Debug> { debug -> debug.includeInReport }
        if (debugAnnotatedMethods.isNotEmpty()) {
            listOf(
                "Attention!".formattedAs.warning,
                "You only see the results of the",
                "${debugAnnotatedMethods.size} @${Debug::class.simpleName} annotated tests.",
                "Don't forget to remove them.".ansi.bold,
            )
                .joinToString(LF)
                .wrapWithBorder(padding = 2, margin = 1, formatter = fromScratch { yellow })
                .also { println(it) }
        }
    }

    private fun format(nonConcurrentTestClasses: Collection<AnnotatedElement>): String =
        if (nonConcurrentTestClasses.isEmpty()) "—" else nonConcurrentTestClasses.joinToString(LF,
            prefix = LF,
            limit = 10) { "$it" }
}
