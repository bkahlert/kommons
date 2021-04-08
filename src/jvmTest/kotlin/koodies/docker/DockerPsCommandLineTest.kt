package koodies.docker

import koodies.builder.Init
import koodies.debug.CapturedOutput
import koodies.docker.DockerPsCommandLine.Companion.CommandContext
import koodies.docker.DockerPsCommandLine.Options
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_CONTAINER
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.BuilderFixture
import koodies.test.SystemIoExclusive
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerPsCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerPsCommandLine = DockerPsCommandLine(init)
        expectThat(dockerPsCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class DockerExtension {

        @Nested
        inner class ListContainers {

            @Test
            fun InMemoryLogger.`should list containers and log`() {
                use(DOCKER_TEST_CONTAINER) {
                    expectThat(Docker.ps {}).contains(it.container)
                    expectThatLogged().contains("Listing containers")
                }
            }

            @SystemIoExclusive
            @Test
            fun `should list containers and print`(capturedOutput: CapturedOutput) {
                use(DOCKER_TEST_CONTAINER) {
                    expectThat(Docker.ps {}).contains(it.container)
                    expectThat(capturedOutput).contains("Listing containers")
                }
            }
        }
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
