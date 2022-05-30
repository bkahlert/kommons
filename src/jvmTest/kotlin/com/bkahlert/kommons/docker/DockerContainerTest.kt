package com.bkahlert.kommons.docker

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.docker.DockerContainer.State
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Exited
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Running
import com.bkahlert.kommons.docker.DockerContainer.State.NotExistent
import com.bkahlert.kommons.test.IdeaWorkaroundTest
import com.bkahlert.kommons.test.IdeaWorkaroundTestFactory
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.text.Semantics.FieldDelimiters.FIELD
import com.bkahlert.kommons.text.Semantics.Symbols.Negative
import com.bkahlert.kommons.text.Semantics.Symbols.OK
import com.bkahlert.kommons.text.endsWithRandomSuffix
import com.bkahlert.kommons.text.spaced
import com.bkahlert.kommons.text.toStringMatchesCurlyPattern
import com.bkahlert.kommons.time.seconds
import com.bkahlert.kommons.tracing.TestSpanScope
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
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
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.length
import java.nio.file.Path
import kotlin.time.measureTime

@Tag("xxx")
@Execution(CONCURRENT)
class DockerContainerTest {

    @Nested
    inner class ConstructorInstantiation {

        @TestFactory
        fun `should throw on illegal name`() = ILLEGAL_NAMES.testEach {
            expectThrows<IllegalArgumentException> { DockerContainer(it) }
        }

        @TestFactory
        fun `should accept legal name`() = LEGAL_NAMES.testEach {
            expectCatching { DockerContainer(it) } that { isSuccess() }
        }
    }

    @Nested
    inner class ToString {

        @ContainersTestFactory @IdeaWorkaroundTestFactory
        fun `should contain actual state`(testContainers: TestContainers) = testEach<Pair<String, (TestContainers) -> DockerContainer>>(
            "not existent" to { it.newNotExistentContainer().apply { containerState } },
            "▶ running" to { it.newRunningTestContainer() },
            "✔︎ exited" to { it.newExitedTestContainer() },
        ) { (state, provider) ->
            val container = provider(testContainers)
            expecting { container } that { toStringMatchesCurlyPattern("DockerContainer { name = {}⦀{}state = $state }") }
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
                expecting { DockerContainer.from(it) } that { name.length.isGreaterThanOrEqualTo(1) }
                expecting { DockerContainer { it.sanitized } } that { name.length.isGreaterThanOrEqualTo(1) }
            }

            @TestFactory
            fun `should sanitize legal name`() = LEGAL_NAMES.testEach {
                expecting { DockerContainer.from(it) } that { name.toStringIsEqualTo(it) }
                expecting { DockerContainer { it.sanitized } } that { name.toStringIsEqualTo(it) }
            }

            @TestFactory
            fun `should not append random suffix by default`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
                expecting { DockerContainer.from(it) } that {
                    name.not { endsWithRandomSuffix() }
                }
            }

            @TestFactory
            fun `should append random suffix if specified`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
                expecting { DockerContainer.from(it, randomSuffix = true) } that { name.endsWithRandomSuffix() }
                expecting { DockerContainer { it.withRandomSuffix } } that { name.endsWithRandomSuffix() }
            }
        }

        @Nested
        inner class PathBased {

            @Suppress("SpellCheckingInspection")
            @Test
            fun `should sanitize illegal path`() {
                val path = Path.of("~file202")
                expectThat(DockerContainer.from(path, randomSuffix = false)) { name.toStringIsEqualTo("Xfile202") }
                expectThat(DockerContainer { path.sanitized }) { name.toStringIsEqualTo("Xfile202") }
            }

            @Test
            fun `should sanitize legal path`() {
                val path = Path.of("2020-08-20-img-lite.img")
                expectThat(DockerContainer.from(path, randomSuffix = false)) { name.toStringIsEqualTo("2020-08-20-img-lite.img") }
                expectThat(DockerContainer { path.sanitized }) { name.toStringIsEqualTo("2020-08-20-img-lite.img") }
            }

            @Test
            fun `should only use filename`() {
                val path = Path.of("dir/2020-08-20-img-lite.img")
                expectThat(DockerContainer.from(path)) { name.not { contains("dir") } }
                expectThat(DockerContainer { path.withRandomSuffix }) { name.not { contains("dir") } }
            }

            @Test
            fun `should append random suffix by default`() {
                expectThat(DockerContainer.from(Path.of("2020-08-20-img-lite.img"))) { name.endsWithRandomSuffix() }
                expectThat(DockerContainer { Path.of("2020-08-20-img-lite.img").withRandomSuffix }) { name.endsWithRandomSuffix() }
            }

            @TestFactory
            fun `should not append random suffix if specified`() {
                expectThat(DockerContainer.from(Path.of("2020-08-20-img-lite.img"), randomSuffix = false)) {
                    name.not { endsWithRandomSuffix() }
                }
            }
        }

        @Test
        fun `should fill name if too short`() {
            expectThat(DockerContainer.from("")).name.length.isEqualTo(1)
            expectThat(DockerContainer { "".sanitized }).name.length.isEqualTo(1)
        }

        @Test
        fun `should truncate center if too long`() {
            val tooLong =
                "x".repeat(64) + "X" + "x".repeat(64)

            expectThat(DockerContainer.from(tooLong)).name.isEqualTo(
                "x".repeat(63) + "..." + "x".repeat(62)
            )
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
            }

            @ContainersTest @IdeaWorkaroundTest
            fun `should get status`(testContainers: TestContainers) {
                val container = testContainers.newRunningTestContainer()
                expectThat(container).hasState<Running> { get { status }.isNotEmpty() }
            }
        }

        @Nested
        inner class ListContainers {

            @ContainersTest @IdeaWorkaroundTest
            fun TestSpanScope.`should list containers and log`(testContainers: TestContainers) {
                val containers = (1..3).map { testContainers.newRunningTestContainer() }
                expectThat(DockerContainer.list()).contains(containers)
                expectThatRendered().contains("Listing all containers ✔︎")
            }
        }

        @Nested
        inner class Start {

            @Nested
            inner class NotExisting {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { start(attach = false) }.isFailed()
                    expectThatRendered().contains("Starting ${container.name} $Negative no such container".ansiRemoved)
                }
            }

            @Nested
            inner class Stopped {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { start(attach = false) }.isSuccessful()
                    expectThatRendered().contains("Starting ${container.name}")
                    expectThat(container.isRunning).isTrue()
                }

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should start attached by default`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer(5.seconds)
                    val passed =
                        measureTime { expectThat(container).get { start(attach = true) }.isSuccessful() }
                    expectThatRendered().contains("Starting ${container.name}")
                    expectThat(passed).isGreaterThanOrEqualTo(5.seconds)
                    expectThat(container.isRunning).isFalse()
                }

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should start multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newExitedTestContainer())
                    expectThat(
                        DockerContainer.start(
                            *containers.toTypedArray(),
                            attach = false,
                        )
                    ).isFailed()
                    expectThatRendered().contains("Starting ${containers[0].name} ${FIELD.ansiRemoved} ${containers[1].name} ${Negative.ansiRemoved} no such container")
                    expectThat(containers).all { not { hasState<Exited>() } }
                }
            }


            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.isRunning).isTrue()
                    expectThat(container).get { start(attach = false) }.isSuccessful()
                    expectThatRendered().contains("Starting ${container.name}")
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
                fun TestSpanScope.`should stop container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { stop() }.isFailed()
                    expectThatRendered().contains("Stopping ${container.name} ${Negative.ansiRemoved} no such container")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class NotRunning {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should stop container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { stop() }.isSuccessful()
                    expectThatRendered().contains("Stopping ${container.name}")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should stop container - but not remove it - and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.isRunning).isTrue()
                    expectThat(container).get { stop() }.isSuccessful()
                    expectThatRendered().contains("Stopping ${container.name}")
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).hasState<Exited>()
                }

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should stop multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                    expectThat(DockerContainer.stop(*containers.toTypedArray())).isFailed()
                    expectThatRendered().contains("Stopping ${containers[0].name} ${FIELD.ansiRemoved} ${containers[1].name} ${Negative.ansiRemoved} no such container")
                    expectThat(containers).all { not { hasState<State.Existent.Running>() } }
                }
            }
        }

        @Nested
        inner class Kill {

            @Nested
            inner class NotExisting {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should kill container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { kill() }.isFailed()
                    expectThatRendered().contains("Killing ${container.name} ${Negative.ansiRemoved} no such container")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class NotRunning {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should kill container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).get { kill() }.isFailed()
                    expectThatRendered().contains("Killing ${container.name}")
                    expectThat(container.isRunning).isFalse()
                }
            }

            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should kill container - but not remove it - and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.isRunning).isTrue()
                    expectThat(container).get { kill() }.isSuccessful()
                    expectThatRendered().contains("Killing ${container.name}")
                    expectThat(container.isRunning).isFalse()
                    expectThat(container).hasState<Exited> { exitCode.isNotEqualTo(0) }
                }

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should kill multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                    expectThat(DockerContainer.kill(*containers.toTypedArray())).isFailed()
                    expectThatRendered().contains("Killing ${containers[0].name} ${FIELD.ansiRemoved} ${containers[1].name} ${Negative.ansiRemoved} no such container")
                    expectThat(containers).all { not { hasState<State.Existent.Running>() } }
                }
            }
        }

        @Nested
        inner class Remove {

            @Nested
            inner class NotExistent {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    expectThat(container.remove()).isFailed()
                    expectThatRendered().contains("Removing ${container.name} ${Negative.ansiRemoved} no such container")
                }
            }

            @Nested
            inner class Existent {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    expectThat(container.remove()).isSuccessful()
                    expectThatRendered().contains("Removing ${container.name}")
                }
            }

            @Nested
            inner class Running {

                @ContainersTest @IdeaWorkaroundTest
                fun TestSpanScope.`should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    expectThat(container.remove()).isFailed()
                    expectThatRendered().contains("Removing ${container.name}")
                    expectThat(container.isRunning).isTrue()
                    expectThat(container.exists).isTrue()
                }
            }
        }

        @Nested
        inner class RemoveForcefully {

            @ContainersTest @IdeaWorkaroundTest
            fun TestSpanScope.`should remove forcibly container and log`(testContainers: TestContainers) {
                val container = testContainers.newRunningTestContainer()
                expectThat(container.remove(force = true)).isSuccessful()
                expectThatRendered().contains("Removing forcefully ${container.name}")
                expectThat(container.isRunning).isFalse()
                expectThat(container.exists).isFalse()
            }

            @ContainersTest @IdeaWorkaroundTest
            fun TestSpanScope.`should remove multiple and log`(testContainers: TestContainers) {
                val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                expectThat(DockerContainer.remove(*containers.toTypedArray(), force = true)).isSuccessful()
                expectThatRendered().contains("Removing forcefully ${containers[0].name}${FIELD.spaced}${containers[1].name} $OK".ansiRemoved)
                expectThat(containers).all { hasState<NotExistent>() }
            }
        }
    }
}

inline fun <reified T : State> Builder<DockerContainer>.hasState(): Builder<DockerContainer> =
    compose("status") {
        get { containerState }.isA<T>()
    }.then { if (allPassed) pass() else fail() }

inline fun <reified T : State> Builder<DockerContainer>.hasState(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<DockerContainer> =
    compose("status") {
        get { containerState }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }

val Builder<DockerContainer>.name get(): Builder<String> = get("name") { name }

val Builder<Exited>.exitCode get(): Builder<Int?> = get("exit code") { exitCode }
