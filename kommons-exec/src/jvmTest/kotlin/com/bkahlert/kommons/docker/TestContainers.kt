package com.bkahlert.kommons.docker

import com.bkahlert.kommons.collections.synchronizedListOf
import com.bkahlert.kommons.collections.synchronizedMapOf
import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Exited
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Running
import com.bkahlert.kommons.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons.docker.TestImages.HelloWorld
import com.bkahlert.kommons.docker.TestImages.Ubuntu
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.RendererProviders.noDetails
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.junit.SimpleIdResolver.Companion.simpleId
import com.bkahlert.kommons.test.junit.getTestStore
import com.bkahlert.kommons.test.junit.getTyped
import com.bkahlert.kommons.test.withAnnotation
import com.bkahlert.kommons.time.poll
import com.bkahlert.kommons.tracing.rendering.BackgroundPrinter
import com.bkahlert.kommons.tracing.rendering.ReturnValues
import com.bkahlert.kommons.tracing.rendering.Styles.None
import com.bkahlert.kommons.tracing.runSpanning
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.ResourceLock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.ceil
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Images used for the purpose of testing.
 */
object TestImages {

    /**
     * [Ubuntu](https://hub.docker.com/_/ubuntu) based [TestContainersProvider]
     */
    object Ubuntu : DockerImage("ubuntu"), TestContainersProvider {
        override val image: DockerImage get() = this
        private val testContainersProvider: TestContainersProvider by lazy { TestContainersProvider.of(this) }

        override fun testContainersFor(simpleId: SimpleId): TestContainers =
            testContainersProvider.testContainersFor(simpleId)

        override fun release(simpleId: SimpleId) =
            testContainersProvider.release(simpleId)
    }

    /**
     * [busybox](https://hub.docker.com/_/busybox) based [TestContainersProvider]
     */
    object BusyBox : DockerImage("busybox"), TestContainersProvider {
        override val image: DockerImage get() = this
        private val testContainersProvider: TestContainersProvider by lazy { TestContainersProvider.of(this) }

        override fun testContainersFor(simpleId: SimpleId): TestContainers =
            testContainersFor(simpleId)

        override fun release(simpleId: SimpleId) =
            testContainersProvider.release(simpleId)
    }

    /**
     * [Hello World!](https://hub.docker.com/_/hello-world) based [TestImageProvider]
     */
    object HelloWorld : DockerImage("hello-world"), TestImageProvider {
        override val image: DockerImage get() = this
        override val lock: ReentrantLock by lazy { ReentrantLock() }
    }
}

/**
 * A [Test] that is provided with an instance of [TestContainers].
 *
 * @see TestContainers
 * @see TestContainersProvider
 */
@Slow
@DockerRequiring @Test
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Extensions(
    ExtendWith(DockerRunningCondition::class),
    ExtendWith(ContainersTestExtension::class)
)
annotation class ContainersTest(
    val provider: KClass<out TestContainersProvider> = Ubuntu::class,
)

/**
 * [ParameterResolver] that provides an instance of [TestContainers] which can be used
 * to create containers with a specific state.
 */
class ContainersTestExtension : TypeBasedParameterResolver<TestContainers>(), AfterEachCallback {
    private val ExtensionContext.store: Store get() = getTestStore<ContainersTestExtension>()

    private val ExtensionContext.provider: TestContainersProvider
        get() = withAnnotation<ContainersTest, KClass<out TestContainersProvider>> { provider }
            ?.objectInstance
            ?: error("Currently only ${TestContainersProvider::class.simpleName} singletons are supported, that is, implemented as an object.")

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): TestContainers =
        context.provider.testContainersFor(context.simpleId).also { context.store.put(TestContainers::class, it) }

    override fun afterEach(context: ExtensionContext) = context.store.getTyped<TestContainers>(TestContainers::class)?.release() ?: Unit
}

/**
 * Provider of [TestContainers] to facilitate testing.
 *
 * @see ContainersTest
 */
interface TestContainersProvider {

    val image: DockerImage

    /**
     * Provides a new [TestContainers] instance for the given [simpleId].
     *
     * If one already exists, an exception is thrown.
     */
    fun testContainersFor(simpleId: SimpleId): TestContainers

    /**
     * Kills and removes all provisioned test containers for the [simpleId]
     */
    fun release(simpleId: SimpleId)

    companion object {
        fun of(image: DockerImage) = object : TestContainersProvider {
            override val image: DockerImage = image
            private val sessions = synchronizedMapOf<SimpleId, TestContainers>()

            override fun testContainersFor(simpleId: SimpleId) =
                TestContainers(image, simpleId)
                    .also {
                        check(!sessions.containsKey(simpleId)) { "A session for $simpleId is already provided!" }
                        sessions[simpleId] = it
                    }

            override fun release(simpleId: SimpleId) {
                sessions.remove(simpleId)?.apply { release() }
            }
        }
    }
}

/**
 * Provider of [DockerContainer] instances that
 * are in a specific state to facilitate testing.
 */
class TestContainers(
    private val image: DockerImage,
    private val simpleId: SimpleId,
) {
    private val provisioned: MutableList<DockerContainer> = synchronizedListOf()

    /**
     * Kills and removes all provisioned test containers.
     */
    fun release() {
        val toBeReleased = provisioned.toList().also { provisioned.clear() }
        runSpanning("Releasing ${toBeReleased.size} container(s)", style = None, printer = BackgroundPrinter) {
            ReturnValues(toBeReleased.map { container -> kotlin.runCatching { container.remove(force = true) }.fold({ it }, { it }) })
        }
    }

    private fun startContainerWithCommandLine(
        commandLine: CommandLine,
    ): DockerContainer {
        val container = DockerContainer.from(name = "$simpleId", randomSuffix = true).also { provisioned.add(it) }
        commandLine.dockerized(
            this@TestContainers.image, Options(
                name = container,
                autoCleanup = false,
                detached = true,
            )
        ).exec.logging(renderer = noDetails())
        return container
    }


    private fun Duration.toIntegerSeconds(): Int = ceil(toDouble(DurationUnit.SECONDS)).toInt()

    /**
     * Returns a new container that will run for as long as specified by [duration].
     */
    private fun newRunningContainer(
        duration: Duration,
    ): DockerContainer =
        startContainerWithCommandLine(CommandLine("sleep", duration.toIntegerSeconds().toString(), name = "${duration.toIntegerSeconds()} sleep"))

    /**
     * Returns a container that does not exist on this system.
     */
    internal fun newNotExistentContainer(): DockerContainer =
        runSpanning("Providing non-existent container", style = None, printer = BackgroundPrinter) {
            DockerContainer.from(randomString())
        }

    /**
     * Returns a new container that already terminated with exit code `0`.
     *
     * The next time this container is started it will run for the specified [duration] (default: 30 seconds).
     */
    internal fun newExitedTestContainer(duration: Duration = 30.seconds): DockerContainer =
        runSpanning("Providing exited container", style = None, printer = BackgroundPrinter) {
            startContainerWithCommandLine(
                CommandLine(
                    "sh", "-c", """
                if [ -f "booted-before" ]; then
                  sleep ${duration.toIntegerSeconds()}
                else
                  touch "booted-before"
                fi
                exit 0
            """.trimIndent(), name = "$duration sleep"
                )
            ).also { container ->
                poll {
                    container.containerState is Exited
                }.every(0.5.seconds).forAtMost(5.seconds) { timeout ->
                    fail { "Could not provide exited test container $container within $timeout." }
                }
            }
        }

    /**
     * Returns a container that is running for the specified [duration] (default: 30 seconds).
     */
    internal fun newRunningTestContainer(duration: Duration = 30.seconds): DockerContainer =
        runSpanning("Providing running container", style = None, printer = BackgroundPrinter) {
            newRunningContainer(duration).also { container ->
                poll {
                    container.containerState is Running
                }.every(0.5.seconds).forAtMost(5.seconds) { duration ->
                    fail { "Could not provide stopped test container $container within $duration." }
                }
            }
        }

    override fun toString(): String = asString(::provisioned)
}

/**
 * A [TestFactory] that is provided with a [TestImage].
 *
 * @see TestImage
 * @see TestImageProvider
 */
@Slow
@DockerRequiring @TestFactory
@ResourceLock(TestImageProvider.RESOURCE)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ExtendWith(ImageTestExtension::class)
annotation class ImageTestFactory(
    val provider: KClass<out TestImageProvider> = HelloWorld::class,
    val logging: Boolean = false,
)

/**
 * A [Test] that is provided with an instance of [TestImage].
 *
 * @see TestImage
 * @see TestImageProvider
 */
@Slow
@DockerRequiring @Test
@ResourceLock(TestImageProvider.RESOURCE)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ExtendWith(ImageTestExtension::class)
annotation class ImageTest(
    val provider: KClass<out TestImageProvider> = HelloWorld::class,
    val logging: Boolean = false,
)

class ImageTestExtension : TypeBasedParameterResolver<TestImage>() {

    private val ExtensionContext.provider: TestImageProvider
        get() = (withAnnotation<ImageTestFactory, KClass<out TestImageProvider>> { provider }
            ?: withAnnotation<ImageTest, KClass<out TestImageProvider>> { provider })
            ?.objectInstance
            ?: error("Currently only ${TestImageProvider::class.simpleName} singletons are supported, that is, implemented as an object.")

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): TestImage =
        context.provider.testImageFor()
}

/**
 * Helper to facilitate tests with requirements to the
 * state of residing on the local system or not.
 */
interface TestImageProvider {

    val lock: ReentrantLock
    val image: DockerImage

    /**
     * Provides a new [TestImage].
     */
    fun testImageFor(): TestImage =
        TestImage(lock, image)

    companion object {
        const val RESOURCE: String = "kommons.docker.test-image"
    }
}

/**
 * A docker image that can run code while being guaranteed to be
 * in a specific state.
 */
class TestImage(
    private val lock: ReentrantLock,
    image: DockerImage,
) : DockerImage(
    image.repository,
    image.path,
    image.tag,
    image.digest
) {

    private fun <R> runWithLock(pulled: Boolean, block: (DockerImage) -> R): R = lock.withLock {
        if (pulled && !isPulled) pull()
        else if (!pulled && isPulled) remove(force = true)
        poll { isPulled == pulled }.every(0.5.seconds).forAtMost(5.seconds) {
            "Failed to " + (if (pulled) "pull" else "remove") + " $this"
        }
        runCatching(block)
    }.getOrThrow()

    /**
     * Runs the given [block] exclusively while the managed [DockerImage]
     * resides on this system.
     */
    fun whilePulled(block: (DockerImage) -> Unit): Unit =
        runWithLock(true, block)

    /**
     * Runs the given [block] exclusively while the managed [DockerImage]
     * is removed from this system.
     */
    fun whileRemoved(block: (DockerImage) -> Unit): Unit =
        runWithLock(false, block)
}
