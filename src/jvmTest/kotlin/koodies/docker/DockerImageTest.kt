@file:Suppress("FINAL_UPPER_BOUND")

package koodies.docker

import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class DockerImageTest {
    
    @Test
    fun `should format image`() {
        expectThat(DockerImage.image(DockerRepository.of("repo", "name"))).formatted.isEqualTo("repo/name")
    }

    @Test
    fun `should format image with tag`() {
        expectThat(DockerImage.imageWithTag(DockerRepository.of("repo", "name"), Tag("my-tag"))).formatted.isEqualTo("repo/name:my-tag")
    }

    @Test
    fun `should format image with digest`() {
        expectThat(DockerImage.imageWithDigest(DockerRepository.of("repo", "name"), Digest("sha..."))).formatted.isEqualTo("repo/name@sha...")
    }

    @Test
    fun `should return formatted as toString()`() {
        expectThat(DockerImage.imageWithDigest(DockerRepository.of("repo", "name"), Digest("sha..."))).toStringIsEqualTo("repo/name@sha...")
    }

    @Test
    fun `should throw on empty paths`() {
        expectCatching { DockerImage(DockerRepository(emptyList()), OptionalTagOrDigest.none()) }.isFailure().isA<IllegalArgumentException>()
    }
}

val <T : DockerImage> Assertion.Builder<T>.formatted
    get() = get("formatted %s") { formatted }
