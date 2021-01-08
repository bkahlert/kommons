package koodies.docker

import koodies.concurrent.process.CommandLine
import koodies.docker.DockerContainerName.Companion.toContainerName
import koodies.shell.toHereDoc
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerCommandLineTest {

    companion object {
        val DOCKER_RUN_COMMAND = DockerCommandLine(
            DockerImage.imageWithTag(DockerRepository.of("repo", "name"), Tag("tag")),
            DockerCommandLineOptions(
                name = "container-name".toContainerName(),
                privileged = true,
                autoCleanup = true,
                interactive = true,
                pseudoTerminal = true,
                mounts = MountOptions(
                    MountOption(source = "/a/b".asHostPath(), target = "/c/d".asContainerPath()),
                    MountOption("bind", "/e/f/../g".asHostPath(), "//h".asContainerPath()),
                ),
            ),
            CommandLine(
                redirects = emptyList(),
                environment = mapOf("key1" to "value1", "KEY2" to "VALUE 2"),
                workingDirectory = Path.of("/some/where"),
                command = "work",
                arguments = listOf(
                    "-arg1", "--argument", "2", listOf("heredoc 1", "-heredoc-line-2").toHereDoc("HEREDOC").toString(),
                    "/a/b/c", "/c/d/e", "/e/f/../g/h", "/e/g/h", "/h/i",
                    "arg=/a/b/c", "arg=/c/d/e", "arg=/e/f/../g/h", "arg=/e/g/h", "arg=/h/i",
                ),
            ),
        )
    }

    @Test
    fun `should build valid docker run`() {
        expect {
            that(DOCKER_RUN_COMMAND.workingDirectory).isEqualTo(Path.of("/some/where"))
            that(DOCKER_RUN_COMMAND).toStringIsEqualTo("""
                docker \
                run \
                --env \
                key1=value1 \
                --env \
                "KEY2=VALUE 2" \
                --name \
                container-name \
                --privileged \
                --rm \
                -i \
                -t \
                --mount \
                type=bind,source=/a/b,target=/c/d \
                --mount \
                type=bind,source=/e/f/../g,target=/h \
                repo/name:tag \
                work \
                -arg1 \
                --argument \
                2 \
                <<HEREDOC
                heredoc 1
                -heredoc-line-2
                HEREDOC \
                /c/d/c \
                /c/d/e \
                /h/h \
                /h/h \
                /h/i \
                arg=/c/d/c \
                arg=/c/d/e \
                arg=/h/h \
                arg=/h/h \
                arg=/h/i
                """.trimIndent())
        }
    }
}
