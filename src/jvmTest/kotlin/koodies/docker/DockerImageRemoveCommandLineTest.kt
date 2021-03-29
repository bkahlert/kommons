package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.ManagedProcess
import koodies.debug.CapturedOutput
import koodies.docker.DockerImageRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerImageRemoveCommandLine.Options
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_IMAGE
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
class DockerImageRemoveCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerImageRemoveCommandLine = DockerImageRemoveCommandLine(init)
        expectThat(dockerImageRemoveCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DOCKER_TEST_IMAGE.pull()
        }

        @Test
        fun InMemoryLogger.`should remove image and log`() {
            expectThat(DOCKER_TEST_IMAGE.image.removeImage {}).isA<ManagedProcess>()
            expectThat(logged).escapeSequencesRemoved.contains("Removing $DOCKER_TEST_IMAGE")
            expectThat(DOCKER_TEST_IMAGE.isPulled).isFalse()
        }

        @SystemIoExclusive
        @Test
        fun `should remove image and print`(capturedOutput: CapturedOutput) {
            expectThat(DOCKER_TEST_IMAGE.image.removeImage {}).isA<ManagedProcess>()
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Removing $DOCKER_TEST_IMAGE")
            expectThat(DOCKER_TEST_IMAGE.isPulled).isFalse()
        }
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerImageRemoveCommandLine>(
        DockerImageRemoveCommandLine,
        {
            options {
                force { no }
            }
            image by DOCKER_TEST_IMAGE.image
        },
        DockerImageRemoveCommandLine(
            options = Options(force = false),
            images = listOf(DOCKER_TEST_IMAGE.image)
        ),
    )
}
