package koodies.docker

import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.expectLogged
import koodies.test.test
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class DockerImageTest {

    private val imageInit: DockerImageInit = { "repo" / "name" }
    private val officialImageInit: DockerImageInit = { official("repo") }
    private val imageWithTagInit: DockerImageInit = { "repo" / "name" tag "my-tag" }
    private val imageWithDigestInit: DockerImageInit = { "repo" / "name" digest "sha256:abc" }

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
    fun `should accept valid repositories and paths`() = testEach(
        "repo",
        "repo123",
        "repo.123",
        "repo_123",
        "repo-123",
    ) {
        expect { DockerImage { it / it } }.that { toStringIsEqualTo("$it/$it") }
    }

    @TestFactory
    fun `should throw on illegal repository`() = testEach("", "REPO", "r'e'p'o") { repo ->
        expectThrowing { DockerImage { repo / "path" } }.that { isFailure().isA<IllegalArgumentException>() }
    }

    @TestFactory
    fun `should throw on illegal path`() = testEach("", "PATH", "p'a't'h") { path ->
        expectThrowing { DockerImage { "repo" / path } }.that { isFailure().isA<IllegalArgumentException>() }
    }

    @TestFactory
    fun `should throw on illegal specifier`() = test("") { specifier ->
        expectThrowing { DockerImage { "repo" / "path" tag specifier } }.that { isFailure().isA<IllegalArgumentException>() }
        expectThrowing { DockerImage { "repo" / "path" digest specifier } }.that { isFailure().isA<IllegalArgumentException>() }
    }

    @TestFactory
    fun `should equal`() = test {
        aspect({ DockerImage.parse("repo/path") }) {
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), null, null)) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), "tag", null)) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), null, "digest")) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), "tag", "digest")) }

            expect { this }.that { not { isEqualTo(DockerImage("repo", listOf("other-path"), null, null)) } }
            expect { this }.that { not { isEqualTo(DockerImage("other-repo", listOf("path"), null, null)) } }
        }
        with { DockerImage.parse("repo/path:tag") }.then {
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), null, null)) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), "tag", null)) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), null, "digest")) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), "tag", "digest")) }

            expect { this }.that { not { isEqualTo(DockerImage("repo", listOf("path"), "other-tag", null)) } }
        }
        with { DockerImage.parse("repo/path@digest") }.then {
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), null, null)) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), "tag", null)) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), null, "digest")) }
            expect { this }.that { isEqualTo(DockerImage("repo", listOf("path"), "tag", "digest")) }

            expect { this }.that { not { isEqualTo(DockerImage("repo", listOf("path"), null, "other-digest")) } }
        }
    }

    @Nested
    inner class DockerCommands {

        private val testImage = DockerImage("hello-world", emptyList(), null, null)

        @Nested
        inner class ListImages {

            @BeforeEach
            fun setUp() {
                testImage.pull()
            }

            @Nested
            inner class ListImages {

                @Test
                fun `should list images and log`() {
                    expectThat(DockerImage.list()).contains(testImage)
                    BACKGROUND.expectLogged.contains("Listing images")
                }
            }


            @Nested
            inner class ListImage {

                @Test
                fun `should list existing image and log`() {
                    expectThat(testImage.list()).contains(testImage)
                    BACKGROUND.expectLogged.contains("Listing $testImage images")

                    testImage.remove()
                    expectThat(testImage.list()).isEmpty()
                    BACKGROUND.expectLogged.contains("Listing $testImage images")
                }
            }


            @Nested
            inner class IsPulled {

                @Test
                fun `should check if is pulled and log`() {
                    expectThat(testImage.isPulled).isTrue()
                    BACKGROUND.expectLogged.contains("Checking if $testImage is pulled")

                    testImage.remove()
                    expectThat(testImage.isPulled).isFalse()
                    BACKGROUND.expectLogged.contains("Checking if $testImage is pulled")
                }
            }
        }

        @Nested
        inner class Pull {

            @BeforeEach
            fun setUp() {
                testImage.remove()
            }

            @Test
            fun `should pull image and log`() {
                expectThat(testImage.pull()).isSuccessful()
                BACKGROUND.expectLogged.contains("Pulling $testImage")
                expectThat(testImage.isPulled).isTrue()

                expectThat(testImage.pull()).isSuccessful()
                BACKGROUND.expectLogged.contains("Pulling $testImage")
            }
        }

        @Nested
        inner class Remove {

            @BeforeEach
            fun setUp() {
                DockerTestImageProvider("hello-world").pull()
            }

            @Test
            fun `should remove image and log`() {
                expectThat(testImage.remove()).isSuccessful()
                BACKGROUND.expectLogged.contains("Removing $testImage")
                expectThat(testImage.isPulled).isFalse()

                expectThat(testImage.remove()).isFailed()
                BACKGROUND.expectLogged.contains("Removing $testImage ${Symbols.Negative.ansiRemoved} no such image")
            }
        }

        @AfterAll
        fun tearDown() {
            testImage.remove(force = true)
        }
    }
}
