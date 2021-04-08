package koodies.docker

import koodies.docker.CleanUpMode.FailAndKill
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.terminal.AnsiColors.cyan
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.withAnnotation
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

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
annotation class DockerRequiring(val requiredImages: Array<String> = [], val mode: CleanUpMode = FailAndKill)

enum class CleanUpMode {
    ThanksForCleaningUp, FailAndKill
}

class DockerContainerLifeCycleCheck : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        context.pullRequiredImages()

        val container = context.dockerContainer()
        check(!container.isRunning) { "Container $container is already running." }
    }

    override fun afterEach(context: ExtensionContext) {
        with(BACKGROUND) {
            val container = context.dockerContainer()
            when (context.withAnnotation<DockerRequiring, CleanUpMode?> { mode }) {
                ThanksForCleaningUp -> {
                    if (container.isRunning) container.remove(force = true)
                }
                FailAndKill -> {
                    check(!container.isRunning) {
                        container.remove(true)
                        "Container $container was still running and had to be killed forcibly."
                    }
                }
                else -> {
                    if (container.isRunning) println("Container $container is still running... just saying".cyan())
                }
            }
        }
    }

    private fun ExtensionContext.dockerContainer() = DockerContainer(UniqueId(uniqueId).simple)

    private fun ExtensionContext.pullRequiredImages() =
        BACKGROUND.logging("Pulling required images") {
            val missing = requiredDockerImages() subtract Docker.images.list()
            missing.forEach { it.pull() }
        }

    private fun ExtensionContext.requiredDockerImages(): List<DockerImage> =
        withAnnotation<DockerRequiring, List<DockerImage>> {
            requiredImages.map { DockerImage.parse(it) }
        } ?: emptyList()
}
