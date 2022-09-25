package com.bkahlert.kommons_deprecated.test.junit

import com.bkahlert.kommons_deprecated.test.allTests
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

class Verbosity : TestExecutionListener {

    companion object {
        // Hack but wouldn't know how else to get the number of tests
        // in order to activate logging.
        var testCount: Int? = null

        val isVerbose: Boolean get() = testCount == 1
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        testCount = testPlan.allTests.size
    }
}
