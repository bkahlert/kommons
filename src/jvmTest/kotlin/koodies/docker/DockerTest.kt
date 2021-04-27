package koodies.docker

import koodies.concurrent.process.output
import koodies.docker.Docker.run
import koodies.docker.TestImages.BusyBox
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.IdeaWorkaroundTest
import koodies.test.SystemIOExclusive
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isSuccess
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class DockerTest {

    @Nested
    inner class ImageProperty {

        @Test
        fun `should build instances`() {
            expectThat(Docker.images { official("hello-world") }).isEqualTo(TestImages.HelloWorld)
        }

        @Test
        fun `should provide commands`() {
            expectCatching { Docker.images.list() }.isSuccess()
        }
    }

    @Nested
    inner class ContainerProperty {

        @Test
        fun `should build instances`() {
            expectThat(Docker.containers { "docker-container".sanitized }).isEqualTo(DockerContainer("docker-container"))
        }

        @DockerRequiring @Test
        fun `should provide commands`() {
            expectCatching { Docker.containers.list() }.isSuccess()
        }
    }

    @DockerRequiring @Test
    fun `should return true if engine is running`() {
        expectThat(Docker.engineRunning).isTrue()
    }

    @Nested
    inner class ContainerRunning {

        @ContainersTest @IdeaWorkaroundTest
        fun `should return true if container is running`(testContainers: TestContainers) {
            val container = testContainers.newRunningTestContainer()
            expectThat(Docker(container.name).isRunning).isTrue()
        }

        @ContainersTest @IdeaWorkaroundTest
        fun `should return false if container exited`(testContainers: TestContainers) {
            val container = testContainers.newExitedTestContainer()
            expectThat(Docker(container.name).isRunning).isFalse()
        }

        @ContainersTest @IdeaWorkaroundTest
        fun `should return false if container does not exist`(testContainers: TestContainers) {
            val container = testContainers.newNotExistentContainer()
            expectThat(Docker(container.name).isRunning).isFalse()
        }
    }

    @Disabled
    @DockerRequiring([BusyBox::class])
    @Nested
    inner class RunCommand {

        @SystemIOExclusive
        @Test
        fun `should run`() {
            val process = Docker.run {
                image { official("busybox") }
                commandLine {
                    command { "echo" }
                    arguments { +"test" }
                }
            }
            expectThat(process.output()).isEqualTo("test")
        }

        @Test
        fun InMemoryLogger.`should use existing logger`() {
            run {
                image { official("busybox") }
                commandLine {
                    command { "echo" }
                    arguments { +"test" }
                }
            }
            expectThatLogged().contains("test")
        }
    }

    @DockerRequiring([BusyBox::class])
    @Nested
    inner class StopCommand {

//        @SystemIoExclusive
//        @Test
//        fun `should stop`() {
//            val process = Docker.stop {
//                image { official("busybox") }
//                commandLine {
//                    command { "echo" }
//                    arguments { +"test" }
//                }
//            }
//            expectThat(process.output()).isEqualTo("test")
//        }
//
//        @Test
//        fun InMemoryLogger.`should use existing logger`() {
//            stop {
//                image { official("busybox") }
//                commandLine {
//                    command { "echo" }
//                    arguments { +"test" }
//                }
//            }
//            expectThatLogged().contains("test")
//        }
    }
}
