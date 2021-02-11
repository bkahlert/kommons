@file:Suppress("FINAL_UPPER_BOUND")

package koodies.docker

import koodies.docker.DockerImage.ImageContext
import koodies.test.test
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class DockerImageTest {

    private val imageInit: Init = { "repo" / "name" }
    private val officialImageInit: Init = { official("repo") }
    private val imageWithTagInit: Init = { "repo" / "name" tag "my-tag" }
    private val imageWithDigestInit: Init = { "repo" / "name" digest "sha256:abc" }

    @TestFactory
    fun `should format and parse image instance `() = listOf(
        imageInit to "repo/name",
        officialImageInit to "repo",
        imageWithTagInit to "repo/name:my-tag",
        imageWithDigestInit to "repo/name@sha256:abc",
    ).testEach { (init, string) ->
        expect { DockerImage(init) }.that { toStringIsEqualTo(string) }
        expect { DockerImage.parse(string) }.that { isEqualTo(DockerImage(init)) }
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

typealias Init = ImageContext.() -> DockerImage
