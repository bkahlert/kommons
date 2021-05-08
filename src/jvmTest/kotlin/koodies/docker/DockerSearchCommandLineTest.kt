package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerSearchCommandLine.Companion.CommandContext
import koodies.docker.DockerSearchCommandLine.DockerSeachResult
import koodies.docker.DockerSearchCommandLine.Options
import koodies.docker.TestImages.BusyBox
import koodies.test.BuilderFixture
import koodies.test.UniqueId
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

    @Test
    fun `should build command line`() {
        val dockerSearchCommandLine = DockerSearchCommandLine(init)
        expectThat(dockerSearchCommandLine).isEqualTo(result)
    }

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

    companion object : BuilderFixture<Init<CommandContext>, DockerSearchCommandLine>(
        DockerSearchCommandLine,
        {
            options {
                filter { "key" to "value" }
                stars by 4
                isAutomated { off }
                isOfficial { on }
                limit by 10
            }
            term { "busybox" }
        },
        DockerSearchCommandLine(
            options = Options(listOf("key" to "value", "stars" to "4", "is-automated" to "false", "is-official" to "true"), 10),
            term = "busybox",
        ),
    )
}

val Assertion.Builder<DockerSeachResult>.image: DescribeableBuilder<DockerImage>
    get() = get("image") { image }
val Assertion.Builder<DockerSeachResult>.description: DescribeableBuilder<String>
    get() = get("description") { description }
val Assertion.Builder<DockerSeachResult>.stars: DescribeableBuilder<Int>
    get() = get("starCount") { stars }

fun Assertion.Builder<DockerSeachResult>.isOfficial() =
    assert("is official") {
        when (it.official) {
            true -> pass()
            else -> fail("not official")
        }
    }

fun Assertion.Builder<DockerSeachResult>.isAutomated() =
    assert("is automated") {
        when (it.automated) {
            true -> pass()
            else -> fail("not official")
        }
    }
