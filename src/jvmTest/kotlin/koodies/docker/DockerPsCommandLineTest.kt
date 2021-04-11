package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerPsCommandLine.Companion.CommandContext
import koodies.docker.DockerPsCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerPsCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerPsCommandLine = DockerPsCommandLine(init)
        expectThat(dockerPsCommandLine).isEqualTo(result)
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerPsCommandLine>(
        DockerPsCommandLine,
        {
            options {
                all { no }
                filter { "key" to "value with spaces" }
                exactName("container-name")
            }
        },
        DockerPsCommandLine(
            options = Options(all = false, filters = listOf("key" to "value with spaces", "name" to "^container-name$")),
        ),
    )
}
