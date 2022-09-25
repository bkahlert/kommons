package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons_deprecated.text.Semantics.Symbols
import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons_deprecated.tracing.TestSpanScope
import io.kotest.assertions.throwables.shouldThrow
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
    fun `should accept valid repositories and paths`() = testEach(
        "repo",
        "repo123",
        "repo.123",
        "repo_123",
        "repo-123",
    ) {
        DockerImage { it / it }.toString() shouldBe "$it/$it"
        DockerImage { it }.toString() shouldBe it
    }

    @TestFactory
    fun `should throw on illegal repository`() = testEach("", "REPO", "r'e'p'o") {
        shouldThrow<IllegalArgumentException> { DockerImage { it / "path" } }
        shouldThrow<IllegalArgumentException> { DockerImage { "$it/path" } }
    }

    @TestFactory
    fun `should throw on illegal path`() = testEach("", "PATH", "p'a't'h") {
        shouldThrow<IllegalArgumentException> { DockerImage { "repo" / it } }
        shouldThrow<IllegalArgumentException> { DockerImage { "repo/$it" } }
    }

    @Test
    fun `should throw on illegal specifier`() = testAll {
        shouldThrow<IllegalArgumentException> { DockerImage { "repo" / "path" tag "" } }
        shouldThrow<IllegalArgumentException> { DockerImage { "repo" / "path" digest "" } }
        shouldThrow<IllegalArgumentException> { DockerImage { "repo/path:" } }
        shouldThrow<IllegalArgumentException> { DockerImage { "repo/path@" } }
    }

    @Test
    fun equals() = testAll {
        DockerImage { "repo/path" } should {
            it shouldBe DockerImage.parse("repo/path")
            it shouldBe DockerImage("repo", listOf("path"), null, null)
            it shouldBe DockerImage("repo", listOf("path"), "tag", null)
            it shouldBe DockerImage("repo", listOf("path"), null, "alg:hash")
            it shouldBe DockerImage("repo", listOf("path"), "tag", "alg:hash")

            it shouldNotBe DockerImage("repo", listOf("other-path"), null, null)
            it shouldNotBe DockerImage("other-repo", listOf("path"), null, null)
        }
        DockerImage { "repo/path:tag" } should {
            it shouldBe DockerImage.parse("repo/path:tag")
            it shouldBe DockerImage("repo", listOf("path"), null, null)
            it shouldBe DockerImage("repo", listOf("path"), "tag", null)
            it shouldBe DockerImage("repo", listOf("path"), null, "alg:hash")
            it shouldBe DockerImage("repo", listOf("path"), "tag", "alg:hash")

            it shouldNotBe DockerImage("repo", listOf("path"), "other-tag", null)
        }
        DockerImage { "repo/path@alg:hash" } should {
            it shouldBe DockerImage.parse("repo/path@alg:hash")
            it shouldBe DockerImage("repo", listOf("path"), null, null)
            it shouldBe DockerImage("repo", listOf("path"), "tag", null)
            it shouldBe DockerImage("repo", listOf("path"), null, "alg:hash")
            it shouldBe DockerImage("repo", listOf("path"), "tag", "alg:hash")

            it shouldNotBe DockerImage("repo", listOf("path"), null, "other:hash")
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
            tagsOnDockerHub should {
                it shouldContain "latest"
                it shouldContain "linux"
            }
        }

        @ImageTest
        fun TestImage.`should check if is pulled and log`(testSpanScope: TestSpanScope) = whilePulled { testImage ->
            testImage.isPulled shouldBe true
            testSpanScope.rendered() shouldContain "Listing $testImage images ✔︎"

            testImage.remove()
            testImage.isPulled shouldBe false
            testSpanScope.rendered() shouldContain "Listing $testImage images ✔︎"
        }

        @ImageTest
        fun TestImage.`should pull image and log`(testSpanScope: TestSpanScope) = whileRemoved { testImage ->
            testImage.pull().successful shouldBe true
            testSpanScope.rendered() shouldContain "Pulling $testImage image ✔︎"
            testImage.isPulled shouldBe true

            testImage.pull().successful shouldBe true
            testSpanScope.rendered() shouldContain "Pulling $testImage image ✔︎"
        }

        @ImageTest
        fun TestImage.`should remove image and log`(testSpanScope: TestSpanScope) = whilePulled { testImage ->
            testImage.remove().successful shouldBe true
            testSpanScope.rendered() shouldContain "Removing $testImage ✔︎"
            testImage.isPulled shouldBe false

            testImage.remove().successful shouldBe false
            testSpanScope.rendered() shouldContain "Removing $testImage ${Symbols.Negative.ansiRemoved} no such image"
        }
    }
}
