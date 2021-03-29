package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.ManagedProcess
import koodies.debug.CapturedOutput
import koodies.docker.DockerRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerRemoveCommandLine.Options
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
class DockerRemoveCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerRemoveCommandLine = DockerRemoveCommandLine(init)
        expectThat(dockerRemoveCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DOCKER_TEST_CONTAINER.start()
        }

        @Test
        fun InMemoryLogger.`should remove container and log`() {
            expectThat(DOCKER_TEST_CONTAINER.container.remove {}).isA<ManagedProcess>()
            expectThat(logged).escapeSequencesRemoved.contains("Removing $DOCKER_TEST_CONTAINER")
            expectThat(DOCKER_TEST_CONTAINER.isRunning).isFalse()
        }

        @SystemIoExclusive
        @Test
        fun `should remove container and print`(capturedOutput: CapturedOutput) {
            expectThat(DOCKER_TEST_CONTAINER.container.remove {}).isA<ManagedProcess>()
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Removing $DOCKER_TEST_CONTAINER")
            expectThat(DOCKER_TEST_CONTAINER.isRunning).isFalse()
        }

        @Test
        fun InMemoryLogger.`should remove container forcefully and log`() {
            expectThat(DOCKER_TEST_CONTAINER.container.remove {}).isA<ManagedProcess>()
            expectThat(logged).escapeSequencesRemoved.contains("Removing forcefully $DOCKER_TEST_CONTAINER")
            expectThat(DOCKER_TEST_CONTAINER.isRunning).isFalse()
        }

        @SystemIoExclusive
        @Test
        fun `should remove container forcefully and print`(capturedOutput: CapturedOutput) {
            expectThat(DOCKER_TEST_CONTAINER.container.remove {}).isA<ManagedProcess>()
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Removing forcefully $DOCKER_TEST_CONTAINER")
            expectThat(DOCKER_TEST_CONTAINER.isRunning).isFalse()
        }
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerRemoveCommandLine>(
        DockerRemoveCommandLine,
        {
            options {
                force { yes }
                link { "the-link" }
                volumes { +"a" + "b" }
            }
            containers { +"container-x" + "container-y" }
        },
        DockerRemoveCommandLine(
            options = Options(force = true, link = "the-link", volumes = listOf("a", "b")),
            containers = listOf("container-x", "container-y"),
        ),
    )
}
