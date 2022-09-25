package com.bkahlert.kommons_deprecated.test.junit

import com.bkahlert.kommons_deprecated.docker.Docker
import com.bkahlert.kommons_deprecated.docker.DockerRequiring
import com.bkahlert.kommons_deprecated.printTestExecutionStatus
import com.bkahlert.kommons_deprecated.test.allTestJavaMethods
import com.bkahlert.kommons_deprecated.test.withAnnotation
import com.bkahlert.kommons_deprecated.text.Semantics.formattedAs
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

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
            printTestExecutionStatus(
                "Docker is not running: ${skipped.size} tests skipped!".formattedAs.warning,
            )
        }
    }
}
