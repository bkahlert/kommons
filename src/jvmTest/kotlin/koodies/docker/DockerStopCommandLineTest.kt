package koodies.docker

import koodies.docker.DockerStopCommandLine.Options
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerStopCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStopCommandLine = DockerStopCommandLine {
            options { time { 20 } }
            containers { +"container-1" + "container-2" }
        }
        expectThat(dockerStopCommandLine).isEqualTo(DockerStopCommandLine(
            options = Options(time = 20),
            containers = listOf("container-1", "container-2"),
        ))
    }
}
