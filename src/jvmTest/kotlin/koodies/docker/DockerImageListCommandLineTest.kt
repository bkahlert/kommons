package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerImageListCommandLine.Companion.CommandContext
import koodies.docker.DockerImageListCommandLine.Options
import koodies.docker.TestImages.HelloWorld
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DockerImageListCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerImageListCommandLine = DockerImageListCommandLine(init)
        expectThat(dockerImageListCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerImageListCommandLine>(
        DockerImageListCommandLine,
        {
            options {
                all { no }
            }
            image by HelloWorld
        },
        DockerImageListCommandLine(
            options = Options(all = false),
            image = HelloWorld
        ),
    )
}
