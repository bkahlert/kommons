package koodies.docker

import koodies.builder.Init
import koodies.concurrent.process.ManagedProcess
import koodies.debug.CapturedOutput
import koodies.docker.DockerImagePullCommandLine.Companion.CommandContext
import koodies.docker.DockerImagePullCommandLine.Options
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
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class DockerImagePullCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerImagePullCommandLine = DockerImagePullCommandLine(init)
        expectThat(dockerImagePullCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DOCKER_TEST_IMAGE.remove()
        }

        @Test
        fun InMemoryLogger.`should pull image and log`() {
            expectThat(DOCKER_TEST_IMAGE.image.pull {}).isA<ManagedProcess>()
            expectThat(logged).escapeSequencesRemoved.contains("Pulling $DOCKER_TEST_IMAGE")
            expectThat(DOCKER_TEST_IMAGE.isPulled).isTrue()
        }

        @SystemIoExclusive
        @Test
        fun `should pull image and print`(capturedOutput: CapturedOutput) {
            expectThat(DOCKER_TEST_IMAGE.image.pull {}).isA<ManagedProcess>()
            expectThat(capturedOutput).escapeSequencesRemoved.contains("Pulling $DOCKER_TEST_IMAGE")
            expectThat(DOCKER_TEST_IMAGE.isPulled).isTrue()
        }
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerImagePullCommandLine>(
        DockerImagePullCommandLine,
        {
            options {
                allTags { yes }
            }
            image { official("busybox") }
        },
        DockerImagePullCommandLine(
            options = Options(allTags = true),
            image = DockerImage("busybox", emptyList(), null, null),
        ),
    )
}
