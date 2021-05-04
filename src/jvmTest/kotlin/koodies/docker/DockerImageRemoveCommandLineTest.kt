package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerImageRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerImageRemoveCommandLine.Options
import koodies.docker.TestImages.HelloWorld
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DockerImageRemoveCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerImageRemoveCommandLine = DockerImageRemoveCommandLine(init)
        expectThat(dockerImageRemoveCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerImageRemoveCommandLine>(
        DockerImageRemoveCommandLine,
        {
            options {
                force { no }
            }
            image by HelloWorld
        },
        DockerImageRemoveCommandLine(
            options = Options(force = false),
            images = listOf(HelloWorld)
        ),
    )
}
