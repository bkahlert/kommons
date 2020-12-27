@file:Suppress("FINAL_UPPER_BOUND")

package koodies.docker

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isSuccess

@Execution(CONCURRENT)
class DockerRepositoryTest {
    @Nested
    inner class Instantiation {
        @Test
        fun `should succeed on at least one component`() {
            expectCatching { DockerRepository(listOf(PathComponent.of("repo"))) }.isSuccess()
        }
    }

    @Nested
    inner class OfFactory {
        @Test
        fun `should succeed on non-empty list`() {
            expectCatching { DockerRepository.of(listOf("repo")) }.isSuccess()
        }

        @Test
        fun `should succeed on non-empty vararg`() {
            expectCatching { DockerRepository.of("repo") }.isSuccess()
        }

        @Test
        fun `should throw on non-empty list`() {
            expectCatching { DockerRepository.of(emptyList()) }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on non-empty vararg`() {
            expectCatching { DockerRepository.of() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Test
    fun `should format`() {
        expectThat(DockerRepository(listOf(PathComponent.of("repo"), PathComponent.of("name")))).formatted.isEqualTo("repo/name")
    }

    @Test
    fun `should throw on empty`() {
        expectCatching { DockerRepository(emptyList()).format() }.isFailure().isA<IllegalArgumentException>()
    }
}

val <T : DockerRepository> Assertion.Builder<T>.formatted
    get() = get("formatted %s") { format() }
