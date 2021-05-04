package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerStopCommandLine.Companion.CommandContext
import koodies.docker.DockerStopCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.time.seconds

class DockerStopCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStopCommandLine = DockerStopCommandLine(init)
        expectThat(dockerStopCommandLine).isEqualTo(result)
    }

    @Test
    fun `should round`() {
        val dockerStopCommandLine = DockerStopCommandLine { init(); options { timeout { 1.6.seconds } } }
        expectThat(dockerStopCommandLine.options.time).isEqualTo(2)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerStopCommandLine>(
        DockerStopCommandLine,
        {
            options {
                time { 5 }
            }
            containers { +"container-x" + "container-y" }
        },
        DockerStopCommandLine(
            options = Options(time = 5),
            containers = listOf("container-x", "container-y"),
        ),
    )
}
