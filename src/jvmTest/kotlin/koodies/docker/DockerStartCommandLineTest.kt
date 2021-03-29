package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.ManagedProcess
import koodies.debug.CapturedOutput
import koodies.docker.DockerStartCommandLine.Companion.CommandContext
import koodies.docker.DockerStartCommandLine.Options
import koodies.logging.InMemoryLogger
import koodies.terminal.escapeSequencesRemoved
import koodies.test.BuilderFixture
import koodies.test.SystemIoExclusive
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue


@Execution(CONCURRENT)
class DockerStartCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStartCommandLine = DockerStartCommandLine(init)
        expectThat(dockerStartCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DockerTestImageExclusive.DOCKER_TEST_CONTAINER.stop()
        }

        @Test
        fun InMemoryLogger.`should start container and log`() {
            expectThat(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.container.start {}).isA<ManagedProcess>()
            expectThat(logged).escapeSequencesRemoved.contains("Removing ${DockerTestImageExclusive.DOCKER_TEST_CONTAINER}")
            expectThat(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.isRunning).isTrue()
        }

        @SystemIoExclusive
        @Test
        fun `should start image and print`(capturedOutput: CapturedOutput) {
            expectThat(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.container.start {}).isA<ManagedProcess>()
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Removing ${DockerTestImageExclusive.DOCKER_TEST_CONTAINER}")
            expectThat(DockerTestImageExclusive.DOCKER_TEST_CONTAINER.isRunning).isTrue()
        }

        @AfterEach
        fun tearDown() {
            DockerTestImageExclusive.DOCKER_TEST_CONTAINER.stop()
        }
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerStartCommandLine>(
        DockerStartCommandLine,
        {
            options {
                attach { no }
                interactive { yes }
            }
            containers { +"container-x" + "container-y" }
        },
        DockerStartCommandLine(
            options = Options(attach = false, interactive = true),
            containers = listOf("container-1", "container-2"),
        ),
    )
}
