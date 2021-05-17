package koodies.docker

import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.expectLogged
import koodies.test.IdeaWorkaroundTest
import koodies.test.testEach
import koodies.test.tests
import koodies.test.toStringIsEqualTo
import koodies.text.ANSI.ansiRemoved
import koodies.text.Semantics.Symbols
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class DockerImageTest {

    private val imageInit: DockerImageInit = { "repo" / "name" }
    private val officialImageInit: DockerImageInit = { "repo" }
    private val imageWithTagInit: DockerImageInit = { "repo" / "name" tag "my-tag" }
    private val imageWithDigestInit: DockerImageInit = { "repo" / "name" digest "sha256:abc" }

    @TestFactory
    fun `should format and parse image instance `() = listOf(
        imageInit to "repo/name",
        officialImageInit to "repo",
        imageWithTagInit to "repo/name:my-tag",
        imageWithDigestInit to "repo/name@sha256:abc",
    ).testEach { (init, string) ->
        expecting { DockerImage(init) } that { toStringIsEqualTo(string) }
        expecting { DockerImage.parse(string) } that { isEqualTo(DockerImage(init)) }
        expecting { DockerImage { string } } that { isEqualTo(DockerImage(init)) }
    }

    @TestFactory
    fun `should not include specifier if specified`() = testEach(
        imageInit,
        imageWithTagInit,
        imageWithDigestInit,
    ) {
        expecting { DockerImage(it).toString(includeSpecifier = false) } that { isEqualTo("repo/name") }
    }

    @TestFactory
    fun `should accept valid repositories and paths`() = testEach(
        "repo",
        "repo123",
        "repo.123",
        "repo_123",
        "repo-123",
    ) {
        expecting { DockerImage { it / it } } that { toStringIsEqualTo("$it/$it") }
        expecting { DockerImage { it } } that { toStringIsEqualTo(it) }
    }

    @TestFactory
    fun `should throw on illegal repository`() = testEach("", "REPO", "r'e'p'o") { repo ->
        expectThrows<IllegalArgumentException> { DockerImage { repo / "path" } }
        expectThrows<IllegalArgumentException> { DockerImage { "$repo/path" } }
    }

    @TestFactory
    fun `should throw on illegal path`() = testEach("", "PATH", "p'a't'h") { path ->
        expectThrows<IllegalArgumentException> { DockerImage { "repo" / path } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo/$path" } }
    }

    @TestFactory
    fun `should throw on illegal specifier`() = testEach("") { specifier ->
        expectThrows<IllegalArgumentException> { DockerImage { "repo" / "path" tag specifier } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo" / "path" digest specifier } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo/path:$specifier" } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo/path@$specifier" } }
    }

    @TestFactory
    fun `should equal`() = tests {
        DockerImage { "repo/path" } all {
            asserting { isEqualTo(DockerImage.parse("repo/path")) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), null, null)) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), "tag", null)) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), null, "digest")) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), "tag", "digest")) }

            asserting { not { isEqualTo(DockerImage("repo", listOf("other-path"), null, null)) } }
            asserting { not { isEqualTo(DockerImage("other-repo", listOf("path"), null, null)) } }
        }
        DockerImage { "repo/path:tag" } all {
            asserting { isEqualTo(DockerImage.parse("repo/path:tag")) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), null, null)) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), "tag", null)) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), null, "digest")) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), "tag", "digest")) }

            asserting { not { isEqualTo(DockerImage("repo", listOf("path"), "other-tag", null)) } }
        }
        DockerImage { "repo/path@digest" } all {
            asserting { isEqualTo(DockerImage.parse("repo/path@digest")) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), null, null)) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), "tag", null)) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), null, "digest")) }
            asserting { isEqualTo(DockerImage("repo", listOf("path"), "tag", "digest")) }

            asserting { not { isEqualTo(DockerImage("repo", listOf("path"), null, "other-digest")) } }
        }
    }

    @Nested
    inner class DockerCommands {

        @Nested
        inner class ListImages {

            @ImageTest @IdeaWorkaroundTest
            fun TestImage.`should list images and log`() = whilePulled { testImage ->
                expectThat(DockerImage.list(logger = BACKGROUND)).contains(testImage)
                BACKGROUND.expectLogged.contains("Listing images")
            }

            @ImageTest @IdeaWorkaroundTest
            fun TestImage.`should list existing image and log`() = whilePulled { testImage ->
                expectThat(testImage.list(logger = BACKGROUND)).contains(testImage)
                BACKGROUND.expectLogged.contains("Listing $testImage images")

                testImage.remove()
                expectThat(testImage.list(logger = BACKGROUND)).isEmpty()
                BACKGROUND.expectLogged.contains("Listing $testImage images")
            }
        }

        @ImageTest @IdeaWorkaroundTest
        fun TestImage.`should provide tags on Docker Hub`() {
            expectThat(tagsOnDockerHub) {
                contains("latest")
                contains("linux")
            }
        }

        @ImageTest @IdeaWorkaroundTest
        fun TestImage.`should check if is pulled and log`() = whilePulled { testImage ->
            expectThat(testImage).isPulled()
            BACKGROUND.expectLogged.contains("Checking if $testImage is pulled")

            testImage.remove()
            expectThat(testImage).not { isPulled() }
            BACKGROUND.expectLogged.contains("Checking if $testImage is pulled")
        }

        @ImageTest @IdeaWorkaroundTest
        fun TestImage.`should pull image and log`() = whileRemoved { testImage ->
            expectThat(testImage.pull(logger = BACKGROUND)).isSuccessful()
            BACKGROUND.expectLogged.contains("Pulling $testImage")
            expectThat(testImage.isPulled).isTrue()

            expectThat(testImage.pull(logger = BACKGROUND)).isSuccessful()
            BACKGROUND.expectLogged.contains("Pulling $testImage")
        }

        @ImageTest @IdeaWorkaroundTest
        fun TestImage.`should remove image and log`() = whilePulled { testImage ->
            expectThat(testImage.remove(logger = BACKGROUND)).isSuccessful()
            BACKGROUND.expectLogged.contains("Removing $testImage")
            expectThat(testImage.isPulled).isFalse()

            expectThat(testImage.remove(logger = BACKGROUND)).isFailed()
            BACKGROUND.expectLogged.contains("Removing $testImage ${Symbols.Negative.ansiRemoved} no such image")
        }
    }
}

fun Assertion.Builder<DockerImage>.isPulled() =
    assert("is pulled") {
        when (with(it) { BACKGROUND.isPulled }) {
            true -> pass()
            else -> fail("$it is not pulled")
        }
    }
