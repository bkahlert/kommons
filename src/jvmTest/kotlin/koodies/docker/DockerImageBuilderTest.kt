@file:Suppress("FINAL_UPPER_BOUND")

package koodies.docker

import koodies.docker.DockerImageBuilder.Companion.build
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class DockerImageBuilderTest {

    @Test
    fun `should build from string`() {
        expectThat(build { "repo" }).isEqualTo(DockerImage.image(DockerRepository.of("repo")))
    }

    @Test
    fun `should build from path components`() {
        expectThat(build { "repo" / "name" }).isEqualTo(DockerImage.image(DockerRepository.of("repo", "name")))
    }

    @Test
    fun `should build from path components and tag`() {
        expectThat(build { "repo" / "name" tag "tag" }).isEqualTo(DockerImage.imageWithTag(DockerRepository.of("repo", "name"), Tag("tag")))
    }

    @Test
    fun `should build from path components and digest`() {
        expectThat(build { "repo" / "name" digest "sha..." }).isEqualTo(DockerImage.imageWithDigest(DockerRepository.of("repo", "name"), Digest("sha...")))
    }
}
