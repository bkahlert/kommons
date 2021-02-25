package koodies.docker

import koodies.docker.DockerRemoveCommandLine.Companion.RemoveContext
import koodies.docker.DockerRemoveCommandLine.Options
import koodies.test.BuilderFixture.Companion.fixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerRemoveCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerRemoveCommandLine = DockerRemoveCommandLine(init)
        expectThat(dockerRemoveCommandLine).isEqualTo(result)
    }
}


private val init: RemoveContext.() -> Unit = {
    options {
        force { yes }
        link { "the-link" }
        volumes { +"a" + "b" }
    }
    containers { +"container-x" + "container-y" }
}
private val result: DockerRemoveCommandLine = DockerRemoveCommandLine(
    options = Options(force = true, link = "the-link", volumes = listOf("a", "b")),
    containers = listOf("container-x", "container-y"),
)

val Docker.DockerRemoveCommandLineBuilderExpectation get() = DockerRemoveCommandLine fixture (init to result)
