package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerStartCommandLine.Companion.CommandContext
import koodies.docker.DockerStartCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerStartCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStartCommandLine = DockerStartCommandLine(init)
        expectThat(dockerStartCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerStartCommandLine>(
        DockerStartCommandLine,
        {
            options {
                attach { no }
                interactive { yes }
            }
            containers { +"container-x" + "container-y" }
        },
        DockerStartCommandLine(
            options = Options(attach = false, interactive = true),
            containers = listOf("container-x", "container-y"),
        ),
    )
}
