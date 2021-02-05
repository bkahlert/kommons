@file:Suppress("FINAL_UPPER_BOUND")

package koodies.docker

import koodies.test.test
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class DockerImageTest {

    @Test
    fun `should format image`() {
        expectThat(dockerImage { "repo" / "name" }).toStringIsEqualTo("repo/name")
    }

    @Test
    fun `should format official image`() {
        expectThat(dockerImage { official("repo") }).toStringIsEqualTo("repo")
    }

    @Test
    fun `should format image with tag`() {
        expectThat(dockerImage { "repo" / "name" tag "my-tag" }).toStringIsEqualTo("repo/name:my-tag")
    }

    @Test
    fun `should format image with digest`() {
        expectThat(dockerImage { "repo" / "name" digest "sha..." }).toStringIsEqualTo("repo/name@sha...")
    }

    @Test
    fun `should return formatted as toString()`() {
        expectThat(dockerImage { "repo" / "name" digest "sha..." }).toStringIsEqualTo("repo/name@sha...")
    }

    @TestFactory
    fun `should throw on illegal repository`() = testEach("", "REPO", "r'e'p'o") { repo ->
        expectThrowing { dockerImage { repo / "path" } }.that { isFailure().isA<IllegalArgumentException>() }
    }

    @TestFactory
    fun `should throw on illegal path`() = testEach("", "PATH", "p'a't'h") { path ->
        expectThrowing { dockerImage { "repo" / path } }.that { isFailure().isA<IllegalArgumentException>() }
    }

    @TestFactory
    fun `should throw on illegal specifier`() = test("") { specifier ->
        expectThrowing { dockerImage { "repo" / "path" tag specifier } }.that { isFailure().isA<IllegalArgumentException>() }
        expectThrowing { dockerImage { "repo" / "path" digest specifier } }.that { isFailure().isA<IllegalArgumentException>() }
    }
}
