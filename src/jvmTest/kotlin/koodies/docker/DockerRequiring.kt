package koodies.docker

import koodies.collections.synchronizedMapOf
import koodies.docker.CleanUpMode.FailAndKill
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.logging.conditionallyVerboseLogger
import koodies.test.Slow
import koodies.test.UniqueId.Companion.simplifiedId
import koodies.test.testName
import koodies.test.withAnnotation
import koodies.text.Semantics.formattedAs
import koodies.text.quoted
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.Extensions
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
@Extensions(
    ExtendWith(DockerRunningCondition::class),
    ExtendWith(TestContainerCheck::class)
)
annotation class DockerRequiring(val value: Array<KClass<out DockerImage>> = [], val mode: CleanUpMode = FailAndKill)

enum class CleanUpMode {
    ThanksForCleaningUp, FailAndKill
}


class TestContainerCheck : BeforeEachCallback, AfterEachCallback {

    private val ExtensionContext.logger get() = conditionallyVerboseLogger()

    override fun beforeEach(context: ExtensionContext) = with(context) {
        pullRequiredImages()

        with(context.uniqueContainer()) {
            if (logger.state is Running) {
                logger.logLine { "Container $name is (still) running and will be removed forcibly." }
                remove(force = true)
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
                        "Container $name was still running and had to be removed forcibly."
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

/**
 * Conditions that disables the test if no running Docker can be found.
 */
class DockerRunningCondition : ExecutionCondition {

    private val dockerUpAndRunning: Boolean by lazy { Docker.engineRunning }

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        context.testName.let { testName ->
            if (dockerUpAndRunning) enabled("Test ${testName.quoted} enabled because Docker is found running.")
            else disabled("Test ${testName.quoted} enabled because Docker is found running.")
                .also { skipped[context.element] = testName }
        }

    companion object {
        var skipped: MutableMap<Any, String> = synchronizedMapOf()
    }
}
