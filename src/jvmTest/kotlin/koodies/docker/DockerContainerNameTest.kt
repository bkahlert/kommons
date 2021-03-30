package koodies.docker

import koodies.docker.DockerContainer.Companion.toUniqueContainerName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.matches
import strikt.assertions.startsWith
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerContainerNameTest {

    @Test
    fun `should create name from path`() {
        val path = Path.of("~/.imgcstmzr.test/test/RaspberryPiLite/2020-11-29T21-46-47--9N3k/2020-08-20-raspios-buster-armhf-lite.img")
        expectThat(path.toUniqueContainerName()).sanitizedName
            .startsWith("2020-08-20--raspios")
            .hasLength(23)
            .get { takeLast(5) }.matches(Regex("-\\w{4}"))
    }
}

val Assertion.Builder<DockerContainer>.sanitizedName
    get() = get("sanitized name %s") { sanitized }
