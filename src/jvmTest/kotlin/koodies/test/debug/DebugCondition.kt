package koodies.test.debug

import koodies.test.Debug
import koodies.test.allTests
import koodies.test.isA
import koodies.test.testName
import koodies.text.quoted
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Checks if a test (container) is annotated with [Debug] and if so, disables all other non-annotated tests.
 */
class DebugCondition : ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        when (context.isDebugMode) {
            true -> when (context.currentIsDebug) {
                true -> context.getEnabledDueToDebugAnnotation()
                else -> context.getDisabledDueToSiblingDebugAnnotation()
            }
            else -> context.getEnabledDueToAbsentDebugAnnotation()
        }

    companion object {
        val ExtensionContext.isDebugMode: Boolean get() = anyDebugTest && testMethod.isPresent
        val ExtensionContext.currentIsDebug: Boolean get() = element.isA<Debug>()
        val ExtensionContext.anyDebugTest: Boolean get() = allTests.any { it.isA<Debug>() }

        private fun ExtensionContext.getEnabledDueToAbsentDebugAnnotation(): ConditionEvaluationResult =
            enabled("Neither ${testName.quoted} nor any other test is annotated with ${Debug::class.simpleName}.")

        private fun ExtensionContext.getEnabledDueToDebugAnnotation(): ConditionEvaluationResult =
            enabled("Test ${testName.quoted} is annotated with ${Debug::class.simpleName}.")

        private fun ExtensionContext.getDisabledDueToSiblingDebugAnnotation(): ConditionEvaluationResult =
            disabled("Test ${testName.quoted} skipped due to existing ${Debug::class.simpleName} annotation on another test.")
    }
}
