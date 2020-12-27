package koodies.docker

import koodies.test.junit.debug.Debug
import koodies.test.junit.isAnnotated
import koodies.test.junit.testName
import koodies.text.quoted
import koodies.text.styling.Boxes.Companion.wrapWithBox
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

class DockerRequiredCondition : ExecutionCondition {
    fun ExtensionContext.getEnabledDueToAbsentDebugAnnotation() =
        enabled("Neither ${testName.quoted} nor any other test is annotated with @${Debug::class.simpleName}.")

    val dockerUpAndRunning get() = Docker.isEngineRunning
    val annotation = "@${DockerRequiring::class.simpleName}"
    fun ExtensionContext.getEnabledCondition() = enabled("No $annotation annotation at test ${testName.quoted} found.")
    fun ExtensionContext.getDisabledCondition() = disabled("Test ${testName.quoted} is annotated with $annotation but no Docker is running.".wrapWithBox())

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        return if (context.isAnnotated<DockerRequiring>() && !dockerUpAndRunning) context.getDisabledCondition()
        else context.getEnabledCondition()
    }
}


