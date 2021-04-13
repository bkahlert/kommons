package koodies.docker

import koodies.concurrent.Executable
import koodies.concurrent.execute
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.ERR
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.Process.ExitState.Failure
import koodies.concurrent.process.Processors
import koodies.concurrent.process.containsDump
import koodies.concurrent.process.hasState
import koodies.concurrent.process.io
import koodies.concurrent.process.merged
import koodies.concurrent.process.out
import koodies.concurrent.process.output
import koodies.debug.CapturedOutput
import koodies.docker.MountOptionContext.Type.bind
import koodies.logging.InMemoryLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.expectLogged
import koodies.logging.expectThatLogged
import koodies.requireNotBlank
import koodies.shell.ShellExecutable
import koodies.test.HtmlFile
import koodies.test.SystemIoExclusive
import koodies.test.UniqueId
import koodies.test.copyToDirectory
import koodies.test.toStringContainsAll
import koodies.test.withTempDir
import koodies.text.ANSI.ansiRemoved
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isNotEqualTo
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.size

@Execution(SAME_THREAD)
class DockerizedExecutionTest {

    private val shellExecutable: ShellExecutable = CommandLine("printenv")

    private val testImage = DockerImage { official("busybox") }

    @SystemIoExclusive
    @Test
    fun InMemoryLogger.`should run container without options and log`(capturedOutput: CapturedOutput) {
        shellExecutable.executeDockerized(testImage, null)
        expectThatLogged().contains("Executing dockerized")
        expectThat(capturedOutput).isEmpty()
    }

    @SystemIoExclusive
    @Test
    fun `should run command line without options and print`(capturedOutput: CapturedOutput) {
        shellExecutable.executeDockerized(testImage, null)
        expectThat(capturedOutput).contains("Executing dockerized")
    }

    @SystemIoExclusive
    @Test
    fun InMemoryLogger.`should run command line using docker image in receiver and provided logger`(capturedOutput: CapturedOutput) {
        val logger = this
        with(testImage) {
            (shellExecutable as Executable).execute(logger)
        }
        expectThatLogged().contains("Executing dockerized")
        expectThat(capturedOutput).isEmpty()
    }

    @SystemIoExclusive
    @Test
    fun `should run command line with docker image in receiver and default logger`(capturedOutput: CapturedOutput) {
        with(testImage) {
            (shellExecutable as CommandLine).execute()
        }
        BACKGROUND.expectLogged.contains("Executing dockerized")
        expectThat(capturedOutput).isEmpty()
    }

    @Test
    fun InMemoryLogger.`should record IO`() {
        val process: DockerProcess = shellExecutable.executeDockerized(TestImages.Ubuntu, null)
        expectThat(process.io.merged.ansiRemoved).matchesCurlyPattern("""
                Executing docker run --name {} --rm -i ${TestImages.Ubuntu} printenv
                {{}}
                Process {} terminated successfully at {}
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should return Docker process`() {
        val process: DockerProcess = shellExecutable.executeDockerized(testImage, null)
        expectThat(process).isA<DockerProcess>()
    }

    @Test
    fun InMemoryLogger.`should run in actual docker container`() {
        val dockerOutput = shellExecutable.executeDockerized(testImage, null).output().requireNotBlank()
        val hostOutput = shellExecutable.execute { null }.output().requireNotBlank()
        expectThat(dockerOutput).isNotEqualTo(hostOutput)
    }

    @Test
    fun InMemoryLogger.`should start implicitly`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val process: DockerProcess = shellExecutable.executeDockerized(testImage, null)
        expectThat(process.started).isTrue()
    }

    @Test
    fun InMemoryLogger.`should start`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val process: DockerProcess = shellExecutable.executeDockerized(testImage, null)
        process.start()
        expectThat(process.started).isTrue()
    }

    @Nested
    inner class WithDockerOptions {

        @Test
        fun InMemoryLogger.`should apply docker options`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            shellExecutable.executeDockerized(testImage) {
                dockerOptions { workingDirectory by "/tmp".asContainerPath() }; null
            }
            expectThatLogged().contains("-w /tmp")
        }

        @Test
        fun InMemoryLogger.`should mount`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val hostDir = this
            val htmlFile = HtmlFile.copyToDirectory(hostDir)
            val process = CommandLine("cat", "host/${htmlFile.fileName}").executeDockerized(testImage) {
                dockerOptions {
                    workingDirectory { "/tmp".asContainerPath() }
                    mounts {
                        hostDir mountAs bind at "/tmp/host"
                    }
                }
                null
            }
            expectThat(process.io.out.merged.ansiRemoved).isEqualTo(HtmlFile.text)
            expectLogged.toStringContainsAll(*HtmlFile.text.lines().toTypedArray())
        }
    }

    @Test
    fun InMemoryLogger.`should apply execution options`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        shellExecutable.executeDockerized(testImage) {
            executionOptions { summary("docker") }; null
        }
        expectThatLogged().matchesCurlyPattern("""
                ╭──╴{}
                │   
                │   docker ➜ {} ➜ {} ✔︎
                │
                ╰──╴✔︎
            """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val processed = mutableListOf<IO>()
        CommandLine("/bin/sh", "-c", "echo OUT; >&2 echo ERR; printenv").executeDockerized(testImage) {
            { io -> processed.add(io) }
        }
        expectThat(processed) {
            contains(OUT typed "OUT", ERR typed "ERR")
            size.isGreaterThan(3)
        }
    }

    @Test
    fun InMemoryLogger.`should not throw on unexpected exit value`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectCatching { CommandLine("echo OUT; >&2 echo ERR").execute { Processors.noopProcessor() } }
            .isSuccess().hasState<Failure> { io<IO>().containsDump(containedStrings = emptyArray()) }
    }
}
