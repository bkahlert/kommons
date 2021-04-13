package koodies.docker

import koodies.docker.CleanUpMode.FailAndKill
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.logging.conditionallyVerboseLogger
import koodies.test.Debug
import koodies.test.Slow
import koodies.test.UniqueId.Companion.simplifiedId
import koodies.test.isAnnotated
import koodies.test.testName
import koodies.test.withAnnotation
import koodies.text.Semantics.formattedAs
import koodies.text.quoted
import koodies.text.styling.Boxes.Companion.wrapWithBox
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.reflect.KClass

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 *
 * The [Timeout] is automatically increased to 2 minutes.
 */
@Slow @Tag("docker")
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ExtendWith(DockerContainerLifeCycleCheck::class)
annotation class DockerRequiring(val value: Array<KClass<out DockerImage>> = [], val mode: CleanUpMode = FailAndKill)

enum class CleanUpMode {
    ThanksForCleaningUp, FailAndKill
}

class DockerContainerLifeCycleCheck : BeforeEachCallback, AfterEachCallback {

    private val ExtensionContext.logger get() = conditionallyVerboseLogger()

    override fun beforeEach(context: ExtensionContext) = with(context) {
        pullRequiredImages()

        with(context.uniqueContainer()) {
            check(logger.state !is Running) {
                remove(force = true)
                "Container $name is already running."
            }
        }
    }

    override fun afterEach(context: ExtensionContext) = with(context) {

        with(uniqueContainer()) {
            when (withAnnotation<DockerRequiring, CleanUpMode?> { mode }) {
                ThanksForCleaningUp -> {
                    if (logger.state is Running) remove(force = true, logger = logger)
                }
                FailAndKill -> {
                    check(logger.state !is Running) {
                        remove(true)
                        "Container $name was still running and had to be killed forcibly."
                    }
                }
                else -> {
                    if (logger.state is Running) {
                        logger.logLine { "Container $name is still running... just saying".formattedAs.debug }
                    }
                }
            }
        }
    }

    private fun ExtensionContext.uniqueContainer() = DockerContainer(simplifiedId)

    private fun ExtensionContext.pullRequiredImages() =
        logger.logging("Pulling required images") {
            val missing = requiredDockerImages() subtract DockerImage.list(logger = logger)
            missing.forEach { it.pull(logger = logger) }
        }

    private fun ExtensionContext.requiredDockerImages(): List<DockerImage> =
        withAnnotation<DockerRequiring, List<DockerImage>> {
            value.map { it.objectInstance ?: error("only singletons implemented using `object` are supported") }
        } ?: emptyList()
}

class DockerRequiringCondition : ExecutionCondition {
    fun ExtensionContext.getEnabledDueToAbsentDebugAnnotation() =
        ConditionEvaluationResult.enabled("Neither ${testName.quoted} nor any other test is annotated with @${Debug::class.simpleName}.")

    val dockerUpAndRunning get() = Docker.engineRunning
    val annotation = "@${DockerRequiring::class.simpleName}"
    fun ExtensionContext.getEnabledCondition() = ConditionEvaluationResult.enabled("No $annotation annotation at test ${testName.quoted} found.")
    fun ExtensionContext.getDisabledCondition() =
        ConditionEvaluationResult.disabled("Test ${testName.quoted} is annotated with $annotation but no Docker is running.".wrapWithBox())

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        return if (context.isAnnotated<DockerRequiring>() && !dockerUpAndRunning) context.getDisabledCondition()
        else context.getEnabledCondition()
    }
}
