package koodies.docker

import koodies.builder.Init
import koodies.docker.DockerRunCommandLine.Companion.CommandContext
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.DockerRunCommandLine.Options.Companion.OptionsContext
import koodies.docker.MountOptionContext.Type.bind
import koodies.docker.TestImages.Ubuntu
import koodies.exec.CommandLine
import koodies.exec.Exec
import koodies.exec.Executable
import koodies.exec.IO
import koodies.exec.IO.Output
import koodies.exec.Process.State.Exited
import koodies.exec.Process.State.Exited.Succeeded
import koodies.exec.ansiRemoved
import koodies.exec.commandLine
import koodies.exec.exitCode
import koodies.exec.hasState
import koodies.exec.io
import koodies.exec.output
import koodies.exec.state
import koodies.io.copyTo
import koodies.io.copyToDirectory
import koodies.io.path.asPath
import koodies.io.path.deleteRecursively
import koodies.io.path.pathString
import koodies.io.tempDir
import koodies.runtime.onExit
import koodies.shell.ShellScript
import koodies.test.BuilderFixture
import koodies.test.HtmlFixture
import koodies.test.Slow
import koodies.test.Smoke
import koodies.test.expecting
import koodies.test.test
import koodies.test.testEach
import koodies.test.tests
import koodies.test.toStringContains
import koodies.test.toStringIsEqualTo
import koodies.text.matchesCurlyPattern
import koodies.time.seconds
import koodies.tracing.TestSpan
import koodies.tracing.rendering.RendererProvider
import koodies.tracing.rendering.TeePrinter
import koodies.tracing.rendering.capturing
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.isNotSameInstanceAs
import strikt.assertions.isSameInstanceAs
import java.nio.file.Path
import kotlin.time.measureTime

@Slow
class DockerRunCommandLineTest {

    @Test
    fun `should build command line`() {
        val dockerRunCommand = DockerRunCommandLine(init)
        expectThat(dockerRunCommand).isEqualTo(result)
    }

    @Test
    fun `should build same format for no sub builders and empty sub builders`() {
        val commandBuiltWithNoBuilders = DockerRunCommandLine { image by dockerImage }
        val commandBuiltWithEmptyBuilders = DockerRunCommandLine {
            image by dockerImage
            options { name by commandBuiltWithNoBuilders.options.name?.name }
            commandLine { }
        }

        expectThat(commandBuiltWithNoBuilders).isEqualTo(commandBuiltWithEmptyBuilders)
    }

    @TestFactory
    fun `should set have auto cleanup and interactive options on by default`() =
        test(DockerRunCommandLine { image by dockerImage }.toCommandLine().arguments) {
            asserting { contains("--rm") }
            asserting { contains("--interactive") }
        }

    @Test
    fun `should build valid docker run`() {
        expectThat(result).toStringIsEqualTo("""
                'docker' \
                'run' \
                '-d' \
                '--name' \
                'container-name' \
                '-p' \
                '8080:6060' \
                '-p' \
                '1234-1236:1234-1236/tcp' \
                '--privileged' \
                '--workdir' \
                '/c' \
                '--rm' \
                '--interactive' \
                '--tty' \
                '--mount' \
                'type=bind,source=/a/b,target=/c/d' \
                '--mount' \
                'type=bind,source=/e/f/../g,target=/h' \
                'custom1' \
                'custom2' \
                'repo/name:tag' \
                'work' \
                '/etc/dnf/dnf.conf:s/check=1/check=0/' \
                '-arg1' \
                '--argument' \
                '2' \
                '/a/b/c' \
                '/c/d/e' \
                '/e/f/../g/h' \
                '/e/g/h' \
                '/h/i' \
                'arg=/a/b/c' \
                'arg=/c/d/e' \
                'arg=/e/f/../g/h' \
                'arg=/e/g/h' \
                'arg=/h/i' \
                'a/b/c' \
                'c/d/e' \
                'e/f/../g/h' \
                'e/g/h' \
                'h/i' \
                'arg=a/b/c' \
                'arg=c/d/e' \
                'arg=e/f/../g/h' \
                'arg=e/g/h' \
                'arg=h/i' \
                'b/c' \
                'd/e' \
                'f/../g/h' \
                'g/h' \
                'i' \
                'arg=b/c' \
                'arg=d/e' \
                'arg=f/../g/h' \
                'arg=g/h' \
                'arg=i'
                """.trimIndent())
    }

    @Nested
    inner class FallbackName {

        @Test
        fun `should return same options if name is set`() {
            val options = Options { name { "container-name" } }
            expectThat(options.withFallbackName("fallback-name"))
                .isSameInstanceAs(options)
                .get { name }.isNotNull().get { name }.isEqualTo("container-name")
        }

        @Test
        fun `should return new options with fallback name if name is not set`() {
            val options = Options {}
            expectThat(options.withFallbackName("fallback-name"))
                .isNotSameInstanceAs(options)
                .get { name }.isNotNull().get { name }.isEqualTo("fallback-name")
        }
    }

    @Nested
    inner class Content {

        private val commandLine = DockerRunCommandLine(DockerImage { "repo" / "name" tag "tag" }, Options(), CommandLine("printenv", "TEST_PROP"))

        @Test
        fun `should provide content`() {
            expectThat(commandLine.content).matchesCurlyPattern("docker run {} repo/name:tag printenv TEST_PROP")
        }
    }

    @DockerRequiring
    @Nested
    inner class DockerizedExecutor {

        @Smoke @TestFactory
        fun `should exec dockerized`() = testEach(
            CommandLine("printenv", "HOME"),
            ShellScript {
                shebang
                !"printenv | grep HOME | perl -pe 's/.*?HOME=//'"
            },
        ) { executable ->
            expecting { executable.dockerized(Ubuntu).exec.logging() } that {
                io.output.ansiRemoved.isEqualTo("/root")
            }
        }

        @TestFactory
        fun `should have success state on exit code 0`() = testEach(
            CommandLine("ls", "/root"),
            ShellScript {
                shebang
                !CommandLine("ls", "/root")
            },
        ) { executable ->
            expecting { executable.dockerized(Ubuntu).exec.logging() } that {
                state.isA<Succeeded>().exitCode.isEqualTo(0)
            }
        }

        @TestFactory
        fun `should have failed state on exit code other than 0`() = testEach(
            CommandLine("ls", "invalid"),
            ShellScript {
                shebang
                !CommandLine("ls", "invalid")
            },
        ) { executable ->
            expecting { executable.dockerized(Ubuntu).exec.logging() } that {
                state.isA<Exited.Failed>().exitCode.isEqualTo(2)
            }
        }

        @TestFactory
        fun `should apply env`() = testEach(
            CommandLine("printenv", "TEST_PROP"),
            ShellScript {
                shebang
                !CommandLine("printenv", "TEST_PROP")
            },
        ) { executable ->
            expecting { executable.dockerized(Ubuntu).exec.env("TEST_PROP", "TEST_VALUE").logging() } that {
                io.output.ansiRemoved.isEqualTo("TEST_VALUE")
            }
        }
    }

    @Nested
    inner class WorkingDirectory {

        @Nested
        inner class WorkingDirectoryMapping {

            private val tempDir = tempDir().also { onExit { it.deleteRecursively() } }
            private val workDir = tempDir.resolve("work")
            private val htmlFile = workDir.resolve("files/sample.html").also { HtmlFixture.copyTo(it) }

            private val commandLine = CommandLine("cat", htmlFile.pathString)

            @DockerRequiring @TestFactory
            fun `should apply working directory`() = testEach(
                commandLine,
                ShellScript {
                    shebang
                    !commandLine
                },
            ) { executable ->
                expecting {
                    executable.dockerized(Ubuntu) {
                        mounts {
                            tempDir mountAt "/host"
                        }
                    }.exec.logging(workDir)
                } that {
                    commandLine.toStringContains("'/host/work/files/sample.html'")
                    io.output.ansiRemoved.isEqualTo(HtmlFixture.text)
                }
            }
        }

        @Nested
        inner class SpecifiedWorkingDirectoryOption {

            @Test
            fun `should apply working directory option`() {
                val dockerCommandLine = dockerRunCommandLine("/a").toCommandLine("/some/where")

                expectThat(dockerCommandLine.shellCommand)
                    .isEqualTo("$d '--workdir' '/a' $ri $m=/a/b,target=/c/d' $m=/e/f/../g,target=/h' $args '/c/d/1'")
            }

            @Test
            fun `should take as is - even if re-mappable`() {
                val dockerCommandLine = dockerRunCommandLine("/a/b/1").toCommandLine("/some/where")
                expectThat(dockerCommandLine.shellCommand)
                    .isEqualTo("$d '--workdir' '/a/b/1' $ri $m=/a/b,target=/c/d' $m=/e/f/../g,target=/h' $args '/c/d/1'")
            }
        }

        @Nested
        inner class NotSpecifiedWorkingDirectoryOption {

            @Test
            fun `should use guest working dir if re-mappable`() {
                val dockerCommandLine = dockerRunCommandLine().toCommandLine("/a/b/1")
                expectThat(dockerCommandLine.shellCommand)
                    .isEqualTo("$d $ri $m=/a/b,target=/c/d' $m=/e/f/../g,target=/h' $args '/c/d/1'")
            }

            @Test
            fun `should ignore guest working dir if not re-mappable`() {
                val dockerCommandLine = dockerRunCommandLine().toCommandLine("/some/where")
                expectThat(dockerCommandLine.shellCommand)
                    .isEqualTo("$d $ri $m=/a/b,target=/c/d' $m=/e/f/../g,target=/h' $args '/c/d/1'")
            }
        }

        private val d = "'docker' 'run' '--name' 'container-name'"
        private val ri = "'--rm' '--interactive'"
        private val m = "'--mount' 'type=bind,source"
        private val args = "'repo/name:tag' 'command' '-arg1' '--argument'"

        private fun dockerRunCommandLine(workingDirectoryOption: String? = null) = DockerRunCommandLine(
            DockerImage { "repo" / "name" tag "tag" },
            dockerOptions(workingDirectoryOption, "/a/b" to "/c/d", "/e/f/../g" to "//h"),
            CommandLine("command", "-arg1", "--argument", "/a/b/1")
        )

        private fun dockerOptions(workingDirectoryOption: String?, vararg mounts: Pair<String, String>): Options =
            Options(
                name = DockerContainer.from("container-name"),
                workingDirectory = workingDirectoryOption?.asContainerPath(),
                mounts = MountOptions(
                    *mounts.map {
                        MountOption(source = it.first.asHostPath(), target = it.second.asContainerPath())
                    }.toTypedArray()
                ),
            )

        private fun DockerRunCommandLine.toCommandLine(hostWorkDir: String) =
            toCommandLine(emptyMap(), hostWorkDir.asPath())
    }

    @Nested
    inner class EntryPoint {

        @Nested
        inner class DockerizingCommandLine {

            @Test
            fun `should use entrypoint if set`() {
                val commandLine = CommandLine("printenv", "HOME")
                expecting { commandLine.dockerized(Ubuntu) { entrypoint { "echo" } }.exec.logging() } that {
                    io.contains(Output typed "HOME")
                }
            }

            @Test
            fun `should use command if not set`() {
                val commandLine = CommandLine("printenv", "HOME")
                expecting { commandLine.dockerized(Ubuntu).exec.logging() } that {
                    io.contains(Output typed "/root")
                }
            }
        }

        @Nested
        inner class DockerizingShellScript {

            @TestFactory
            @Suppress("NonAsciiCharacters")
            fun `should always use ⧸bin⧸sh`() = tests {
                val script = object : ShellScript(null, "printenv HOME") {
                    override fun toCommandLine(environment: Map<String, String>, workingDirectory: Path?, transform: (String) -> String): CommandLine {
                        val originalCommandLine = super.toCommandLine(environment, workingDirectory, transform)
                        return CommandLine("/any/interpreter", originalCommandLine.arguments.last())
                    }
                }
                expecting { script.dockerized(Ubuntu) { entrypoint { "bullshit" } }.exec.logging() } that { io.contains(Output typed "/root") }
                expecting { script.dockerized(Ubuntu).exec.logging() } that { io.contains(Output typed "/root") }
            }
        }
    }

    @DockerRequiring @TestFactory
    fun `should exec using specified image`() = testEach<Executable<Exec>.() -> DockerExec>(
        { dockerized(Ubuntu).exec() },
        { dockerized { "ubuntu" }.exec() },
        { with(Ubuntu) { dockerized.exec() } },
    ) { execVariant ->
        expecting {
            CommandLine("printenv", "HOME").execVariant()
        } that {
            io.contains(Output typed "/root")
        }
    }

    @DockerRequiring @TestFactory
    fun TestSpan.`should exec logging using specified image`() = testEach<Executable<Exec>.(RendererProvider) -> DockerExec>(
        { dockerized(Ubuntu).exec.logging(renderer = it) },
        { dockerized { "ubuntu" }.exec.logging(renderer = it) },
        { with(Ubuntu) { dockerized.exec.logging(renderer = it) } },
    ) { execVariant ->
        expecting {
            capturing { capturingPrinter ->
                CommandLine("printenv", "HOME").execVariant {
                    it(copy(printer = TeePrinter(printer, capturingPrinter)))
                }
            }
        } that {
            matchesCurlyPattern("""
                ╭──╴file://{}.sh
                │
                │   /root
                │
                ╰──╴✔︎
            """.trimIndent())
        }
    }

    @DockerRequiring @TestFactory
    fun `should exec processing using specified image`() = testEach<Executable<Exec>.(MutableList<IO>) -> DockerExec>(
        { dockerized(Ubuntu).exec.processing { _, process -> process { io -> it.add(io) } } },
        { dockerized { "ubuntu" }.exec.processing { _, process -> process { io -> it.add(io) } } },
        { with(Ubuntu) { dockerized.exec.processing { _, process -> process { io -> it.add(io) } } } },
    ) { execVariant ->
        expecting {
            mutableListOf<IO>().also {
                CommandLine("printenv", "HOME").execVariant(it)
            }
        } that {
            contains(Output typed "/root")
        }
    }

    @DockerRequiring @TestFactory
    fun `should exec synchronously`() = testEach<Executable<Exec>.() -> DockerExec>(
        { dockerized(Ubuntu).exec() },
        { dockerized { "ubuntu" }.exec() },
        { with(Ubuntu) { dockerized.exec() } },
    ) { execVariant ->
        var exec: DockerExec? = null
        expecting { measureTime { exec = CommandLine("sleep", "2").execVariant() } } that { isGreaterThanOrEqualTo(2.seconds) }
        { exec } asserting { get { invoke() }.isNotNull().hasState<Exited>() }
    }

    @TestFactory
    fun `should exec asynchronously`() = testEach<Executable<Exec>.() -> DockerExec>(
        { dockerized(Ubuntu).exec.async() },
        { dockerized { "ubuntu" }.exec.async() },
        { with(Ubuntu) { dockerized.exec.async() } },
    ) { execVariant ->
        expecting { measureTime { CommandLine("sleep", "2").execVariant() } } that { isLessThan(2.seconds) }
    }

    @Nested
    inner class WithOptions {

        private val tempDir = tempDir()
            .also { HtmlFixture.copyToDirectory(it) }
            .also { onExit { it.deleteRecursively() } }

        private val optionsInit: Init<OptionsContext> = {
            workingDirectory { "/tmp".asContainerPath() }
            mounts {
                tempDir mountAs bind at "/tmp/host"
            }
        }

        private val options = Options(optionsInit)

        @DockerRequiring @TestFactory
        fun `should exec using specified options`() = testEach<Executable<Exec>.() -> DockerExec>(
            { dockerized(Ubuntu, options).exec() },
            { dockerized(Ubuntu) { optionsInit() }.exec() },

            { dockerized(options) { "ubuntu" }.exec() },
            { dockerized({ "ubuntu" }) { optionsInit() }.exec() },

            { with(Ubuntu) { dockerized(options).exec() } },
            { with(Ubuntu) { dockerized { optionsInit() } }.exec() },
        ) { execVariant ->
            expecting {
                CommandLine("cat", "host/${HtmlFixture.name}").execVariant()
            } that {
                io.output.ansiRemoved.isEqualTo(HtmlFixture.text)
            }
        }
    }

    companion object : BuilderFixture<Init<CommandContext>, DockerRunCommandLine>(
        DockerRunCommandLine,
        {
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
                command { "work" }
                arguments {
                    +"/etc/dnf/dnf.conf:s/check=1/check=0/"
                    +"-arg1"
                    +"--argument" + "2"
                    +"/a/b/c" + "/c/d/e" + "/e/f/../g/h" + "/e/g/h" + "/h/i"
                    +"arg=/a/b/c" + "arg=/c/d/e" + "arg=/e/f/../g/h" + "arg=/e/g/h" + "arg=/h/i"
                    +"a/b/c" + "c/d/e" + "e/f/../g/h" + "e/g/h" + "h/i"
                    +"arg=a/b/c" + "arg=c/d/e" + "arg=e/f/../g/h" + "arg=e/g/h" + "arg=h/i"
                    +"b/c" + "d/e" + "f/../g/h" + "g/h" + "i"
                    +"arg=b/c" + "arg=d/e" + "arg=f/../g/h" + "arg=g/h" + "arg=i"
                }
            }
        },
        DockerRunCommandLine(
            DockerImage { "repo" / "name" tag "tag" },
            Options(
                detached = true,
                name = DockerContainer.from("container-name"),
                publish = listOf("8080:6060", "1234-1236:1234-1236/tcp"),
                privileged = true,
                autoCleanup = true,
                workingDirectory = "/c".asContainerPath(),
                interactive = true,
                pseudoTerminal = true,
                mounts = MountOptions(
                    MountOption(source = "/a/b".asHostPath(), target = "/c/d".asContainerPath()),
                    MountOption("/e/f/../g".asHostPath(), "//h".asContainerPath(), "bind"),
                ),
                custom = listOf("custom1", "custom2")
            ),
            CommandLine(
                "work",
                "/etc/dnf/dnf.conf:s/check=1/check=0/",
                "-arg1", "--argument", "2",
                "/a/b/c", "/c/d/e", "/e/f/../g/h", "/e/g/h", "/h/i",
                "arg=/a/b/c", "arg=/c/d/e", "arg=/e/f/../g/h", "arg=/e/g/h", "arg=/h/i",
                "a/b/c", "c/d/e", "e/f/../g/h", "e/g/h", "h/i",
                "arg=a/b/c", "arg=c/d/e", "arg=e/f/../g/h", "arg=e/g/h", "arg=h/i",
                "b/c", "d/e", "f/../g/h", "g/h", "i",
                "arg=b/c", "arg=d/e", "arg=f/../g/h", "arg=g/h", "arg=i",
            ),
        ),
    ) {
        private val dockerImage = Docker.images { "repo" / "name" tag "tag" }
    }
}
