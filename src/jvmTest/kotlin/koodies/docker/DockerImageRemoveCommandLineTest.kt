package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerImageRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerImageRemoveCommandLine.Options
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_IMAGE
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
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
            image by DOCKER_TEST_IMAGE.image
        },
        DockerImageRemoveCommandLine(
            options = Options(force = false),
            images = listOf(DOCKER_TEST_IMAGE.image)
        ),
    )
}
