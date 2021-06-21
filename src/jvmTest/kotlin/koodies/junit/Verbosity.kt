package koodies.junit

import koodies.debug.Debug
import koodies.debug.DebugCondition.Companion.currentIsDebug
import koodies.test.allTests
import koodies.test.isA
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

class Verbosity : TestExecutionListener {

    companion object {
        // Hack but wouldn't know how else to get the number of tests
        // in order to activate logging.
        var testCount: Int? = null
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        testCount = testPlan.allTests.size
    }
}

val ExtensionContext.isVerbose: Boolean get() = currentIsDebug || Verbosity.testCount == 1
val ParameterContext.isVerbose: Boolean get() = parameter.isA<Debug>()
