package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.ManagedProcess
import koodies.debug.CapturedOutput
import koodies.docker.DockerStopCommandLine.Companion.CommandContext
import koodies.docker.DockerStopCommandLine.Options
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
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse

@Execution(CONCURRENT)
class DockerStopCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStopCommandLine = DockerStopCommandLine(init)
        expectThat(dockerStopCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DOCKER_TEST_CONTAINER.start()
        }

        @Test
        fun InMemoryLogger.`should stop container and log`() {
            expectThat(DOCKER_TEST_CONTAINER.container.stop {}).isA<ManagedProcess>()
            expectThat(logged).escapeSequencesRemoved.contains("Removing $DOCKER_TEST_CONTAINER")
            expectThat(DOCKER_TEST_CONTAINER.isRunning).isFalse()
        }

        @SystemIoExclusive
        @Test
        fun `should stop image and print`(capturedOutput: CapturedOutput) {
            expectThat(DOCKER_TEST_CONTAINER.container.stop {}).isA<ManagedProcess>()
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Removing $DOCKER_TEST_CONTAINER")
            expectThat(DOCKER_TEST_CONTAINER.isRunning).isFalse()
        }
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
