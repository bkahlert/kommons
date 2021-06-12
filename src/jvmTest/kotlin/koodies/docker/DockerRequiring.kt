package koodies.docker

import koodies.docker.CleanUpMode.FailAndKill
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.conditionallyVerboseLogger
import koodies.test.Slow
import koodies.test.UniqueId.Companion.simplifiedId
import koodies.test.testName
import koodies.test.withAnnotation
import koodies.text.Semantics.formattedAs
import koodies.text.quoted
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
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.reflect.KClass

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 *
 * The [Timeout] is automatically increased to 2 minutes.
 *
 * Also a parameter of type [DockerContainer] is provided with the following properties:
 * - Before the test starts, the container is checked to not be in [Running] state. Otherwise the container will be force removed.
 * - After the test finished, the container is checked to not be in [Running] state. If it is it will be force removed.
 * - The default [mode] [FailAndKill] leads to a failing tests if the container is still running. Specify [ThanksForCleaningUp] to change that behaviour.
 * checks
 */
@Slow
@Retention(RUNTIME)
@Target(ANNOTATION_CLASS, CLASS, FUNCTION)
@Extensions(
    ExtendWith(DockerRunningCondition::class),
    ExtendWith(TestContainerCheck::class)
)
annotation class DockerRequiring(
    val value: Array<KClass<out DockerImage>> = [],
    val mode: CleanUpMode = FailAndKill,
    val logging: Boolean = false,
)

enum class CleanUpMode {
    ThanksForCleaningUp, FailAndKill
}

class TestContainerCheck : BeforeEachCallback, AfterEachCallback, TypeBasedParameterResolver<DockerContainer>() {

    private val ExtensionContext.logger: FixedWidthRenderingLogger
        get() = conditionallyVerboseLogger(withAnnotation<ContainersTestFactory, Boolean> { logging } ?: withAnnotation<ContainersTest, Boolean> { logging })

    override fun beforeEach(context: ExtensionContext) = with(context) {
        pullRequiredImages()

        with(context.uniqueContainer()) {
            if (logger.containerState is Running) {
                logger.logLine { "Container $name is (still) running and will be removed forcibly." }
                remove(force = true)
            }
        }
    }

    override fun afterEach(context: ExtensionContext) = with(context) {

        with(uniqueContainer()) {
            when (withAnnotation<DockerRequiring, CleanUpMode?> { mode }) {
                ThanksForCleaningUp -> {
                    if (logger.containerState is Running) remove(force = true, logger = logger)
                }
                FailAndKill -> {
                    check(logger.containerState !is Running) {
                        remove(true)
                        "Container $name was still running and had to be removed forcibly."
                    }
                }
                else -> {
                    if (logger.containerState is Running) {
                        logger.logLine { "Container $name is still running… just saying".formattedAs.debug }
                    }
                }
            }
        }
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): DockerContainer =
        extensionContext.uniqueContainer()

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
            else disabled("Test ${testName.quoted} disabled because Docker was NOT found running.")
        }
}
