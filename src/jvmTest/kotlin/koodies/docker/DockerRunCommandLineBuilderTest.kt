package koodies.docker

import koodies.docker.DockerRunCommandLineTest.Companion.DOCKER_RUN_COMMAND
import koodies.docker.MountOptionContext.Type.bind
import koodies.shell.HereDocBuilder.hereDoc
import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class DockerRunCommandLineBuilderTest {

    private val dockerImage = Docker.image { "repo" / "name" tag "tag" }

    @Test
    fun `should build valid docker run`() {
        val dockerRunCommand = DockerRunCommandLine {
            image(dockerImage)
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
                    +hereDoc(label = "HEREDOC") {
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
        expectThat(dockerRunCommand).toStringIsEqualTo(DOCKER_RUN_COMMAND.toString())
    }

    @Test
    fun `should build same format for no sub builders and empty sub builders`() {
        val commandBuiltWithNoBuilders = DockerRunCommandLine { image(dockerImage) }
        val commandBuiltWithEmptyBuilders = DockerRunCommandLine {
            image(dockerImage)
            options { }
            commandLine { }
        }

        expectThat(commandBuiltWithNoBuilders).isEqualTo(commandBuiltWithEmptyBuilders)
    }

    @Test
    fun `should set auto cleanup as default`() {
        expectThat(DockerRunCommandLine { image(dockerImage) }.arguments).contains("--rm")
    }

    @Test
    fun `should set interactive as default`() {
        expectThat(DockerRunCommandLine { image(dockerImage) }.arguments).contains("-i")
    }
}
