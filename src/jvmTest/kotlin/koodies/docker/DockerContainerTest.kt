package koodies.docker

import koodies.docker.DockerContainer.State
import koodies.docker.DockerContainer.State.Existent.Exited
import koodies.docker.DockerContainer.State.Existent.Running
import koodies.docker.DockerContainer.State.NotExistent
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.expectLogged
import koodies.test.IdeaWorkaroundTest
import koodies.test.IdeaWorkaroundTestFactory
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.Symbols
import koodies.text.Semantics.Symbols.Negative
import koodies.text.Unicode.NBSP
import koodies.text.endsWithRandomSuffix
import koodies.text.matchesCurlyPattern
import koodies.text.spaced
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.length
import java.nio.file.Path
import kotlin.time.measureTime
import kotlin.time.seconds

@Execution(CONCURRENT)
class DockerContainerTest {

    @Nested
    inner class ConstructorInstantiation {

        @TestFactory
        fun `should throw on illegal name`() = ILLEGAL_NAMES.testEach {
            expectThrowing { DockerContainer(it) }.that { isFailure().isA<IllegalArgumentException>() }
        }

        @TestFactory
        fun `should accept legal name`() = LEGAL_NAMES.testEach {
            expectThrowing { DockerContainer(it) }.that { isSuccess() }
        }
    }

    @Nested
    inner class ToString {

        @ContainersTestFactory @IdeaWorkaroundTestFactory
        fun `should contain actual state`(testContainers: TestContainers) = listOf<Pair<String, (TestContainers) -> DockerContainer>>(
            "not existent" to { it.newNotExistentContainer().apply { state } },
            "▶ running" to { it.newRunningTestContainer() },
            "✔︎ exited" to { it.newExitedTestContainer() },
        ).testEach("{}") { (state, provider) ->
            val container = provider(testContainers)
            expect { container.toString() }.that { matchesCurlyPattern("DockerContainer { name = {}⦀{}state = $state }") }
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `should be equal on same name`() {
            expectThat(DockerContainer("docker-container-1")).isEqualTo(DockerContainer("docker-container-1"))
        }

        @Test
        fun `should be unequal on different name`() {
            expectThat(DockerContainer("docker-container-1")).isNotEqualTo(DockerContainer("docker-container-2"))
        }
    }

    @Nested
    inner class Sanitization {

        @Test
        fun `should append random suffix to container`() {
            val container = DockerContainer("docker-container")
            expectThat(DockerContainer { container.withRandomSuffix }).isA<DockerContainer>().name.endsWithRandomSuffix()
        }

        @Nested
        inner class StringBased {

            @TestFactory
            fun `should sanitize illegal name`() = ILLEGAL_NAMES.testEach {
                expect { DockerContainer.from(it) }.that { name.length.isGreaterThanOrEqualTo(8) }
                expect { DockerContainer { it.sanitized } }.that { name.length.isGreaterThanOrEqualTo(8) }
            }

            @TestFactory
            fun `should sanitize legal name`() = LEGAL_NAMES.testEach {
                expect { DockerContainer.from(it) }.that { name.toStringIsEqualTo(it) }
                expect { DockerContainer { it.sanitized } }.that { name.toStringIsEqualTo(it) }
            }

            @TestFactory
            fun `should not append random suffix by default`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
                expect { DockerContainer.from(it) }.that {
                    name.not { endsWithRandomSuffix() }
                }
            }

            @TestFactory
            fun `should append random suffix if specified`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
                expect { DockerContainer.from(it, randomSuffix = true) }.that { name.endsWithRandomSuffix() }
                expect { DockerContainer { it.withRandomSuffix } }.that { name.endsWithRandomSuffix() }
            }
        }

        @Nested
        inner class PathBased {

            @Test
            fun `should sanitize illegal path`() {
                val path = Path.of("~file202")
                expectThat(DockerContainer.from(path, randomSuffix = false)) { name.toStringIsEqualTo("Xfile202") }
                expectThat(DockerContainer { path.sanitized }) { name.toStringIsEqualTo("Xfile202") }
            }

            @Test
            fun `should sanitize legal path`() {
                val path = Path.of("2020-08-20-raspios-buster-armhf-lite.img")
                expectThat(DockerContainer.from(path, randomSuffix = false)) { name.toStringIsEqualTo("2020-08-20-raspios-buster-armhf-lite.img") }
                expectThat(DockerContainer { path.sanitized }) { name.toStringIsEqualTo("2020-08-20-raspios-buster-armhf-lite.img") }
            }

            @Test
            fun `should only use filename`() {
                val path = Path.of("dir/2020-08-20-raspios-buster-armhf-lite.img")
                expectThat(DockerContainer.from(path)) { name.not { contains("dir") } }
                expectThat(DockerContainer { path.withRandomSuffix }) { name.not { contains("dir") } }
            }

            @Test
            fun `should append random suffix by default`() {
                expectThat(DockerContainer.from(Path.of("2020-08-20-raspios-buster-armhf-lite.img"))) { name.endsWithRandomSuffix() }
                expectThat(DockerContainer { Path.of("2020-08-20-raspios-buster-armhf-lite.img").withRandomSuffix }) { name.endsWithRandomSuffix() }
            }

            @TestFactory
            fun `should not append random suffix if specified`() {
                expectThat(DockerContainer.from(Path.of("2020-08-20-raspios-buster-armhf-lite.img"), randomSuffix = false)) {
                    name.not { endsWithRandomSuffix() }
                }
            }
        }

        @Test
        fun `should fill to short name`() {
            expectThat(DockerContainer.from("abc")).name.length.isEqualTo(8)
            expectThat(DockerContainer { "abc".sanitized }).name.length.isEqualTo(8)
        }

        @Test
        fun `should truncate to long name`() {
            expectThat(DockerContainer.from("X".repeat(130))).name.toStringIsEqualTo("X".repeat(128))
            expectThat(DockerContainer { "X".repeat(130).sanitized }).name.toStringIsEqualTo("X".repeat(128))
        }
    }

    companion object {
        val ILLEGAL_NAMES = listOf("", "---", ".container")
        val LEGAL_NAMES = listOf("dockerDocker", "docker-container", "container.1234")
    }

    @Nested
    inner class DockerCommands {

        @Nested
        inner class GetStatus {

            @ContainersTest @IdeaWorkaroundTest
            fun `should get status of non-existent`(testContainers: TestContainers) {
                val container = testContainers.newNotExistentContainer()
                expectThat(container).hasState<NotExistent>()
                BACKGROUND.expectLogged.contains("Checking status of ${container.name}")
            }

            @ContainersTest @IdeaWorkaroundTest
            fun `should get status`(testContainers: TestContainers) {
                val container = testContainers.newRunningTestContainer()
                expectThat(container).hasState<Running> { get { status }.isNotEmpty() }
                BACKGROUND.expectLogged.contains("Checking status of ${container.name}")
            }
        }

        @Nested
        inner class ListContainers {

            @ContainersTest @IdeaWorkaroundTest
            fun `should list containers and log`(testContainers: TestContainers) {
                val containers = (1..3).map { testContainers.newRunningTestContainer() }
                expectThat(DockerContainer.list()).contains(containers)
                BACKGROUND.expectLogged.contains("Listing all containers")
            }
        }

        @Nested
        inner class Start {

            @Nested
            inner class NotExisting {

                @ContainersTest @IdeaWorkaroundTest
                fun `should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { start(attach = false) }.isFailed()
                    BACKGROUND.expectLogged.contains("Starting ${container.name} $Negative no such container".ansiRemoved)
                }
            }

            @Nested
            inner class Stopped {

                @ContainersTest @IdeaWorkaroundTest
                fun `should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { start(attach = false) }.isSuccessful()
                    BACKGROUND.expectLogged.contains("Starting ${container.name}")
                    expectThat(container.isRunning).isTrue()
                }

                @ContainersTest @IdeaWorkaroundTest
                fun `should start attached by default`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer(5.seconds)
                    val passed = measureTime { expectThat(container).get { start(attach = true) }.isSuccessful() }
                    BACKGROUND.expectLogged.contains("Starting ${container.name}")
                    expectThat(passed).isGreaterThanOrEqualTo(5.seconds)
                    expectThat(container.isRunning).isFalse()
                }

                @ContainersTest @IdeaWorkaroundTest
                fun `should start multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newExitedTestContainer())
                    expectThat(DockerContainer.start(*containers.toTypedArray(), attach = false)).isFailed()
                    BACKGROUND.expectLogged.contains("Starting ${containers[0].name}$NBSP${FieldDelimiters.FIELD.ansiRemoved}$NBSP${containers[1].name} ${Negative.ansiRemoved} no such container")
                    expectThat(containers).all { not { hasState<Exited>() } }
                }
            }


            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun `should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.isRunning).isTrue()
                    expectThat(container).get { start(attach = false) }.isSuccessful()
                    BACKGROUND.expectLogged.contains("Starting ${container.name}")
                    expectThat(container.isRunning).isTrue()
                    expectThat(container).hasState<State.Existent.Running>()
                }
            }
        }

        @Nested
        inner class Stop {

            @Nested
            inner class NotExisting {

                @ContainersTest @IdeaWorkaroundTest
                fun `should stop container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { stop() }.isFailed()
                    BACKGROUND.expectLogged.contains("Stopping ${container.name} ${Negative.ansiRemoved} no such container")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class NotRunning {

                @ContainersTest @IdeaWorkaroundTest
                fun `should stop container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { stop() }.isSuccessful()
                    BACKGROUND.expectLogged.contains("Stopping ${container.name}")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun `should stop container - but not remove it - and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.isRunning).isTrue()
                    expectThat(container).get { stop() }.isSuccessful()
                    BACKGROUND.expectLogged.contains("Stopping ${container.name}")
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).hasState<Exited>()
                }

                @ContainersTest @IdeaWorkaroundTest
                fun `should stop multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                    expectThat(DockerContainer.stop(*containers.toTypedArray())).isFailed()
                    BACKGROUND.expectLogged.contains("Stopping ${containers[0].name}$NBSP${FieldDelimiters.FIELD.ansiRemoved}$NBSP${containers[1].name} ${Negative.ansiRemoved} no such container")
                    expectThat(containers).all { not { hasState<State.Existent.Running>() } }
                }
            }
        }

        @Nested
        inner class Kill {

            @Nested
            inner class NotExisting {

                @ContainersTest @IdeaWorkaroundTest
                fun `should kill container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { kill() }.isFailed()
                    BACKGROUND.expectLogged.contains("Killing ${container.name} ${Negative.ansiRemoved} no such container")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class NotRunning {

                @ContainersTest @IdeaWorkaroundTest
                fun `should kill container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { kill() }.isFailed()
                    BACKGROUND.expectLogged.contains("Killing ${container.name}")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun `should kill container - but not remove it - and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.isRunning).isTrue()
                    expectThat(container).get { kill() }.isSuccessful()
                    BACKGROUND.expectLogged.contains("Killing ${container.name}")
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).hasState<Exited> { exitCode.isNotEqualTo(0) }
                }

                @ContainersTest @IdeaWorkaroundTest
                fun `should kill multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                    expectThat(DockerContainer.kill(*containers.toTypedArray())).isFailed()
                    BACKGROUND.expectLogged.contains("Killing ${containers[0].name}$NBSP${FieldDelimiters.FIELD.ansiRemoved}$NBSP${containers[1].name} ${Negative.ansiRemoved} no such container")
                    expectThat(containers).all { not { hasState<State.Existent.Running>() } }
                }
            }
        }

        @Nested
        inner class Remove {

            @Nested
            inner class NotExistent {

                @ContainersTest @IdeaWorkaroundTest
                fun `should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.remove()).isFailed()
                    BACKGROUND.expectLogged.contains("Removing ${container.name} ${Negative.ansiRemoved} no such container")
                }
            }

            @Nested
            inner class Existent {

                @ContainersTest @IdeaWorkaroundTest
                fun `should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.remove()).isSuccessful()
                    BACKGROUND.expectLogged.contains("Removing ${container.name}")
                }
            }

            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun `should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.remove()).isFailed()
                    BACKGROUND.expectLogged.contains("Removing ${container.name}")
                    expectThat(container.isRunning).isTrue()
                    expectThat(container.exists).isTrue()
                }
            }
        }

        @Nested
        inner class RemoveForcefully {

            @ContainersTest @IdeaWorkaroundTest
            fun `should remove forcibly container and log`(testContainers: TestContainers) {
                val container = testContainers.newRunningTestContainer()
                expectThat(container.remove(force = true)).isSuccessful()
                BACKGROUND.expectLogged.contains("Removing forcefully ${container.name}")
                expectThat(container.isRunning).isFalse()
                expectThat(container.exists).isFalse()
            }

            @ContainersTest @IdeaWorkaroundTest
            fun `should remove multiple and log`(testContainers: TestContainers) {
                val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                expectThat(DockerContainer.remove(*containers.toTypedArray(), force = true)).isSuccessful()
                BACKGROUND.expectLogged.contains("Removing forcefully ${containers[0].name}${FieldDelimiters.FIELD.spaced}${containers[1].name} ${Symbols.OK}".ansiRemoved)
                expectThat(containers).all { hasState<NotExistent>() }
            }
        }
    }
}

public inline fun <reified T : State> Builder<DockerContainer>.hasState(): Builder<DockerContainer> =
    compose("status") {
        get { state }.isA<T>()
    }.then { if (allPassed) pass() else fail() }

public inline fun <reified T : State> Builder<DockerContainer>.hasState(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<DockerContainer> =
    compose("status") {
        get { state }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }

public val Builder<DockerContainer>.name get(): Builder<String> = get("name") { name }

public val Builder<Exited>.exitCode get(): Builder<Int?> = get("exit code") { exitCode }
