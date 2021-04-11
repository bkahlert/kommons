package koodies.docker

import koodies.concurrent.process.output
import koodies.docker.Docker.run
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.SystemIoExclusive
import koodies.test.test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess

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

        @Test
        fun `should provide commands`() {
            expectCatching { Docker.containers.list() }.isSuccess()
        }
    }

    @SystemIoExclusive
    @DockerRequiring(["busybox"])
    @TestFactory
    fun `should provide builder access points`() = test(Docker) {
//        expect { search(DockerSearchCommandLineBuilderExpectation.init) }.that { o }
//        expect { start(Docker.DockerStartCommandLineBuilderExpectation.init) }.that { isEqualTo(Docker.DockerStartCommandLineBuilderExpectation.result) }
        expect { stop(DockerStopCommandLineTest.init) }.that { isEqualTo(DockerStopCommandLineTest.result) }
    }

    @DockerRequiring(["busybox"])
    @Nested
    inner class RunCommand {

        @SystemIoExclusive
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

    @DockerRequiring(["busybox"])
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
