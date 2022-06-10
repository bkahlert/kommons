package com.bkahlert.kommons.docker

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.junit.testing
import com.bkahlert.kommons.test.junit.testingAll
import com.bkahlert.kommons.test.test
import com.bkahlert.kommons.text.Semantics.Symbols
import com.bkahlert.kommons.tracing.TestSpanScope
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class DockerImageTest {

    private val imageInit: DockerImageInit = { "repo" / "name" }
    private val officialImageInit: DockerImageInit = { "repo" }
    private val imageWithTagInit: DockerImageInit = { "repo" / "name" tag "my-tag" }
    private val imageWithDigestInit: DockerImageInit = { "repo" / "name" digest "sha256:abc" }

    @TestFactory
    fun `should format and parse image instance `() = testEach(
        imageInit to "repo/name",
        officialImageInit to "repo",
        imageWithTagInit to "repo/name:my-tag",
        imageWithDigestInit to "repo/name@sha256:abc",
    ) { (init, string) ->
        DockerImage(init).toString() shouldBe string
        DockerImage.parse(string) shouldBe DockerImage(init)
        DockerImage { string } shouldBe DockerImage(init)
    }

    @TestFactory
    fun `should not include specifier if specified`() = testEach(
        imageInit,
        imageWithTagInit,
        imageWithDigestInit,
    ) {
        DockerImage(it).toString(includeSpecifier = false) shouldBe "repo/name"
    }

    @TestFactory
    fun `should accept valid repositories and paths`() = testingAll(
        "repo",
        "repo123",
        "repo.123",
        "repo_123",
        "repo-123",
    ) {
        expecting { DockerImage { subject / subject } } it { toString() shouldBe "$subject/$subject" }
        expecting { DockerImage { subject } } it { toString() shouldBe subject }
    }

    @TestFactory
    fun `should throw on illegal repository`() = testingAll("", "REPO", "r'e'p'o") {
        expectThrows<IllegalArgumentException> { DockerImage { subject / "path" } }
        expectThrows<IllegalArgumentException> { DockerImage { "$subject/path" } }
    }

    @TestFactory
    fun `should throw on illegal path`() = testingAll("", "PATH", "p'a't'h") {
        expectThrows<IllegalArgumentException> { DockerImage { "repo" / subject } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo/$subject" } }
    }

    @TestFactory
    fun `should throw on illegal specifier`() = testing {
        expectThrows<IllegalArgumentException> { DockerImage { "repo" / "path" tag "" } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo" / "path" digest "" } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo/path:" } }
        expectThrows<IllegalArgumentException> { DockerImage { "repo/path@" } }
    }

    @Test
    fun equals() = test {
        DockerImage { "repo/path" } should {
            it shouldBe DockerImage.parse("repo/path")
            it shouldBe DockerImage("repo", listOf("path"), null, null)
            it shouldBe DockerImage("repo", listOf("path"), "tag", null)
            it shouldBe DockerImage("repo", listOf("path"), null, "digest")
            it shouldBe DockerImage("repo", listOf("path"), "tag", "digest")

            it shouldNotBe DockerImage("repo", listOf("other-path"), null, null)
            it shouldNotBe DockerImage("other-repo", listOf("path"), null, null)
        }
        DockerImage { "repo/path:tag" } should {
            it shouldBe DockerImage.parse("repo/path:tag")
            it shouldBe DockerImage("repo", listOf("path"), null, null)
            it shouldBe DockerImage("repo", listOf("path"), "tag", null)
            it shouldBe DockerImage("repo", listOf("path"), null, "digest")
            it shouldBe DockerImage("repo", listOf("path"), "tag", "digest")

            it shouldNotBe DockerImage("repo", listOf("path"), "other-tag", null)
        }
        DockerImage { "repo/path@digest" } should {
            it shouldBe DockerImage.parse("repo/path@digest")
            it shouldBe DockerImage("repo", listOf("path"), null, null)
            it shouldBe DockerImage("repo", listOf("path"), "tag", null)
            it shouldBe DockerImage("repo", listOf("path"), null, "digest")
            it shouldBe DockerImage("repo", listOf("path"), "tag", "digest")

            it shouldNotBe DockerImage("repo", listOf("path"), null, "other-digest")
        }
    }

    @Nested
    inner class DockerCommands {

        @Nested
        inner class ListImages {

            @ImageTest
            fun TestImage.`should list images and log`(testSpanScope: TestSpanScope) = whilePulled { testImage ->
                DockerImage.list() shouldContain testImage
                testSpanScope.rendered() shouldContain "Listing images ✔︎"
            }

            @ImageTest
            fun TestImage.`should list existing image and log`(testSpanScope: TestSpanScope) = whilePulled { testImage ->
                testImage.list() shouldContain testImage
                testSpanScope.rendered() shouldContain "Listing $testImage images ✔︎"

                testImage.remove()
                testImage.list().shouldBeEmpty()
                testSpanScope.rendered() shouldContain "Listing $testImage images ✔︎"
            }
        }

        @ImageTest
        fun TestImage.`should provide tags on Docker Hub`() {
            expectThat(tagsOnDockerHub) {
                contains("latest")
                contains("linux")
            }
        }

        @ImageTest
        fun TestImage.`should check if is pulled and log`(testSpanScope: TestSpanScope) = whilePulled { testImage ->
            expectThat(testImage).isPulled()
            testSpanScope.expectThatRendered().contains("Listing $testImage images ✔︎")

            testImage.remove()
            expectThat(testImage).not { isPulled() }
            testSpanScope.expectThatRendered().contains("Listing $testImage images ✔︎")
        }

        @ImageTest
        fun TestImage.`should pull image and log`(testSpanScope: TestSpanScope) = whileRemoved { testImage ->
            expectThat(testImage.pull()).isSuccessful()
            testSpanScope.expectThatRendered().contains("Pulling $testImage image ✔︎")
            expectThat(testImage.isPulled).isTrue()

            expectThat(testImage.pull()).isSuccessful()
            testSpanScope.expectThatRendered().contains("Pulling $testImage image ✔︎")
        }

        @ImageTest
        fun TestImage.`should remove image and log`(testSpanScope: TestSpanScope) = whilePulled { testImage ->
            expectThat(testImage.remove()).isSuccessful()
            testSpanScope.expectThatRendered().contains("Removing $testImage ✔︎")
            expectThat(testImage.isPulled).isFalse()

            expectThat(testImage.remove()).isFailed()
            testSpanScope.expectThatRendered().contains("Removing $testImage ${Symbols.Negative.ansiRemoved} no such image")
        }
    }
}

fun Assertion.Builder<DockerImage>.isPulled() =
    assert("is pulled") {
        when (it.isPulled) {
            true -> pass()
            else -> fail("$it is not pulled")
        }
    }
