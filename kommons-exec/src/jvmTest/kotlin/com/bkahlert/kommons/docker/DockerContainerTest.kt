package com.bkahlert.kommons.docker

import com.bkahlert.kommons.docker.DockerContainer.State
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Exited
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Running
import com.bkahlert.kommons.docker.DockerContainer.State.NotExistent
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.text.Semantics.FieldDelimiters.FIELD
import com.bkahlert.kommons.text.Semantics.Symbols.Negative
import com.bkahlert.kommons.text.Semantics.Symbols.OK
import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.text.spaced
import com.bkahlert.kommons.tracing.TestSpanScope
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.matchers.string.shouldHaveMinLength
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldNotMatch
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion.Builder
import strikt.assertions.isA
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@Execution(CONCURRENT)
class DockerContainerTest {

    @Nested
    inner class ConstructorInstantiation {

        @TestFactory
        fun `should throw on illegal name`() = ILLEGAL_NAMES.testEach {
            shouldThrow<IllegalArgumentException> { DockerContainer(it) }
        }

        @TestFactory
        fun `should accept legal name`() = LEGAL_NAMES.testEach {
            shouldNotThrowAny { DockerContainer(it) }
        }
    }

    @Nested
    inner class ToString {

        @ContainersTest
        fun `should contain actual state`(testContainers: TestContainers) = testAll {
            testContainers.newNotExistentContainer().apply { containerState }
                .toString() shouldMatch """DockerContainer \{[\s\S]*name[ =|:] .*,[\s\S]*state[ =|:] not existent[\s\S]*}""".toRegex()
            testContainers.newRunningTestContainer().apply { containerState }
                .toString() shouldMatch """DockerContainer \{[\s\S]*name[ =|:] .*,[\s\S]*state[ =|:] ▶ running[\s\S]*}""".toRegex()
            testContainers.newExitedTestContainer().apply { containerState }
                .toString() shouldMatch """DockerContainer \{[\s\S]*name[ =|:] .*,[\s\S]*state[ =|:] ✔︎ exited[\s\S]*}""".toRegex()
        }
    }

    @Nested
    inner class Equality {

        @Test
        fun `should be equal on same name`() {
            DockerContainer("docker-container-1") shouldBe DockerContainer("docker-container-1")
        }

        @Test
        fun `should be unequal on different name`() {
            DockerContainer("docker-container-1") shouldNotBe DockerContainer("docker-container-2")
        }
    }

    @Nested
    inner class Sanitization {

        @Test
        fun `should append random suffix to container`() {
            val container = DockerContainer("docker-container")
            DockerContainer { container.withRandomSuffix }
                .shouldBeInstanceOf<DockerContainer>().name shouldMatch Regex(".*--[\\da-zA-Z]{4}")
        }

        @Nested
        inner class StringBased {

            @TestFactory
            fun `should sanitize illegal name`() = ILLEGAL_NAMES.testEach {
                DockerContainer.from(it).name shouldHaveMinLength 1
                DockerContainer { it.sanitized }.name shouldHaveMinLength 1
            }

            @TestFactory
            fun `should sanitize legal name`() = LEGAL_NAMES.testEach {
                DockerContainer.from(it).name shouldBe it
                DockerContainer { it.sanitized }.name shouldBe it
            }

            @TestFactory
            fun `should not append random suffix by default`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEach {
                DockerContainer.from(it).name shouldNotMatch ".*--[\\da-zA-Z]{4}"
            }

            @TestFactory
            fun `should append random suffix if specified`() = (ILLEGAL_NAMES + LEGAL_NAMES).testEachOld {
                DockerContainer.from(it, randomSuffix = true).name shouldMatch Regex(".*--[\\da-zA-Z]{4}")
                DockerContainer { it.withRandomSuffix }.name shouldMatch Regex(".*--[\\da-zA-Z]{4}")
            }
        }

        @Nested
        inner class PathBased {

            @Suppress("SpellCheckingInspection")
            @Test
            fun `should sanitize illegal path`() {
                val path = Paths.get("~file202")
                DockerContainer.from(path, randomSuffix = false).name shouldBe "Xfile202"
                DockerContainer { path.sanitized }.name shouldBe "Xfile202"
            }

            @Test
            fun `should sanitize legal path`() {
                val path = Paths.get("2020-08-20-img-lite.img")
                DockerContainer.from(path, randomSuffix = false).name shouldBe "2020-08-20-img-lite.img"
                DockerContainer { path.sanitized }.name shouldBe "2020-08-20-img-lite.img"
            }

            @Test
            fun `should only use filename`() {
                val path = Paths.get("dir/2020-08-20-img-lite.img")
                DockerContainer.from(path).name shouldNotContain "dir"
                DockerContainer { path.withRandomSuffix }.name shouldNotContain "dir"
            }

            @Test
            fun `should append random suffix by default`() {
                DockerContainer.from(Paths.get("2020-08-20-img-lite.img")).name shouldMatch Regex(".*--[\\da-zA-Z]{4}")
                DockerContainer { Paths.get("2020-08-20-img-lite.img").withRandomSuffix }.name shouldMatch Regex(".*--[\\da-zA-Z]{4}")
            }

            @TestFactory
            fun `should not append random suffix if specified`() {
                DockerContainer.from(Paths.get("2020-08-20-img-lite.img"), randomSuffix = false).name shouldNotMatch ".*--[\\da-zA-Z]{4}"
            }
        }

        @Test
        fun `should fill name if too short`() {
            DockerContainer.from("").name shouldHaveLength 1
            DockerContainer { "".sanitized }.name shouldHaveLength 1
        }

        @Test
        fun `should truncate center if too long`() {
            val tooLong =
                "x".repeat(64) + "X" + "x".repeat(64)

            DockerContainer.from(tooLong).name shouldBe "x".repeat(63) + "..." + "x".repeat(62)
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

            @ContainersTest
            fun `should get status of non-existent`(testContainers: TestContainers) {
                val container = testContainers.newNotExistentContainer()
                container.containerState.shouldBeInstanceOf<NotExistent>()
            }

            @ContainersTest
            fun `should get status`(testContainers: TestContainers) {
                val container = testContainers.newRunningTestContainer()
                container.containerState.shouldBeInstanceOf<Running>().status.shouldNotBeEmpty()
            }
        }

        @Nested
        inner class ListContainers {

            @ContainersTest
            fun TestSpanScope.`should list containers and log`(testContainers: TestContainers) {
                val containers = (1..3).map { testContainers.newRunningTestContainer() }
                DockerContainer.list().shouldContainAll(containers)
                rendered() shouldContain "Listing all containers ✔︎"
            }
        }

        @Nested
        inner class Start {

            @Nested
            inner class NotExisting {

                @ContainersTest
                fun TestSpanScope.`should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    container.isRunning shouldBe false
                    container.start(attach = false).successful shouldBe false
                    rendered() shouldContain "Starting ${container.name} $Negative no such container".ansiRemoved
                }
            }

            @Nested
            inner class Stopped {

                @ContainersTest
                fun TestSpanScope.`should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    container.isRunning shouldBe false
                    container.start(attach = false).successful shouldBe true
                    rendered() shouldContain "Starting ${container.name}"
                    container.isRunning shouldBe true
                }

                @ContainersTest
                fun TestSpanScope.`should start attached by default`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer(5.seconds)
                    val passed =
                        measureTime { container.start(attach = true).successful shouldBe true }
                    rendered() shouldContain "Starting ${container.name}"
                    passed shouldBeGreaterThanOrEqualTo 5.seconds
                    container.isRunning shouldBe false
                }

                @ContainersTest
                fun TestSpanScope.`should start multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newExitedTestContainer())
                    DockerContainer.start(*containers.toTypedArray(), attach = false).successful shouldBe false
                    rendered()
                        .shouldContain("Starting ${containers[0].name} ${FIELD.ansiRemoved} ${containers[1].name} ${Negative.ansiRemoved} no such container")
                    containers.forNone { it.containerState.shouldBeInstanceOf<Exited>() }
                }
            }


            @Nested
            inner class Running {

                @ContainersTest
                fun TestSpanScope.`should start container and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    container.isRunning shouldBe true
                    container.start(attach = false).successful shouldBe true
                    rendered() shouldContain "Starting ${container.name}"
                    container.isRunning shouldBe true
                    container.containerState.shouldBeInstanceOf<State.Existent.Running>()
                }
            }
        }

        @Nested
        inner class Stop {

            @Nested
            inner class NotExisting {

                @ContainersTest
                fun TestSpanScope.`should stop container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    container.isRunning shouldBe false
                    container.stop().successful shouldBe false
                    rendered() shouldContain "Stopping ${container.name} ${Negative.ansiRemoved} no such container"
                    container.isRunning shouldBe false
                }
            }

            @Nested
            inner class NotRunning {

                @ContainersTest
                fun TestSpanScope.`should stop container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    container.isRunning shouldBe false
                    container.stop().successful shouldBe true
                    rendered() shouldContain "Stopping ${container.name}"
                    container.isRunning shouldBe false
                }
            }

            @Nested
            inner class Running {

                @ContainersTest
                fun TestSpanScope.`should stop container - but not remove it - and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    container.isRunning shouldBe true
                    container.stop().successful shouldBe true
                    rendered() shouldContain "Stopping ${container.name}"
                    container.isRunning shouldBe false
                    container.containerState.shouldBeInstanceOf<Exited>()
                }

                @ContainersTest
                fun TestSpanScope.`should stop multiple and log`(testContainers: TestContainers) {
                    val containers: List<DockerContainer> = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                    DockerContainer.stop(*containers.toTypedArray()).successful shouldBe false
                    rendered()
                        .shouldContain("Stopping ${containers[0].name} ${FIELD.ansiRemoved} ${containers[1].name} ${Negative.ansiRemoved} no such container")
                    containers.forNone { it.containerState.shouldBeInstanceOf<State.Existent.Running>() }
                }
            }
        }

        @Nested
        inner class Kill {

            @Nested
            inner class NotExisting {

                @ContainersTest
                fun TestSpanScope.`should kill container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    container.isRunning shouldBe false
                    container.kill().successful shouldBe false
                    rendered() shouldContain "Killing ${container.name} ${Negative.ansiRemoved} no such container"
                    container.isRunning shouldBe false
                }
            }

            @Nested
            inner class NotRunning {

                @ContainersTest
                fun TestSpanScope.`should kill container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    container.isRunning shouldBe false
                    container.kill().successful shouldBe false
                    rendered() shouldContain "Killing ${container.name}"
                    container.isRunning shouldBe false
                }
            }

            @Nested
            inner class Running {

                @ContainersTest
                fun TestSpanScope.`should kill container - but not remove it - and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    container.isRunning shouldBe true
                    container.kill().successful shouldBe true
                    rendered() shouldContain "Killing ${container.name}"
                    container.isRunning shouldBe false
                    container.containerState.shouldBeInstanceOf<Exited>().exitCode shouldNotBe 0
                }

                @ContainersTest
                fun TestSpanScope.`should kill multiple and log`(testContainers: TestContainers) {
                    val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                    DockerContainer.kill(*containers.toTypedArray()).successful shouldBe false
                    rendered()
                        .shouldContain("Killing ${containers[0].name} ${FIELD.ansiRemoved} ${containers[1].name} ${Negative.ansiRemoved} no such container")
                    containers.forNone { it.shouldBeInstanceOf<State.Existent.Running>() }
                }
            }
        }

        @Nested
        inner class Remove {

            @Nested
            inner class NotExistent {

                @ContainersTest
                fun TestSpanScope.`should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newNotExistentContainer()
                    container.remove().successful shouldBe false
                    rendered() shouldContain "Removing ${container.name} ${Negative.ansiRemoved} no such container"
                }
            }

            @Nested
            inner class Existent {

                @ContainersTest
                fun TestSpanScope.`should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newExitedTestContainer()
                    container.remove().successful shouldBe true
                    rendered() shouldContain "Removing ${container.name}"
                }
            }

            @Nested
            inner class Running {

                @ContainersTest
                fun TestSpanScope.`should remove container and log`(testContainers: TestContainers) {
                    val container = testContainers.newRunningTestContainer()
                    container.remove().successful shouldBe false
                    rendered() shouldContain "Removing ${container.name}"
                    container.isRunning shouldBe true
                    container.exists shouldBe true
                }
            }
        }

        @Nested
        inner class RemoveForcefully {

            @ContainersTest
            fun TestSpanScope.`should remove forcibly container and log`(testContainers: TestContainers) {
                val container = testContainers.newRunningTestContainer()
                container.remove(force = true).successful shouldBe true
                rendered() shouldContain "Removing forcefully ${container.name}"
                container.isRunning shouldBe false
                container.exists shouldBe false
            }

            @ContainersTest
            fun TestSpanScope.`should remove multiple and log`(testContainers: TestContainers) {
                val containers = listOf(testContainers.newNotExistentContainer(), testContainers.newRunningTestContainer())
                DockerContainer.remove(*containers.toTypedArray(), force = true).successful shouldBe true
                rendered() shouldContain "Removing forcefully ${containers[0].name}${FIELD.spaced}${containers[1].name} $OK".ansiRemoved
                containers.forAll { it.containerState.shouldBeInstanceOf<NotExistent>() }
            }
        }
    }
}

inline fun <reified T : State> Builder<DockerContainer>.hasState(): Builder<DockerContainer> =
    compose("status") {
        get { containerState }.isA<T>()
    }.then { if (allPassed) pass() else fail() }
