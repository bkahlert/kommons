package koodies.docker

import koodies.builder.Init
import koodies.debug.CapturedOutput
import koodies.docker.DockerImageListCommandLine.Companion.CommandContext
import koodies.docker.DockerImageListCommandLine.Options
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_IMAGE
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
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
class DockerImageListCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerImageListCommandLine = DockerImageListCommandLine(init)
        expectThat(dockerImageListCommandLine).isEqualTo(result)
    }

    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DOCKER_TEST_IMAGE.pull()
        }

        @Nested
        inner class ListImages {

            @Test
            fun InMemoryLogger.`should list images and log`() {
                expectThat(Docker.images.list {}).contains(DOCKER_TEST_IMAGE.image)
                expectThatLogged().contains("Listing images")
            }

            @SystemIoExclusive
            @Test
            fun `should list images and print`(capturedOutput: CapturedOutput) {
                expectThat(Docker.images.list {}).contains(DOCKER_TEST_IMAGE.image)
                expectThat(capturedOutput).contains("Listing images")
            }
        }


        @Nested
        inner class ListImage {

            @Test
            fun InMemoryLogger.`should list image and log`() {
                expectThat(DOCKER_TEST_IMAGE.image.listImages {}).contains(DOCKER_TEST_IMAGE.image)
                expectThatLogged().contains("Listing $DOCKER_TEST_IMAGE images")

                DOCKER_TEST_IMAGE.remove()
                expectThatLogged().contains("Listing $DOCKER_TEST_IMAGE images")
            }

            @SystemIoExclusive
            @Test
            fun `should list image and print`(capturedOutput: CapturedOutput) {
                expectThat(DOCKER_TEST_IMAGE.image.listImages {}).contains(DOCKER_TEST_IMAGE.image)
                expectThat(capturedOutput).contains("Listing $DOCKER_TEST_IMAGE images")
            }
        }


        @Nested
        inner class IsPulled {

            @Test
            fun InMemoryLogger.`should check if is pulled and log`() {
                expectThat(DOCKER_TEST_IMAGE.image.isPulled()).isTrue()
                expectThatLogged().contains("Checking if $DOCKER_TEST_IMAGE is pulled")

                DOCKER_TEST_IMAGE.remove()
                expectThat(DOCKER_TEST_IMAGE.image.isPulled()).isFalse()
            }

            @SystemIoExclusive
            @Test
            fun `should check if is pulled and print`(capturedOutput: CapturedOutput) {
                expectThat(DOCKER_TEST_IMAGE.image.isPulled()).isTrue()
                expectThat(capturedOutput).contains("Checking if $DOCKER_TEST_IMAGE is pulled")

                DOCKER_TEST_IMAGE.remove()
                expectThat(DOCKER_TEST_IMAGE.image.isPulled()).isFalse()
            }
        }
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerImageListCommandLine>(
        DockerImageListCommandLine,
        {
            options {
                all { no }
            }
            image by DOCKER_TEST_IMAGE.image
        },
        DockerImageListCommandLine(
            options = Options(all = false),
            image = DOCKER_TEST_IMAGE.image
        ),
    )
}
