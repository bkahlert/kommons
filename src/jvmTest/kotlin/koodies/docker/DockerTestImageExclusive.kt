package koodies.docker

import koodies.concurrent.process.CommandLine
import koodies.logging.MutedRenderingLogger
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceLock

/**
 * Declares a requirement on a Docker image used for testing.
 * Using the annotations provides exclusive access to the named
 * resources but in contrast to [Isolated] tests using a different
 * image can run in parallel.
 */
@ResourceLock(DockerTestImageExclusive.RESOURCE, mode = ResourceAccessMode.READ_WRITE)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DockerTestImageExclusive {
    companion object {
        public const val RESOURCE: String = "koodies.docker.test-image"
        public val DOCKER_TEST_IMAGE: DockerTestImageProvider = DockerTestImageProvider("hello-world")
        public val DOCKER_TEST_CONTAINER: DockerTestContainerProvider = DockerTestContainerProvider(
            "koodies.docker.test-container",
            DockerImage { official("ubuntu") },
            CommandLine("sleep", "10")
        )
    }
}

/**
 * Provider of a [DockerImage] with non-logging / non-printing implementations
 * of [pull] and [remove] for the purpose of testing.
 */
public inline class DockerTestImageProvider(val image: DockerImage) {
    constructor(officialName: String) : this(DockerImage(officialName, emptyList(), null, null))

    /**
     * Silently pulls this [DockerImage].
     */
    fun pull() {
        with(MutedRenderingLogger()) { image.pull {} }
    }

    val isPulled: Boolean
        get() = with(MutedRenderingLogger()) { image.isPulled() }

    /**
     * Silently removes this [DockerImage].
     */
    fun remove() {
        with(MutedRenderingLogger()) { runCatching { image.removeImage {} } }
    }

    override fun toString(): String = image.toString()
}

public class DockerTestContainerProvider(name: String, val image: DockerImage, private val commandLine: CommandLine) {

    val container: DockerContainer = DockerContainer(name)

    /**
     * Silently starts this [DockerContainer].
     */
    fun start() {
        commandLine.executeDockerized(image) {
            dockerOptions { container by this@DockerTestContainerProvider.container }
            executionOptions { processing { async } }
            null
        }
    }

    /**
     * Silently removes this [DockerContainer].
     */
    val isRunning: Boolean
        get() = with(MutedRenderingLogger()) { container.isRunning() }


    /**
     * Silently removes this [DockerContainer].
     */
    fun stop() {
        with(MutedRenderingLogger()) { runCatching { container.stop {} } }
    }

    /**
     * Silently removes this [DockerContainer].
     */
    fun remove() {
        with(MutedRenderingLogger()) { runCatching { container.remove {} } }
    }

    override fun toString(): String = container.toString()
}
