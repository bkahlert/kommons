package koodies.docker

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class DockerTest {

    @TestFactory
    fun `should provide builder access points`() = test(Docker) {
        expect { start(Docker.DockerStartCommandLineBuilderExpectation.init) }.that { isEqualTo(Docker.DockerStartCommandLineBuilderExpectation.result) }
        expect { run(Docker.DockerRunCommandLineBuilderExpectation.init) }.that { isEqualTo(Docker.DockerRunCommandLineBuilderExpectation.result) }
        expect { stop(Docker.DockerStopCommandLineBuilderExpectation.init) }.that { isEqualTo(Docker.DockerStopCommandLineBuilderExpectation.result) }
    }
}
