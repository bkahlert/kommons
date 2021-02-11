package koodies.docker

import koodies.concurrent.process.CommandLine
import koodies.docker.DockerContainerName.Companion.toContainerName
import koodies.io.path.asPath
import koodies.shell.toHereDoc
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerRunCommandLineTest {

    companion object {
        val DOCKER_RUN_COMMAND: DockerRunCommandLine = DockerRunCommandLine(
            DockerImage { "repo" / "name" tag "tag" },
            DockerRunCommandLineOptions(
                detached = true,
                name = "container-name".toContainerName(),
                publish = listOf("8080:6060", "1234-1236:1234-1236/tcp"),
                privileged = true,
                autoCleanup = true,
                workingDirectory = "/c".asContainerPath(),
                interactive = true,
                pseudoTerminal = true,
                mounts = MountOptions(
                    MountOption(source = "/a/b".asHostPath(), target = "/c/d".asContainerPath()),
                    MountOption("bind", "/e/f/../g".asHostPath(), "//h".asContainerPath()),
                ),
                custom = listOf("custom1", "custom2")
            ),
            CommandLine(
                redirects = emptyList(),
                environment = mapOf("key1" to "value1", "KEY2" to "VALUE 2"),
                workingDirectory = Path.of("/a"),
                command = "work",
                arguments = listOf(
                    "/etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/",
                    "-arg1", "--argument", "2", listOf("heredoc 1", "-heredoc-line-2").toHereDoc("HEREDOC").toString(),
                    "/a/b/c", "/c/d/e", "/e/f/../g/h", "/e/g/h", "/h/i",
                    "arg=/a/b/c", "arg=/c/d/e", "arg=/e/f/../g/h", "arg=/e/g/h", "arg=/h/i",
                    "a/b/c", "c/d/e", "e/f/../g/h", "e/g/h", "h/i",
                    "arg=a/b/c", "arg=c/d/e", "arg=e/f/../g/h", "arg=e/g/h", "arg=h/i",
                    "b/c", "d/e", "f/../g/h", "g/h", "i",
                    "arg=b/c", "arg=d/e", "arg=f/../g/h", "arg=g/h", "arg=i",
                ),
            ),
        )
    }

    @Test
    fun `should build valid docker run`() {
        expectThat(DOCKER_RUN_COMMAND).toStringIsEqualTo("""
                docker \
                run \
                --env \
                key1=value1 \
                --env \
                "KEY2=VALUE 2" \
                -d \
                --name \
                container-name \
                -p \
                8080:6060 \
                -p \
                1234-1236:1234-1236/tcp \
                --privileged \
                -w \
                /c \
                --rm \
                -i \
                -t \
                --mount \
                type=bind,source=/a/b,target=/c/d \
                --mount \
                type=bind,source=/e/f/../g,target=/h \
                custom1 \
                custom2 \
                repo/name:tag \
                work \
                /etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/ \
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
                arg=/h/i \
                a/b/c \
                c/d/e \
                e/f/../g/h \
                e/g/h \
                h/i \
                arg=a/b/c \
                arg=c/d/e \
                arg=e/f/../g/h \
                arg=e/g/h \
                arg=h/i \
                d/c \
                d/e \
                f/../g/h \
                g/h \
                i \
                arg=d/c \
                arg=d/e \
                arg=f/../g/h \
                arg=g/h \
                arg=i
                """.trimIndent())
    }

    @Nested
    inner class WorkingDirectory {

        @Nested
        inner class DockerOptionSpecified {

            @Test
            fun `should ignore guest command line`() {
                expectThat(dockerCommandLine(
                    optionsWorkingDir = "/a",
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h").options.workingDirectory).isEqualTo("/a".asContainerPath())
            }

            @Test
            fun `should take as is - even if re-mappable`() {
                expectThat(dockerCommandLine(
                    optionsWorkingDir = "/a/b/1",
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h").options.workingDirectory).isEqualTo("/a/b/1".asContainerPath())
            }

            @Test
            fun `should use re-use guest working dir`() {
                expectThat(dockerCommandLine(
                    optionsWorkingDir = "/a",
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h").workingDirectory).isEqualTo("/some/where".asPath())
            }
        }

        @Nested
        inner class NoDockerOptionSpecified {

            @Test
            fun `should use guest working dir if re-mappable`() {
                expectThat(dockerCommandLine(
                    optionsWorkingDir = null,
                    guestWorkingDir = "/a/b/1",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h").options.workingDirectory).isEqualTo("/c/d/1".asContainerPath())
            }

            @Test
            fun `should ignore guest working dir if not re-mappable`() {
                expectThat(dockerCommandLine(
                    optionsWorkingDir = null,
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h").options.workingDirectory).isNull()
            }

            @Test
            fun `should use re-use guest working dir`() {
                expectThat(dockerCommandLine(
                    optionsWorkingDir = null,
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h").workingDirectory).isEqualTo("/some/where".asPath())
            }
        }

        private fun dockerCommandLine(optionsWorkingDir: String?, guestWorkingDir: String, vararg mounts: Pair<String, String>) = DockerRunCommandLine(
            dockerImage { "repo" / "name" tag "tag" },
            dockerOptions(optionsWorkingDir, *mounts),
            guestCommandLine(guestWorkingDir)
        )

        private fun dockerOptions(optionsWorkingDir: String?, vararg mounts: Pair<String, String>): DockerRunCommandLineOptions {
            return DockerRunCommandLineOptions(
                name = "container-name".toContainerName(),
                workingDirectory = optionsWorkingDir?.asContainerPath(),
                mounts = MountOptions(
                    *mounts.map {
                        MountOption(source = it.first.asHostPath(), target = it.second.asContainerPath())
                    }.toTypedArray()
                ),
            )
        }

        private fun guestCommandLine(guestWorkingDir: String) = CommandLine(
            redirects = emptyList(),
            environment = mapOf("key1" to "value1", "KEY2" to "VALUE 2"),
            workingDirectory = guestWorkingDir.asPath(),
            command = "work",
            arguments = listOf("-arg1", "--argument"),
        )
    }
}
