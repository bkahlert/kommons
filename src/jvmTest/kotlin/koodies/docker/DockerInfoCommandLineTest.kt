package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerInfoCommandLine.Companion.CommandContext
import koodies.docker.DockerInfoCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DockerInfoCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerInfoCommandLine = DockerInfoCommandLine(DockerInfoCommandLineTest.init)
        expectThat(dockerInfoCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerInfoCommandLine>(
        DockerInfoCommandLine,
        {
            options {
                format { "{{json .}}" }
            }
        },
        DockerInfoCommandLine(
            options = Options(format = "{{json .}}"),
        ),
    )
}
