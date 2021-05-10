package koodies.docker

import koodies.asString
import koodies.collections.synchronizedListOf
import koodies.collections.synchronizedMapOf
import koodies.docker.DockerContainer.State.Existent.Exited
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.docker.TestImages.HelloWorld
import koodies.docker.TestImages.Ubuntu
import koodies.exec.CommandLine
import koodies.logging.FixedWidthRenderingLogger
import koodies.logging.FixedWidthRenderingLogger.Border.DOTTED
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLogger
import koodies.logging.ReturnValues
import koodies.logging.conditionallyVerboseLogger
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.UniqueId.Companion.id
import koodies.test.store
import koodies.test.withAnnotation
import koodies.text.randomString
import koodies.time.poll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
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
import kotlin.time.DurationUnit

/**
 * Images used for the purpose of testing.
 */
object TestImages {

    /**
     * [Ubuntu](https://hub.docker.com/_/ubuntu) based [TestContainersProvider]
     */
    object Ubuntu : DockerImage("ubuntu", emptyList()), TestContainersProvider {
        override val image: DockerImage get() = this
        private val testContainersProvider: TestContainersProvider by lazy { TestContainersProvider.of(this) }

        override fun testContainersFor(uniqueId: UniqueId, logger: FixedWidthRenderingLogger): TestContainers =
            testContainersProvider.testContainersFor(uniqueId, logger)

        override fun release(uniqueId: UniqueId) =
            testContainersProvider.release(uniqueId)
    }

    /**
     * [busybox](https://hub.docker.com/_/busybox) based [TestContainersProvider]
     */
    object BusyBox : DockerImage("busybox", emptyList()), TestContainersProvider {
        override val image: DockerImage get() = this
        private val testContainersProvider: TestContainersProvider by lazy { TestContainersProvider.of(this) }

        override fun testContainersFor(uniqueId: UniqueId, logger: FixedWidthRenderingLogger): TestContainers =
            testContainersFor(uniqueId, logger)

        override fun release(uniqueId: UniqueId) =
            testContainersProvider.release(uniqueId)
    }

    /**
     * [Hello World!](https://hub.docker.com/_/hello-world) based [TestImageProvider]
     */
    object HelloWorld : DockerImage("hello-world", emptyList()), TestImageProvider {
        override val image: DockerImage get() = this
        override val lock: ReentrantLock by lazy { ReentrantLock() }
    }
}

/**
 * A [TestFactory] that is provided with an instance of [TestContainers].
 *
 * @see TestContainers
 * @see TestContainersProvider
 */
@Slow
@DockerRequiring @TestFactory
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Extensions(
    ExtendWith(DockerRunningCondition::class),
    ExtendWith(ContainersTestExtension::class)
)
annotation class ContainersTestFactory(
    val provider: KClass<out TestContainersProvider> = Ubuntu::class,
    val logging: Boolean = false,
)

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
    val logging: Boolean = false,
)

/**
 * [ParameterResolver] that provides an instance of [TestContainers] which can be used
 * to create containers with a specific state.
 */
class ContainersTestExtension : TypeBasedParameterResolver<TestContainers>(), AfterEachCallback {

    private val ExtensionContext.provider: TestContainersProvider
        get() = (withAnnotation<ContainersTestFactory, KClass<out TestContainersProvider>> { provider }
            ?: withAnnotation<ContainersTest, KClass<out TestContainersProvider>> { provider })
            ?.objectInstance
            ?: error("Currently only ${TestContainersProvider::class.simpleName} singletons are supported, that is, implemented as an object.")

    private val ExtensionContext.logger: FixedWidthRenderingLogger
        get() = conditionallyVerboseLogger(withAnnotation<ContainersTestFactory, Boolean> { logging } ?: withAnnotation<ContainersTest, Boolean> { logging })

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): TestContainers =
        context.provider.testContainersFor(context.id, context.logger).also { context.save(it) }

    override fun afterEach(context: ExtensionContext) = context.load()?.release() ?: Unit

    private fun ExtensionContext.load(): TestContainers? = store<ContainersTestExtension>().get(element, TestContainers::class.java)
    private fun ExtensionContext.save(testContainers: TestContainers): Unit = store<ContainersTestExtension>().put(element, testContainers)
}

/**
 * Provider of [TestContainers] to facilitate testing.
 *
 * @see ContainersTest
 */
interface TestContainersProvider {

    val image: DockerImage

    /**
     * Provides a new [TestContainers] instance for the given [uniqueId].
     *
     * If one already exists, an exception is thrown.
     */
    fun testContainersFor(uniqueId: UniqueId, logger: FixedWidthRenderingLogger = BACKGROUND): TestContainers

    /**
     * Kills and removes all provisioned test containers for the [uniqueId]
     */
    fun release(uniqueId: UniqueId)

    companion object {
        fun of(image: DockerImage) = object : TestContainersProvider {
            override val image: DockerImage = image
            private val sessions = synchronizedMapOf<UniqueId, TestContainers>()

            override fun testContainersFor(uniqueId: UniqueId, logger: FixedWidthRenderingLogger) =
                TestContainers(logger, image, uniqueId)
                    .also {
                        check(!sessions.containsKey(uniqueId)) { "A session for $uniqueId is already provided!" }
                        sessions[uniqueId] = it
                    }

            override fun release(uniqueId: UniqueId) {
                sessions.remove(uniqueId)?.apply { release() }
            }
        }
    }
}

/**
 * Provider of [DockerContainer] instances that
 * are in a specific state to facilitate testing.
 */
class TestContainers(
    private val logger: FixedWidthRenderingLogger,
    private val image: DockerImage,
    private val uniqueId: UniqueId,
) {
    private val provisioned: MutableList<DockerContainer> = synchronizedListOf()

    /**
     * Kills and removes all provisioned test containers.
     */
    fun release() {
        val copy = provisioned.toList().also { provisioned.clear() }
        logger.logging("Releasing ${copy.size} container(s)", border = DOTTED) {
            ReturnValues(copy.map { kotlin.runCatching { it.remove(force = true, logger = this) }.fold({ it }, { it }) })
        }
    }

    private fun startContainerWithCommandLine(
        commandLine: CommandLine,
    ): DockerContainer {
        val container = DockerContainer.from(name = "$uniqueId", randomSuffix = true).also { provisioned.add(it) }
        commandLine.dockerized(this@TestContainers.image) {
            name by container.name
            this.autoCleanup by false
            detached { on }
        }.exec.logging(logger) { noDetails("running ${commandLine.summary}") }
        return container
    }


    private fun Duration.toIntegerSeconds(): Int = ceil(toDouble(DurationUnit.SECONDS)).toInt()

    /**
     * Returns a new container that will run for as long as specified by [duration].
     */
    private fun newRunningContainer(
        duration: Duration,
    ): DockerContainer =
        startContainerWithCommandLine(CommandLine("sleep", duration.toIntegerSeconds().toString()))

    /**
     * Returns a container that does not exist on this system.
     */
    internal fun newNotExistentContainer() = DockerContainer.from(randomString())

    /**
     * Returns a new container that already terminated with exit code `0`.
     *
     * The next time this container is started it will run for the specified [duration] (default: 30 seconds).
     */
    internal fun newExitedTestContainer(duration: Duration = Duration.seconds(30)): DockerContainer =
        startContainerWithCommandLine(CommandLine("sh", "-c", """
                if [ -f "booted-before" ]; then
                  sleep ${duration.toIntegerSeconds()}
                else
                  touch "booted-before"
                fi
                exit 0
            """.trimIndent())).also { container ->
            poll {
                with(container) { with(logger) { state } } is Exited
            }.every(Duration.milliseconds(500)).forAtMost(Duration.seconds(5)) { timeout ->
                fail { "Could not provide exited test container $container within $timeout." }
            }
        }

    /**
     * Returns a container that is running for the specified [duration] (default: 30 seconds).
     */
    internal fun newRunningTestContainer(duration: Duration = Duration.seconds(30)): DockerContainer =
        newRunningContainer(duration).also { container ->
            poll {
                with(container) { with(logger) { state } } is Running
            }.every(Duration.milliseconds(500)).forAtMost(Duration.seconds(5)) { duration ->
                fail { "Could not provide stopped test container $container within $duration." }
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

    private val ExtensionContext.logger: FixedWidthRenderingLogger
        get() = conditionallyVerboseLogger(withAnnotation<ImageTestFactory, Boolean> { logging } ?: withAnnotation<ImageTest, Boolean> { logging })

    override fun resolveParameter(parameterContext: ParameterContext, context: ExtensionContext): TestImage =
        context.provider.testImageFor(context.logger)
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
    fun testImageFor(logger: FixedWidthRenderingLogger = BACKGROUND): TestImage =
        TestImage(lock, image, logger)

    companion object {
        const val RESOURCE: String = "koodies.docker.test-image"
    }
}

/**
 * A docker image that can run code while being guaranteed to be
 * in a specific state.
 */
class TestImage(
    private val lock: ReentrantLock,
    image: DockerImage,
    private val logger: RenderingLogger,
) : DockerImage(
    image.repository,
    image.path,
    image.tag,
    image.digest
) {

    private fun <R> runWithLock(pulled: Boolean, block: (DockerImage) -> R): R = lock.withLock {
        with(logger) {
            if (pulled && !isPulled) pull(logger = logger)
            else if (!pulled && isPulled) remove(force = true, logger = logger)
            poll { isPulled == pulled }.every(Duration.milliseconds(500)).forAtMost(Duration.seconds(5)) {
                "Failed to " + (if (pulled) "pull" else "remove") + " $this"
            }
            runCatching(block)
        }
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
