package koodies.docker

import koodies.builder.Init
import koodies.debug.CapturedOutput
import koodies.docker.DockerRemoveCommandLine.Companion.CommandContext
import koodies.docker.DockerRemoveCommandLine.Options
import koodies.docker.DockerTestImageExclusive.Companion.DOCKER_TEST_CONTAINER
import koodies.logging.InMemoryLogger
import koodies.logging.expectThatLogged
import koodies.test.BuilderFixture
import koodies.test.SystemIoExclusive
import koodies.test.UniqueId
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue


@Execution(CONCURRENT)
class DockerRemoveCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerRemoveCommandLine = DockerRemoveCommandLine(init)
        expectThat(dockerRemoveCommandLine).isEqualTo(result)
    }

    @Tag("docker")
    @DockerTestImageExclusive
    @Nested
    inner class Extension {

        @Nested
        inner class Remove {

            @Nested
            inner class NotExistent {

                @Test
                fun InMemoryLogger.`should remove container and log`(uniqueId: UniqueId) {
                    val container = DockerContainer { uniqueId.uniqueId.sanitized }
                    expectThat(container.remove {}).isTrue()
                    expectThatLogged().contains("Removing $container")
                }

                @SystemIoExclusive
                @Test
                fun `should remove container and print`(uniqueId: UniqueId, capturedOutput: CapturedOutput) {
                    val container = DockerContainer { uniqueId.uniqueId.sanitized }
                    expectThat(container.remove {}).isTrue()
                    expectThat(capturedOutput).contains("Removing $container")
                }
            }

            @Nested
            inner class Existent {

                @Test
                fun InMemoryLogger.`should remove container and log`(uniqueId: UniqueId) {
                    val container = DockerTestUtil.createContainer(uniqueId)
                    expectThat(container.remove {}).isTrue()
                    expectThatLogged().contains("Removing $container")
                }

                @SystemIoExclusive
                @Test
                fun `should remove container and print`(uniqueId: UniqueId, capturedOutput: CapturedOutput) {
                    val container = DockerTestUtil.createContainer(uniqueId)
                    expectThat(container.remove {}).isTrue()
                    expectThat(capturedOutput).contains("Removing $container")
                }
            }

            @Nested
            inner class Running {

                @Test
                fun InMemoryLogger.`should remove container and log`() {
                    use(DOCKER_TEST_CONTAINER) {
                        expectThat(it.container.remove {}).isFalse()
                        expectThatLogged().contains("Removing ${it.container}")
                        expectThat(it.container.isRunning).isTrue()
                        expectThat(it.container.exists).isTrue()
                    }
                }

                @SystemIoExclusive
                @Test
                fun `should remove container and print`(capturedOutput: CapturedOutput) {
                    use(DOCKER_TEST_CONTAINER) {
                        expectThat(it.container.remove {}).isFalse()
                        expectThat(capturedOutput).contains("Removing ${it.container}")
                        expectThat(it.container.isRunning).isTrue()
                        expectThat(it.container.exists).isTrue()
                    }
                }
            }
        }

        @Nested
        inner class RemoveForcefully {

            @Test
            fun InMemoryLogger.`should remove container forcefully and log`() {
                use(DOCKER_TEST_CONTAINER) {
                    expectThat(it.container.remove { force { yes } }).isTrue()
                    expectThatLogged().contains("Removing forcefully ${it.container}")
                    expectThat(it.container.isRunning).isFalse()
                    expectThat(it.container.exists).isFalse()
                }
            }

            @SystemIoExclusive
            @Test
            fun `should remove container forcefully and print`(capturedOutput: CapturedOutput) {
                use(DOCKER_TEST_CONTAINER) {
                    expectThat(it.container.remove { force { yes } }).isTrue()
                    expectThat(capturedOutput).contains("Removing forcefully ${it.container}")
                    expectThat(it.container.isRunning).isFalse()
                    expectThat(it.container.exists).isFalse()
                }
            }
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
