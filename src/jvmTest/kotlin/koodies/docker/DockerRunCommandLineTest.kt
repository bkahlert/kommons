package koodies.docker

import koodies.concurrent.process.CommandLine
import koodies.docker.DockerContainerName.Companion.toContainerName
import koodies.docker.DockerRunCommandLine.Companion.DockerRunCommandContext
import koodies.docker.MountOptionContext.Type.bind
import koodies.io.path.asPath
import koodies.shell.HereDocBuilder
import koodies.shell.toHereDoc
import koodies.test.BuilderFixture.Companion.fixture
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNull
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerRunCommandLineTest {

    @Test
    fun `should build valid docker run`() {
        expectThat(result).toStringIsEqualTo("""
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
                val dockerCommandLine = dockerCommandLine(
                    optionsWorkingDir = "/a",
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h")
                expectThat(dockerCommandLine.options.workingDirectory).isEqualTo("/a".asContainerPath())
            }

            @Test
            fun `should take as is - even if re-mappable`() {
                val dockerCommandLine = dockerCommandLine(
                    optionsWorkingDir = "/a/b/1",
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h")
                expectThat(dockerCommandLine.options.workingDirectory).isEqualTo("/a/b/1".asContainerPath())
            }

            @Test
            fun `should use re-use guest working dir`() {
                val dockerCommandLine = dockerCommandLine(
                    optionsWorkingDir = "/a",
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h")
                expectThat(dockerCommandLine.workingDirectory).isEqualTo("/some/where".asPath())
            }
        }

        @Nested
        inner class NoDockerOptionSpecified {

            @Test
            fun `should use guest working dir if re-mappable`() {
                val dockerCommandLine = dockerCommandLine(
                    optionsWorkingDir = null,
                    guestWorkingDir = "/a/b/1",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h")
                expectThat(dockerCommandLine.options.workingDirectory).isEqualTo("/c/d/1".asContainerPath())
            }

            @Test
            fun `should ignore guest working dir if not re-mappable`() {
                val dockerCommandLine = dockerCommandLine(
                    optionsWorkingDir = null,
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h")
                expectThat(dockerCommandLine.options.workingDirectory).isNull()
            }

            @Test
            fun `should use re-use guest working dir`() {
                val dockerCommandLine = dockerCommandLine(
                    optionsWorkingDir = null,
                    guestWorkingDir = "/some/where",
                    "/a/b" to "/c/d",
                    "/e/f/../g" to "//h")
                expectThat(dockerCommandLine.workingDirectory).isEqualTo("/some/where".asPath())
            }
        }

        private fun dockerCommandLine(optionsWorkingDir: String?, guestWorkingDir: String, vararg mounts: Pair<String, String>) = DockerRunCommandLine(
            DockerImage { "repo" / "name" tag "tag" },
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


    @Nested
    inner class Builder {

        private val dockerImage = Docker.image { "repo" / "name" tag "tag" }

        @Test
        fun `should build valid docker run`() {
            val dockerRunCommand = DockerRunCommandLine(init)
            expectThat(dockerRunCommand).isEqualTo(result)
        }

        @Test
        fun `should build same format for no sub builders and empty sub builders`() {
            val commandBuiltWithNoBuilders = DockerRunCommandLine { image by dockerImage }
            val commandBuiltWithEmptyBuilders = DockerRunCommandLine {
                image by dockerImage
                options { }
                commandLine { }
            }

            expectThat(commandBuiltWithNoBuilders).isEqualTo(commandBuiltWithEmptyBuilders)
        }

        @Test
        fun `should set auto cleanup as default`() {
            expectThat(DockerRunCommandLine { image by dockerImage }.arguments).contains("--rm")
        }

        @Test
        fun `should set interactive as default`() {
            expectThat(DockerRunCommandLine { image by dockerImage }.arguments).contains("-i")
        }
    }

}


private val init: DockerRunCommandContext.() -> Unit = {
    image { "repo" / "name" tag "tag" }
    options {
        detached { on }
        publish {
            +"8080:6060"
            +"1234-1236:1234-1236/tcp"
        }
        name { "container-name" }
        privileged { on }
        autoCleanup { on }
        workingDirectory { "/c".asContainerPath() }
        interactive { on }
        pseudoTerminal { on }
        mounts {
            "/a/b" mountAt "/c/d"
            "/e/f/../g" mountAs bind at "//h"
        }
        custom {
            +"custom1"
            +"custom2"
        }
    }
    commandLine {
        redirects {}
        environment {
            "key1" to "value1"
            "KEY2" to "VALUE 2"
        }
        workingDirectory { "/a".asHostPath() }
        command { "work" }
        arguments {
            +"/etc/dnf/dnf.conf:s/gpgcheck=1/gpgcheck=0/"
            +"-arg1"
            +"--argument" + "2"
            +HereDocBuilder.hereDoc(label = "HEREDOC") {
                +"heredoc 1"
                +"-heredoc-line-2"
            }
            +"/a/b/c" + "/c/d/e" + "/e/f/../g/h" + "/e/g/h" + "/h/i"
            +"arg=/a/b/c" + "arg=/c/d/e" + "arg=/e/f/../g/h" + "arg=/e/g/h" + "arg=/h/i"
            +"a/b/c" + "c/d/e" + "e/f/../g/h" + "e/g/h" + "h/i"
            +"arg=a/b/c" + "arg=c/d/e" + "arg=e/f/../g/h" + "arg=e/g/h" + "arg=h/i"
            +"b/c" + "d/e" + "f/../g/h" + "g/h" + "i"
            +"arg=b/c" + "arg=d/e" + "arg=f/../g/h" + "arg=g/h" + "arg=i"
        }
    }
}

private val result: DockerRunCommandLine = DockerRunCommandLine(
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

val Docker.DockerRunCommandLineBuilderExpectation get() = DockerRunCommandLine fixture (init to result)
