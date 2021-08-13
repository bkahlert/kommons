package koodies.docker

import koodies.docker.DockerSearchCommandLine.DockerSearchResult
import koodies.docker.TestImages.BusyBox
import koodies.test.junit.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan

class DockerSearchCommandLineTest {

    @DockerRequiring @Test
    fun `should search`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(DockerSearchCommandLine.search(
            "busybox",
            stars = 4,
            automated = false,
            official = true,
            limit = 2
        )).any {
            image.isEqualTo(BusyBox)
            description.contains("Busybox").contains("base").contains("image")
            stars.isGreaterThan(1000)
            isOfficial()
            not { isAutomated() }
        }
    }
}

val Assertion.Builder<DockerSearchResult>.image: DescribeableBuilder<DockerImage>
    get() = get("image") { image }
val Assertion.Builder<DockerSearchResult>.description: DescribeableBuilder<String>
    get() = get("description") { description }
val Assertion.Builder<DockerSearchResult>.stars: DescribeableBuilder<Int>
    get() = get("starCount") { stars }

fun Assertion.Builder<DockerSearchResult>.isOfficial() =
    assert("is official") {
        when (it.official) {
            true -> pass()
            else -> fail("not official")
        }
    }

fun Assertion.Builder<DockerSearchResult>.isAutomated() =
    assert("is automated") {
        when (it.automated) {
            true -> pass()
            else -> fail("not official")
        }
    }
