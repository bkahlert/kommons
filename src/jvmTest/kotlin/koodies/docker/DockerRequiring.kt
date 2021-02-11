package koodies.docker

import koodies.concurrent.output
import koodies.concurrent.script
import koodies.docker.CleanUpMode.FailAndKill
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.logging.blockLogging
import koodies.terminal.AnsiColors.cyan
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.withAnnotation
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ
import org.junit.jupiter.api.parallel.ResourceLock

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 *
 * The [Timeout] is automatically increased to 2 minutes.
 */
@Slow
@ResourceLock(DockerResources.SERIAL, mode = READ)
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

        val name = context.containerName()
        check(!Docker.isContainerRunning(name)) { "Container $name is already running." }
    }

    override fun afterEach(context: ExtensionContext) {
        val name = context.containerName()
        when (context.withAnnotation<DockerRequiring, CleanUpMode?> { mode }) {
            ThanksForCleaningUp -> {
                if (Docker.isContainerRunning(name)) Docker.remove(name, forcibly = true)
            }
            FailAndKill -> {
                check(!Docker.isContainerRunning(name)) {
                    Docker.remove(name, forcibly = true)
                    "Container $name was still running and had to be killed forcibly."
                }
            }
            else -> {
                if (Docker.isContainerRunning(name)) println("Container $name is still running... just saying".cyan())
            }
        }
    }

    private fun ExtensionContext.containerName() = UniqueId(uniqueId).simple

    private fun ExtensionContext.pullRequiredImages() {
        withAnnotation<DockerRequiring, List<DockerImage>> {
            requiredImages.map { DockerImage.parse(it) }
        }?.subtract(Docker.images)?.forEach {
            blockLogging("Downloading required Docker image $it") {
                script(logger = this) { !"docker pull $it" }.output()
            }
        }
    }
}
