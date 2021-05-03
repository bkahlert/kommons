package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerKillCommandLine.Companion.CommandContext
import koodies.docker.DockerKillCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class DockerKillCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerKillCommandLine = DockerKillCommandLine(init)
        expectThat(dockerKillCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerKillCommandLine>(
        DockerKillCommandLine,
        {
            options {
                signal { "SIGHUP" }
            }
            containers { +"container-x" + "container-y" }
        },
        DockerKillCommandLine(
            options = Options(signal = "SIGHUP"),
            containers = listOf("container-x", "container-y"),
        ),
    )
}
