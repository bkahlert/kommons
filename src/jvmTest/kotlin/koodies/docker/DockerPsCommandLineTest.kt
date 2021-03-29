package koodies.docker

import koodies.builder.Init
import koodies.debug.CapturedOutput
import koodies.docker.DockerPsCommandLine.Companion.CommandContext
import koodies.docker.DockerPsCommandLine.Options
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_CONTAINER
import koodies.logging.InMemoryLogger
import koodies.terminal.escapeSequencesRemoved
import koodies.test.BuilderFixture
import koodies.test.SystemIoExclusive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue


@Execution(CONCURRENT)
class DockerPsCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerPsCommandLine = DockerPsCommandLine(init)
        expectThat(dockerPsCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DOCKER_TEST_CONTAINER.start()
        }

        @Test
        fun InMemoryLogger.`should list containers and log`() {
            expectThat(Docker.ps {}).contains(DOCKER_TEST_CONTAINER.container)
            expectThat(logged).escapeSequencesRemoved.contains("Listing containers")
        }

        @SystemIoExclusive
        @Test
        fun `should list containers and print`(capturedOutput: CapturedOutput) {
            expectThat(Docker.ps {}).contains(DOCKER_TEST_CONTAINER.image)
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Listing containers")
        }

        @Test
        fun InMemoryLogger.`should check if is running and log`() {
            expectThat(DOCKER_TEST_CONTAINER.container.isRunning()).isTrue()
            expectThat(logged).escapeSequencesRemoved.contains("Checking if $DOCKER_TEST_CONTAINER is running")

            DOCKER_TEST_CONTAINER.stop()
            expectThat(DOCKER_TEST_CONTAINER.container.isRunning()).isFalse()
        }

        @SystemIoExclusive
        @Test
        fun `should check if is running and print`(capturedOutput: CapturedOutput) {
            expectThat(DOCKER_TEST_CONTAINER.container.isRunning()).isTrue()
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Checking if $DOCKER_TEST_CONTAINER is running")

            DOCKER_TEST_CONTAINER.stop()
            expectThat(DOCKER_TEST_CONTAINER.container.isRunning()).isFalse()
        }
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerPsCommandLine>(
        DockerPsCommandLine,
        {
            options {
                all { no }
                filter { "key" to "value" }
                exactName { "container-name" }
            }
        },
        DockerPsCommandLine(
            options = Options(all = false, filters = listOf("key" to "value", "name" to "name=^container-name$")),
        ),
    )
}
