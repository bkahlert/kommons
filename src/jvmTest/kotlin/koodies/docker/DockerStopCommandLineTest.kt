package koodies.docker

import koodies.builder.Init
import koodies.debug.CapturedOutput
import koodies.docker.DockerStopCommandLine.Companion.CommandContext
import koodies.docker.DockerStopCommandLine.Options
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_CONTAINER
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.BuilderFixture
import koodies.test.SystemIoExclusive
import koodies.test.UniqueId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import kotlin.time.seconds

@Execution(CONCURRENT)
class DockerStopCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStopCommandLine = DockerStopCommandLine(init)
        expectThat(dockerStopCommandLine).isEqualTo(result)
    }

    @Test
    fun `should round`() {
        val dockerStopCommandLine = DockerStopCommandLine { init(); options { timeout { 1.6.seconds } } }
        expectThat(dockerStopCommandLine.options.time).isEqualTo(2)
    }

    @Tag("docker")
    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @BeforeEach
        fun setUp() {
            DOCKER_TEST_CONTAINER.start()
        }

        @Nested
        inner class NotRunning {

            @Test
            fun InMemoryLogger.`should stop container and log`(uniqueId: UniqueId) {
                val container = DockerTestUtil.createContainer(uniqueId)
                expectThat(container.stop {}).isTrue()
                expectThatLogged().contains("Stopping $container")
            }

            @SystemIoExclusive
            @Test
            fun `should stop container and print`(uniqueId: UniqueId, capturedOutput: CapturedOutput) {
                val container = DockerTestUtil.createContainer(uniqueId)
                expectThat(container.stop {}).isTrue()
                expectThat(capturedOutput).contains("Stopping $container")
            }
        }

        @Nested
        inner class Running {

            @Test
            fun InMemoryLogger.`should stop container and log`() {
                use(DOCKER_TEST_CONTAINER) {
                    expectThat(it.container.stop {}).isTrue()
                    expectThatLogged().contains("Stopping ${it.container}")
                    expectThat(it.container.isRunning).isTrue()
                    expectThat(it.container.exists).isTrue()
                }
            }

            @SystemIoExclusive
            @Test
            fun `should stop container and print`(capturedOutput: CapturedOutput) {
                use(DOCKER_TEST_CONTAINER) {
                    expectThat(it.container.stop {}).isTrue()
                    expectThat(capturedOutput).contains("Stopping ${it.container}")
                    expectThat(it.container.isRunning).isTrue()
                    expectThat(it.container.exists).isTrue()
                }
            }
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
