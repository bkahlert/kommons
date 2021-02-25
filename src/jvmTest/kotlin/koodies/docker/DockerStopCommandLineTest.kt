package koodies.docker

import koodies.docker.DockerStopCommandLine.Companion.StopContext
import koodies.docker.DockerStopCommandLine.Options
import koodies.test.BuilderFixture.Companion.fixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


@Execution(CONCURRENT)
class DockerStopCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerStopCommandLine = DockerStopCommandLine(init)
        expectThat(dockerStopCommandLine).isEqualTo(result)
    }
}


private val init: StopContext.() -> Unit = {
    options { time { 20 } }
    containers { +"container-1" + "container-2" }
}
private val result: DockerStopCommandLine = DockerStopCommandLine(
    options = Options(time = 20),
    containers = listOf("container-1", "container-2"),
)

val Docker.DockerStopCommandLineBuilderExpectation get() = DockerStopCommandLine fixture (init to result)
