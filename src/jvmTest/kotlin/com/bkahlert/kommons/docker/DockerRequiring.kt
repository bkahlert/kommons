package com.bkahlert.kommons.docker

import com.bkahlert.kommons.docker.CleanUpMode.FailAndKill
import com.bkahlert.kommons.docker.CleanUpMode.ThanksForCleaningUp
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Running
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.junit.TestName.Companion.testName
import com.bkahlert.kommons.test.junit.UniqueId.Companion.simplifiedId
import com.bkahlert.kommons.test.withAnnotation
import com.bkahlert.kommons.text.ANSI.FilteringFormatter
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.quoted
import com.bkahlert.kommons.text.styling.wrapWithBorder
import com.bkahlert.kommons.tracing.rendering.BackgroundPrinter
import com.bkahlert.kommons.tracing.rendering.Styles.None
import com.bkahlert.kommons.tracing.rendering.spanningLine
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kommons.tracing.spanScope
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
)

enum class CleanUpMode {
    ThanksForCleaningUp, FailAndKill
}

class TestContainerCheck : BeforeEachCallback, AfterEachCallback, TypeBasedParameterResolver<DockerContainer>() {

    override fun beforeEach(context: ExtensionContext) = with(context) {
        runSpanning("Checking for required images and running containers", style = None, printer = BackgroundPrinter) {
            pullRequiredImages()

            with(context.uniqueContainer()) {
                if (containerState is Running) {
                    log("Container $name is (still) running and will be removed forcibly.")
                    remove(force = true)
                }
            }
        }
    }

    override fun afterEach(context: ExtensionContext) = with(context) {
        runSpanning("Cleaning up", style = None, printer = BackgroundPrinter) {
            with(uniqueContainer()) {
                when (withAnnotation<DockerRequiring, CleanUpMode?> { mode }) {
                    ThanksForCleaningUp -> {
                        if (containerState is Running) remove(force = true)
                    }
                    FailAndKill -> {
                        check(containerState !is Running) {
                            remove(true)
                            "Container $name was still running and had to be removed forcibly."
                        }
                    }
                    else -> {
                        if (containerState is Running) {
                            spanScope {
                                log("Container $name is still running… just saying".formattedAs.debug)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): DockerContainer =
        extensionContext.uniqueContainer()

    private fun ExtensionContext.uniqueContainer() = DockerContainer(simplifiedId)

    private fun ExtensionContext.pullRequiredImages() =
        spanningLine("Pulling required images") {
            val missing = requiredDockerImages() subtract DockerImage.list()
            missing.forEach { it.pull() }
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

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        context.testName.let { testName ->
            if (dockerUpAndRunning) enabled("Test ${testName.quoted} enabled because Docker is found running.")
            else disabled("Test ${testName.quoted} disabled because Docker was NOT found running.")
        }

    private companion object {
        private val dockerUpAndRunning: Boolean by lazy {
            Docker.engineRunning
                .also { isRunning ->
                    val message = if (isRunning) {
                        listOf("Docker is running.".formattedAs.success)
                            .wrapWithBorder(padding = 2, margin = 0, formatter = FilteringFormatter.fromScratch { yellow })
                    } else {
                        listOf("Docker is not running.".formattedAs.warning)
                            .wrapWithBorder(padding = 2, margin = 0, formatter = FilteringFormatter.fromScratch { yellow })
                    }

                        .also { println(it) }
                }
        }
    }
}
