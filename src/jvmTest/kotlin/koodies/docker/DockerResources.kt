package koodies.docker

import koodies.collections.synchronizedListOf
import koodies.collections.synchronizedMapOf
import koodies.concurrent.process.CommandLine
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.MutedRenderingLogger
import koodies.logging.ReturnValues
import koodies.test.UniqueId
import koodies.text.randomString
import koodies.time.poll
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.parallel.Resources
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.milliseconds
import kotlin.time.seconds

/**
 * Common resource names for synchronizing [Docker] test execution.
 *
 * @see ResourceLock
 * @see Resources
 */
internal object DockerResources {
    /**
     * Represents the device `/dev/serial1`.
     */
    internal const val SERIAL = "/dev/serial1"

    /**
     * Images used for the purpose of testing.
     */
    internal sealed class TestImage(officialName: String) : DockerImage(officialName, emptyList(), null, null) {
        internal companion object {
            internal const val RESOURCE: String = "koodies.docker.test-image"
        }

        /**
         * Used for script-based tests; will not get removed.
         */
        internal object Ubuntu : TestImage("ubuntu"), TestContainersProvider {
            internal const val RESOURCE: String = TestImage.RESOURCE + ".ubuntu"

            override val image: DockerImage get() = this
            private val testContainersProvider: TestContainersProvider by lazy { TestContainersProvider.of(this) }

            override fun testContainersFor(uniqueId: UniqueId, logging: Boolean): TestContainers =
                testContainersProvider.testContainersFor(uniqueId, logging)

            override fun release(uniqueId: UniqueId) =
                testContainersProvider.release(uniqueId)
        }

        /**
         * Used for script-based tests; will not get removed.
         */
        internal object BusyBox : TestImage("busybox"), TestContainersProvider {
            internal const val RESOURCE: String = TestImage.RESOURCE + ".busybox"

            override val image: DockerImage get() = this
            private val testContainersProvider: TestContainersProvider by lazy { TestContainersProvider.of(this) }

            override fun testContainersFor(uniqueId: UniqueId, logging: Boolean): TestContainers =
                testContainersFor(uniqueId, logging)

            override fun release(uniqueId: UniqueId) =
                testContainersProvider.release(uniqueId)
        }

        /**
         * Used for script-based tests; will not get removed.
         */
        internal object HelloWorld : TestImage("hello-world"), TestImageProvider {
            internal const val RESOURCE: String = TestImage.RESOURCE + ".hello-world"

            private val imageLock = ReentrantLock()
            private fun <R> runWithLock(logging: Boolean, pulled: Boolean, block: (DockerImage) -> R): R = imageLock.withLock {
                with(if (logging) BACKGROUND else MutedRenderingLogger()) {
                    if (pulled && !isPulled) pull()
                    else if (!pulled && isPulled) remove(force = true)
                    poll { isPulled == pulled }.every(500.milliseconds).forAtMost(5.seconds) {
                        "Failed to " + (if (pulled) "pull" else "remove") + " $this"
                    }
                    runCatching(block)
                }
            }.getOrThrow()

            override fun <R> usingPulledImage(logging: Boolean, block: (DockerImage) -> R): R =
                runWithLock(logging, true, block)

            override fun <R> usingRemovedImage(logging: Boolean, block: (DockerImage) -> R): R =
                runWithLock(logging, false, block)
        }
    }
}

/**
 * Helper to facilitate tests with requirements to the
 * state of residing on the local system or not.
 */
internal interface TestImageProvider {
    /**
     * Runs the given [block] exclusively while the managed [DockerImage]
     * resides on this system.
     */
    fun <R> usingPulledImage(logging: Boolean = false, block: (DockerImage) -> R): R

    /**
     * Runs the given [block] exclusively while the managed [DockerImage]
     * is removed from this system.
     */
    fun <R> usingRemovedImage(logging: Boolean = false, block: (DockerImage) -> R): R
}

/**
 * Provider of [DockerContainer] instances to facilitate
 * testing.
 */
internal interface TestContainersProvider {

    val image: DockerImage

    /**
     * Provides a new [TestContainers] instance for the given [uniqueId].
     *
     * If one already exists, an exception is thrown.
     */
    fun testContainersFor(uniqueId: UniqueId, logging: Boolean = false): TestContainers

    /**
     * Kills and removes all provisioned test containers for the [uniqueId]
     */
    fun release(uniqueId: UniqueId): Unit

    companion object {
        fun of(image: DockerImage) = object : TestContainersProvider {
            override val image: DockerImage = image
            private val sessions = synchronizedMapOf<UniqueId, TestContainers>()

            override fun testContainersFor(uniqueId: UniqueId, logging: Boolean) =
                TestContainers(if (logging) BACKGROUND else MutedRenderingLogger(), image, uniqueId)
                    .also {
                        check(!sessions.containsKey(uniqueId)) { "A session for $uniqueId is already provided!" }
                        sessions[uniqueId] = it
                    }

            override fun release(uniqueId: UniqueId): Unit {
                sessions.remove(uniqueId)?.apply { release() }
            }
        }
    }
}

/**
 * Provider of [DockerContainer] instances to facilitate
 * testing.
 */
internal class TestContainers(
    private val logger: FixedWidthRenderingLogger,
    private val image: DockerImage,
    private val uniqueId: UniqueId,
) {
    private val provisioned: MutableList<DockerContainer> = synchronizedListOf()

    /**
     * Kills and removes all provisioned test containers.
     */
    internal fun release(): Unit {
        val copy = provisioned.toList().also { provisioned.clear() }
        logger.compactLogging("Releasing ${provisioned.size} container(s)") {
            copy.map { kotlin.runCatching { it.remove(force = true) }.fold({ it }, { it }) }
                .let { ReturnValues(it) }
        }
    }

    private fun startContainerWithCommandLine(
        autoCleanup: Boolean = true,
        commandLine: CommandLine,
    ): DockerContainer {
        val container = DockerContainer.from(name = uniqueId.simplified, randomSuffix = true).also { provisioned.add(it) }
        with(logger) {
            commandLine.executeDockerized(this@TestContainers.image) {
                dockerOptions {
                    name by container.name
                    this.autoCleanup by autoCleanup
                    detached { on }
                }
                executionOptions {
                    noDetails("running ${commandLine.summary}")
                }
                null
            }
        }
        return container
    }


    private fun Duration.toIntegerSeconds(): Int = ceil(inSeconds).toInt()

    /**
     * Returns a new container that will run for as long as specified by [duration].
     */
    private fun newRunningContainer(
        duration: Duration,
        autoCleanup: Boolean = true,
    ): DockerContainer =
        startContainerWithCommandLine(autoCleanup, CommandLine("sleep", duration.toIntegerSeconds().toString()))

    /**
     * Returns a container that does not exist on this system.
     */
    internal fun newNotExistentContainer() = DockerContainer.from(randomString())

    /**
     * Returns a new container that already exited with exit code `0`.
     *
     * The next time this container is started it will run for the specified [duration] (default: 30 seconds).
     */
    internal fun newExitedTestContainer(duration: Duration = 30.seconds): DockerContainer =
        startContainerWithCommandLine(autoCleanup = false, CommandLine("sh", "-c", """
                if [ -f "booted-before" ]; then
                  sleep ${duration.toIntegerSeconds()}
                else
                  touch "booted-before"
                fi
                exit 0
            """.trimIndent())).also {
            poll { it.isExited }.every(500.milliseconds).forAtMost(5.seconds) { timeout ->
                fail { "Could not provide exited test container $it within $timeout." }
            }
        }

    /**
     * Returns a container that is running for the specified [duration] (default: 30 seconds).
     */
    internal fun newRunningTestContainer(duration: Duration = 30.seconds): DockerContainer =
        newRunningContainer(duration, false).also {
            poll { it.isRunning }.every(500.milliseconds).forAtMost(5.seconds) { duration ->
                fail { "Could not provide stopped test container $it within $duration." }
            }
        }

    override fun toString(): String = this.image.toString()
}
