package koodies.docker

import koodies.builder.Init
import koodies.collections.head
import koodies.collections.tail
import koodies.concurrent.process.output
import koodies.debug.trace
import koodies.docker.DockerSearchCommandLine.Companion.CommandContext
import koodies.docker.DockerSearchCommandLine.Options
import koodies.test.BuilderFixture
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Disabled
@Execution(CONCURRENT)
class DockerSearchCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerSearchCommandLine = DockerSearchCommandLine(init)
        expectThat(dockerSearchCommandLine).isEqualTo(result)
    }

    @Test
    fun `should search`() {
        val dockerSearchCommandLine = DockerSearchCommandLine {
            options {
                stars by 4
                isAutomated { off }
                isOfficial { on }
                format { "{{.Name}}" }
                limit by 10
            }
            term { "busybox" }
        }
        val o = with(dockerSearchCommandLine) { exec().io.ansiRemoved }
        o.lines().map { repo ->
            val size = 100
            val page = 1
            // TODO improve busybox to run ShellScripts
            val url = """https://registry.hub.docker.com/api/content/v1/repositories/public/library/${repo}/tags?page=${page}&page_size=${size}"""
            Docker.busybox({ "dwdraju" / "alpine-curl-jq" digest "sha256:5f6561fff50ab16cba4a9da5c72a2278082bcfdca0f72a9769d7e78bdc5eb954" },
                """curl '$url' 2>/dev/null | jq -r '.results[].name' | sort"""
            ).output().lines().map { tag -> DockerImage(repo.split("/").head, repo.split("/").tail, tag = tag) }

//            Docker.busybox({ "dwdraju" / "alpine-curl-jq" digest "sha256:5f6561fff50ab16cba4a9da5c72a2278082bcfdca0f72a9769d7e78bdc5eb954" },
//                """curl '$url' 2>/dev/null | jq -r '.results[].name' | sort"""
//            ).process(mode = Synchronous) {
//                { tag: IO ->
//                    DockerImage(repo.split("/").head, repo.split("/").tail, tag = tag.unformatted)
//                }
//            }
        }.trace
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
            options = Options(filters = listOf("key" to "value", "stars" to "4", "is-automated" to "false", "is-official" to "true"),
                format = null,
                limit = 10),
            term = "busybox",
        ),
    )
}
