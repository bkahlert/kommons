package koodies.docker

import koodies.concurrent.execute
import koodies.concurrent.process.CommandLine
import koodies.docker.DockerImage.ImageContext
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.MutedRenderingLogger
import koodies.runWrapping
import koodies.test.UniqueId
import koodies.text.withRandomSuffix
import koodies.toBaseName
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Isolated
import org.junit.jupiter.api.parallel.ResourceAccessMode
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ
import org.junit.jupiter.api.parallel.ResourceLock
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.seconds

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
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 */
@ResourceLock(DockerResources.SERIAL, mode = READ)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ExtendWith(DockerContainerLifeCycleCheck::class)
annotation class WithDockerContainer(val requiredState: State, val expectedState: State) {

    enum class State {
        ThanksForCleaningUp, FailAndKill
    }
}


public class DockerTestRunner(private val baseName: String = "koodies.docker.test-runner") {
    private val image: DockerImage = DockerImage { ImageContext.official("ubuntu") }
    private val cmd: CommandLine = CommandLine("sleep", "10")

    val lock: ReentrantLock = ReentrantLock()
    val container: DockerContainer = DockerContainer { baseName.sanitized }

    public sealed class ContainerState {
        public class ContainerNotExistent : ContainerState()
        public sealed class ContainerExistent : ContainerState() {
            public class Created : ContainerExistent()
            public class Restarting : ContainerExistent()
            public class Running : ContainerExistent()
            public class Removing : ContainerExistent()
            public class Paused : ContainerExistent()
            public class Exited : ContainerExistent()
            public class Dead : ContainerExistent()
        }
    }
//
//    public fun containerWithState(state: State)
//
//    /**
//     * Silently starts this [DockerContainer].
//     */
//    fun start(uniqueName: Boolean = true): DockerProcess {
//        val name = container.name.takeUnless { uniqueName } ?: container.name.withRandomSuffix()
//        return with(MutedRenderingLogger()) {
//            commandLine.executeDockerized(image) {
//                dockerOptions { name { name }; detached { on } }
//                executionOptions {
//                    noDetails("sleeping for 10 seconds")
//                    ignoreExitValue()
//                }
//                null
//            }
//        }
//    }

    /**
     * Silently checks if this [DockerContainer] is running.
     */
//    val isRunning: Boolean
//        get() = with(MutedRenderingLogger()) { container.isRunning }

//    fun <R> use(block: (DockerProcess) -> R) = lock.withLock {
//        runWrapping({ start() }, { stop() }) { block(it) }
}

//
//    /**
//     * Silently removes this [DockerContainer].
//     */
//    fun stop(actual: DockerContainer = container, timeout: Duration = 1.seconds) {
//        with(MutedRenderingLogger()) { actual.stop { timeout { timeout } } }
//    }
//
//    /**
//     * Silently removes this [DockerContainer].
//     */
//    fun remove(actual: DockerContainer = container) {
//        with(MutedRenderingLogger()) { runCatching { actual.remove {} } }
//    }
//
//    override fun toString(): String = container.toString()
//}


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

    val lock: ReentrantLock = ReentrantLock()
    val container: DockerContainer = DockerContainer.from(name)

    /**
     * Silently starts this [DockerContainer].
     */
    fun start(uniqueName: Boolean = true): DockerProcess {
        val name = container.name.takeUnless { uniqueName } ?: container.name.withRandomSuffix()
        return with(MutedRenderingLogger()) {
            commandLine.executeDockerized(image) {
                dockerOptions { name { name }; detached { on } }
                executionOptions {
                    noDetails("sleeping for 10 seconds")
                    ignoreExitValue()
                }
                null
            }
        }
    }

    /**
     * Silently checks if this [DockerContainer] is running.
     */
    val isRunning: Boolean
        get() = with(MutedRenderingLogger()) { container.isRunning }

    fun <R> use(block: (DockerProcess) -> R) = lock.withLock {
        runWrapping({ start() }, { stop() }) { block(it) }
    }


    /**
     * Silently removes this [DockerContainer].
     */
    fun stop(actual: DockerContainer = container, timeout: Duration = 1.seconds) {
        with(MutedRenderingLogger()) { actual.stop { timeout { timeout } } }
    }

    /**
     * Silently removes this [DockerContainer].
     */
    fun remove(actual: DockerContainer = container) {
        with(MutedRenderingLogger()) { runCatching { actual.remove {} } }
    }

    override fun toString(): String = container.toString()
}

val FixedWidthRenderingLogger.use: DockerTestContainerProvider.(FixedWidthRenderingLogger.(DockerProcess) -> Unit) -> DockerProcess
    get() = { koodies.runWrapping({ start() }, { stop(it.container) }) { this@use.it(it);it } }

val use: DockerTestContainerProvider.((DockerProcess) -> Unit) -> DockerProcess
    get() = { koodies.runWrapping({ start() }, { stop(it.container) }) { it(it);it } }


public object DockerTestUtil {
    /**
     * Creates a docker container by running a very short script
     * and the option not to delete the container afterwards.
     *
     * The container is verified to not run anymore when returned.
     */
    public fun createContainer(uniqueId: UniqueId): DockerContainer {
        val containerName = uniqueId.uniqueId.toBaseName()
        val container = DockerContainer.from(containerName)
        expectThat(container).exists

        val process = DockerRunCommandLine {
            image by DockerTestImageExclusive.DOCKER_TEST_CONTAINER.image
            commandLine { command { "printenv" } }
            options {
                name { containerName }
                autoCleanup by false
            }
        }.execute { null }
        expectThat(process) {
            get { started }.isTrue()
            get { exitValue }.isEqualTo(0)
        }

        expectThat(container.isRunning).isFalse()
        return container
    }
}

public val <T : DockerContainer> Assertion.Builder<T>.exists
    get() = assert("%s exists") {
        when (it.exists) {
            true -> pass()
            else -> fail()
        }
    }
