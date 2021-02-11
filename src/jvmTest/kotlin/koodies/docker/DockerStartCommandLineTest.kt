package koodies.docker

import koodies.docker.DockerStartCommandLine.Options
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerStartCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStartCommandLine = DockerStartCommandLine {
            options {
                attach { no }
                interactive { yes }
            }
            containers { +"container-1" + "container-2" }
        }
        expectThat(dockerStartCommandLine).isEqualTo(DockerStartCommandLine(
            options = Options(attach = false, interactive = true),
            containers = listOf("container-1", "container-2"),
        ))
    }
}
