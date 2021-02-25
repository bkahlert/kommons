package koodies.docker

import koodies.docker.DockerStartCommandLine.Companion.StartContext
import koodies.docker.DockerStartCommandLine.Options
import koodies.test.BuilderFixture.Companion.fixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerStartCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStartCommandLine = DockerStartCommandLine(init)
        expectThat(dockerStartCommandLine).isEqualTo(result)
    }
}


private val init: StartContext.() -> Unit = {
    options {
        attach { no }
        interactive { yes }
    }
    containers { +"container-1" + "container-2" }
}
private val result: DockerStartCommandLine = DockerStartCommandLine(
    options = Options(attach = false, interactive = true),
    containers = listOf("container-1", "container-2"),
)

val Docker.DockerStartCommandLineBuilderExpectation get() = DockerStartCommandLine fixture (init to result)
