package koodies.docker

import koodies.concurrent.process.output
import koodies.docker.Docker.run
import koodies.logging.InMemoryLogger
import koodies.test.SystemIoExclusive
import koodies.test.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class DockerTest {

    @SystemIoExclusive
    @DockerRequiring(["busybox"])
    @TestFactory
    fun `should provide builder access points`() = test(Docker) {
//        expect { start(Docker.DockerStartCommandLineBuilderExpectation.init) }.that { isEqualTo(Docker.DockerStartCommandLineBuilderExpectation.result) }
        expect {
            run {
                image { official("busybox") }
                commandLine {
                    command { "echo" }
                    arguments { +"test" }
                }
            }
        }.that { output.isEqualTo("test") }
        expect { stop(Docker.DockerStopCommandLineBuilderExpectation.init) }.that { isEqualTo(Docker.DockerStopCommandLineBuilderExpectation.result) }
    }

    @DockerRequiring(["busybox"])
    @Test
    fun InMemoryLogger.`should use existing logger`() {
        run {
            image { official("busybox") }
            commandLine {
                command { "echo" }
                arguments { +"test" }
            }
        }
    }
}
