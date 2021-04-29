package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerImagePullCommandLine.Companion.CommandContext
import koodies.docker.DockerImagePullCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class DockerImagePullCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerImagePullCommandLine = DockerImagePullCommandLine(init)
        expectThat(dockerImagePullCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerImagePullCommandLine>(
        DockerImagePullCommandLine,
        {
            options {
                allTags { yes }
            }
            image { "busybox" }
        },
        DockerImagePullCommandLine(
            options = Options(allTags = true),
            image = DockerImage("busybox", emptyList(), null, null),
        ),
    )
}
