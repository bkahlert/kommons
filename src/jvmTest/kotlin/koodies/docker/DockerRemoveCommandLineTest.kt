package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerRemoveCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DockerRemoveCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerRemoveCommandLine = DockerRemoveCommandLine(init)
        expectThat(dockerRemoveCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerRemoveCommandLine>(
        DockerRemoveCommandLine,
        {
            options {
                force { yes }
                link { yes }
                volumes { yes }
            }
            containers { +"container-x" + "container-y" }
        },
        DockerRemoveCommandLine(
            options = Options(force = true, link = true, volumes = true),
            containers = listOf("container-x", "container-y"),
        ),
    )
}
