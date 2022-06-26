package com.bkahlert.kommons.test.junit

import com.bkahlert.kommons.docker.Docker
import com.bkahlert.kommons.docker.DockerRequiring
import com.bkahlert.kommons.printTestExecutionStatus
import com.bkahlert.kommons.test.allTestJavaMethods
import com.bkahlert.kommons.test.withAnnotation
import com.bkahlert.kommons.text.Semantics.formattedAs
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.Method

/** [TestExecutionListener] that warns about skipped Docker tests. */
class SkippedDockerTestsReporter : TestExecutionListener {

    @Suppress("KDocMissingDocumentation")
    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        super.testPlanExecutionFinished(testPlan)
        checkSkippedDockerTests(testPlan)
    }

    private fun checkSkippedDockerTests(testPlan: TestPlan) {
        if (Docker.engineRunning) return

        val skipped = testPlan.allTestJavaMethods.withAnnotation<DockerRequiring>(ancestorsIgnored = false)
        if (skipped.isNotEmpty()) {
            val groupBy = skipped.groupBy { testElement: Any ->
                val testContainer = (testElement as? Method)?.declaringClass ?: testElement
                DynamicTestDisplayNameGenerator.displayNameFor(testContainer)
            }.map { (group, elements) -> "$group: ${elements.size}" }

            printTestExecutionStatus(
                "Docker is not running: ${skipped.size} tests skipped!".formattedAs.warning,
                *groupBy.toTypedArray(),
            ) { yellow }
        }
    }
}
