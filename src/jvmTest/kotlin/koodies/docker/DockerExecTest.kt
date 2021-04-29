package koodies.docker

import koodies.builder.Init
import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.UserInput.enter
import koodies.concurrent.process.out
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.DockerRunCommandLine.Options.Companion.OptionsContext
import koodies.docker.MountOptionContext.Type.bind
import koodies.docker.TestImages.BusyBox
import koodies.docker.TestImages.Ubuntu
import koodies.exec.CommandLine
import koodies.exec.ExecTerminationCallback
import koodies.exec.Executable
import koodies.exec.Process.ExitState
import koodies.exec.Process.ProcessState.Running
import koodies.exec.Process.ProcessState.Terminated
import koodies.exec.alive
import koodies.exec.ansiRemoved
import koodies.exec.exitCode
import koodies.exec.exitState
import koodies.exec.hasState
import koodies.exec.io
import koodies.exec.out
import koodies.exec.started
import koodies.io.path.Locations
import koodies.io.path.deleteRecursively
import koodies.io.path.tempDir
import koodies.logging.InMemoryLogger
import koodies.logging.expectLogged
import koodies.runtime.onExit
import koodies.shell.ShellScript
import koodies.test.HtmlFile
import koodies.test.Slow
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.test.copyToDirectory
import koodies.test.output.TestLogger
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.poll
import koodies.times
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.size
import java.nio.file.Path
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class DockerExecTest {

    @DockerRequiring([BusyBox::class]) @Test
    fun `should override toString`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dockerExec = createExec(uniqueId, "echo", "test")
        expectThat(dockerExec).toStringMatchesCurlyPattern("DockerExec { container = {} exec = {} }")
    }

    @DockerRequiring([BusyBox::class]) @Test
    fun `should start docker`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dockerExec = createExec(uniqueId, "echo", "test")
        dockerExec.waitForOutputOrFail(
            "Process terminated without logging: ${dockerExec.io.ansiRemoved}.",
            "Did not log \"test\" output within 8 seconds.") {
            any { it is OUT && it.unformatted == "test" }
        }
    }

    @Nested
    inner class Lifecycle {

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and pass arguments`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dockerExec = createExec(uniqueId, "echo", "test")
            dockerExec.waitForOutputOrFail(
                "Process terminated without logging: ${dockerExec.io.ansiRemoved}.",
                "Did not log \"test\" output within 8 seconds.") {
                any { it is OUT && it.unformatted == "test" }
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and process input`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            var entered = false
            val dockerExec = createExec(uniqueId, "/bin/sh", "-c", "echo 'test 1' && cat") {
                if (it !is IO.META) {
                    if (!entered) {
                        entered = true
                        enter("test 2")
                        inputStream.close()
                    }
                }
            }

            dockerExec.waitForOutputOrFail("Did not log self-induced \"test\" output within 8 seconds.") {
                filterIsInstance<OUT>().contains(OUT typed "test 1") and contains(OUT typed "test 2")
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and process output`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dockerExec = createExec(uniqueId, "echo", "Hello\nWorld")

            dockerExec.waitForOutputOrFail("Did not log any output within 8 seconds.") {
                filterIsInstance<OUT>().contains(OUT typed "Hello") and contains(OUT typed "World")
            }
        }

        @DockerRequiring([BusyBox::class]) @Smoke @Test
        fun `should start docker and process output produced by own input`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            var times = 0
            val dockerExec = createExec(uniqueId, "/bin/sh", "-c", "echo 'test 1' && cat") {
                if (it !is IO.META) {
                    if (times < 3) {
                        times++
                        enter("test ${it.unformatted.last().toString().toInt() * 2}")
                    } else {
                        inputStream.close()
                    }
                }
            }

            dockerExec.waitForOutputOrFail("Did not log self-produced \"test\" output within 8 seconds.") {
                filterIsInstance<OUT>()
                    .contains(OUT typed "test 1")
                    .and(contains(OUT typed "test 2"))
                    .and(contains(OUT typed "test 4"))
                    .and(contains(OUT typed "test 8"))
            }
        }

        @Nested
        inner class IsRunning {

            private fun Path.unprocessedProcess(
                uniqueId: UniqueId,
                command: String,
                vararg args: String,
                callback: ExecTerminationCallback? = null,
            ): DockerExec = DockerExec.NATIVE_DOCKER_EXEC_WRAPPED.toProcess(DockerRunCommandLine(
                image = BusyBox,
                options = Options(name = DockerContainer(uniqueId.simplified)),
                commandLine = CommandLine(this, command, *args)), callback)

            @DockerRequiring([BusyBox::class]) @Test
            fun `should return false on not yet started container container`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val unprocessedProcess = unprocessedProcess(uniqueId, "sleep", "10")
                val subject = unprocessedProcess.started
                expectThat(subject).isFalse()
                expectThat(unprocessedProcess.alive).isFalse()
            }

            @DockerRequiring([BusyBox::class], mode = ThanksForCleaningUp) @Test
            fun `should return true on running container`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val runningProcess = unprocessedProcess(uniqueId, "sleep", "10")
                runningProcess.start().waitForCondition("Did not start in time.") { state is Running }
                expectThat(runningProcess.alive).isTrue()
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should return false on completed container`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val completedProcess = unprocessedProcess(uniqueId, "sleep", "1")
                completedProcess.start().apply {
                    waitForCondition("Did not start in time.") { state is Running }
                    waitForCondition("Did not complete in time.") { state is Terminated }
                }
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should stop running container`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val runningProcess = unprocessedProcess(uniqueId, "sleep", "10")
                runningProcess.start().waitForCondition("Did not start in time.") { state is Running }

                val passed = measureTime { runningProcess.stop(1.seconds) }
                expectThat(passed).isLessThan(4.seconds)
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should kill running container`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val runningProcess = unprocessedProcess(uniqueId, "sleep", "10")
                runningProcess.start().apply {
                    waitForCondition("Did not start in time.") { state is Running }
                }

                val passed = measureTime { runningProcess.kill() }
                expectThat(passed).isLessThan(2.seconds)
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should call callback on termination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var calledBack = false
                val runningProcess = unprocessedProcess(uniqueId, "exit", "0") { calledBack = true }
                runningProcess.start().waitForCondition("Did not call back.") { calledBack }
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should have failed state on non 0 exit code`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dockerExec = createExec(uniqueId, "invalid")
            expectThat(dockerExec).hasState<ExitState.Failure> { exitCode.isEqualTo(127) }
        }

        @Slow @DockerRequiring([BusyBox::class]) @Test
        fun `should remove docker container after completion`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val dockerExec = createExec(uniqueId, "echo", "was alive")

            dockerExec.apply {
                expectThat(io.ansiRemoved).contains("was alive")
                expectThat(container).hasState<DockerContainer.State.NotExistent>()
            }
        }
    }

    @Slow @DockerRequiring([BusyBox::class]) @Test
    fun `should not produce incorrect empty lines`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        var killed = false
        val output = synchronizedListOf<IO>()
        createExec(uniqueId, "/bin/sh", "-c", """
                while true; do
                ${20.times { "echo \"looping\"" }.joinToString("; ")}
                sleep 1
                done
            """.trimIndent()) {
            if (it !is IO.META) {
                output.add(it)
                if (output.size > 100 && !killed) {
                    killed = true
                    kill()
                }
            }
        }
        expectThat(output).size.isGreaterThanOrEqualTo(100)
        expectThat(output.filter { it.isBlank() }.size).isLessThan(25)
    }


    @DockerRequiring
    @Nested
    inner class DockerizedExecutor {

        @Smoke @Test
        fun InMemoryLogger.`should exec command line`() {
            val exec = CommandLine(Locations.Temp, "printenv", "HOME").exec.dockerized(Ubuntu).logging(this)
            expectThat(exec.io.out.ansiRemoved).isEqualTo("/root")
        }

        @Smoke @Test
        fun InMemoryLogger.`should exec shell script`() {
            val exec = ShellScript {
                shebang
                !"printenv | grep HOME"
            }.exec.dockerized(Ubuntu).logging(this)
            expectThat(exec.io.out.ansiRemoved).isEqualTo("HOME=/root")
        }

        @Test
        fun InMemoryLogger.`should have success state on exit code 0`() {
            val exec = CommandLine("ls", "/root").exec.dockerized(Ubuntu).logging(this)
            expectThat(exec).exitState.isA<ExitState.Success>().exitCode.isEqualTo(0)
        }

        @Test
        fun InMemoryLogger.`should have failure state on exit code other than 0`() {
            val exec = CommandLine("ls", "invalid").exec.dockerized(Ubuntu).logging(this)
            expectThat(exec).exitState.isA<ExitState.Failure>().exitCode.isEqualTo(2)
        }

        @Test
        fun InMemoryLogger.`should add apply command line provided env`() {
            val exec = CommandLine(mapOf("TEST_PROP" to "TEST_VALUE"), Locations.Temp, "printenv", "TEST_PROP").exec.dockerized(Ubuntu).logging(this)
            expectThat(exec.io.out.ansiRemoved).isEqualTo("TEST_VALUE")
        }
        
        @TestFactory
        fun `should exec using specified image`() = testEach<Executable.() -> DockerExec>(
            { exec.dockerized(Ubuntu)() },
            { exec.dockerized(Ubuntu).exec() },

            { exec.dockerized { "ubuntu" }() },
            { exec.dockerized { "ubuntu" }.exec() },

            { with(Ubuntu) { exec.dockerized() } },
            { with(Ubuntu) { exec.dockerized.exec() } },
        ) { execVariant ->
            expecting {
                CommandLine(Locations.Temp, "printenv", "HOME").execVariant()
            } that {
                io.contains(OUT typed "/root")
            }
        }

        @Execution(SAME_THREAD) @TestFactory
        fun TestLogger.`should exec logging using specified image`() = testEach<Executable.() -> DockerExec>(
            { exec.dockerized(Ubuntu).logging(this@`should exec logging using specified image`) },
            { exec.dockerized { "ubuntu" }.logging(this@`should exec logging using specified image`) },
            { with(Ubuntu) { exec.dockerized.logging(this@`should exec logging using specified image`) } },
            { with(Ubuntu) { logging.dockerized() } },
        ) { execVariant ->
            expecting {
                clear()
                CommandLine(Locations.Temp, "printenv", "HOME").execVariant()
            } that {
                expectLogged.toStringMatchesCurlyPattern("""
                · Executing docker run --name {} --rm --interactive ubuntu printenv HOME
                · /root
                · Process {} terminated {}
                ✔︎
            """.trimIndent())
            }
        }

        @TestFactory
        fun `should exec processing using specified image`() = testEach<Executable.(MutableList<IO>) -> DockerExec>(
            { exec.dockerized(Ubuntu).processing { io -> it.add(io) } },
            { exec.dockerized { "ubuntu" }.processing { io -> it.add(io) } },
            { with(Ubuntu) { exec.dockerized.processing { io -> it.add(io) } } },
        ) { execVariant ->
            expecting {
                mutableListOf<IO>().also {
                    CommandLine(Locations.Temp, "printenv", "HOME").execVariant(it)
                }
            } that {
                contains(OUT typed "/root")
            }
        }

        @Execution(CONCURRENT) @TestFactory
        fun `should exec synchronously`() = testEach<Executable.() -> DockerExec>(
            { exec.dockerized(Ubuntu)() },
            { exec.dockerized(Ubuntu).exec() },

            { exec.dockerized { "ubuntu" }() },
            { exec.dockerized { "ubuntu" }.exec() },

            { with(Ubuntu) { exec.dockerized() } },
            { with(Ubuntu) { exec.dockerized.exec() } },
        ) { execVariant ->
            var exec: DockerExec? = null
            expecting { measureTime { exec = CommandLine(Locations.Temp, "sleep", "2").execVariant() } } that { isGreaterThanOrEqualTo(2.seconds) }
            { exec } asserting { get { invoke() }.isNotNull().hasState<Terminated>() }
        }

        @Execution(CONCURRENT) @TestFactory
        fun `should exec asynchronously`() = testEach<Executable.() -> DockerExec>(
            { exec.dockerized(Ubuntu).async() },
            { exec.dockerized(Ubuntu).async.exec() },
            { exec.async.dockerized(Ubuntu)() },
            { exec.async.dockerized(Ubuntu).exec() },

            { exec.dockerized { "ubuntu" }.async() },
            { exec.dockerized { "ubuntu" }.async.exec() },
            { exec.async.dockerized { "ubuntu" }() },
            { exec.async.dockerized { "ubuntu" }.exec() },

            { with(Ubuntu) { exec.dockerized.async() } },
            { with(Ubuntu) { exec.dockerized.async.exec() } },
            { with(Ubuntu) { exec.async.dockerized() } },
            { with(Ubuntu) { exec.async.dockerized.exec() } },
        ) { execVariant ->
            expecting { measureTime { CommandLine(Locations.Temp, "sleep", "2").execVariant() } } that { isLessThan(2.seconds) }
        }

        @Nested
        inner class WithOptions {

            private val tempDir = tempDir()
                .also { HtmlFile.copyToDirectory(it) }
                .also { onExit { it.deleteRecursively() } }

            private val optionsInit: Init<OptionsContext> = {
                workingDirectory { "/tmp".asContainerPath() }
                mounts {
                    tempDir mountAs bind at "/tmp/host"
                }
            }

            private val options = Options(optionsInit)

            @Execution(CONCURRENT) @TestFactory
            fun `should exec using specified options`() = testEach<Executable.() -> DockerExec>(
                { exec.dockerized(Ubuntu, options)() },
                { exec.dockerized(Ubuntu) { optionsInit() }() },
                { exec.dockerized(Ubuntu, options).exec() },
                { exec.dockerized(Ubuntu) { optionsInit() }.exec() },
                { exec.dockerized(Ubuntu, options).logging() },
                { exec.dockerized(Ubuntu) { optionsInit() }.logging() },

                { exec.dockerized(options) { "ubuntu" }() },
                { exec.dockerized({ "ubuntu" }) { optionsInit() }() },
                { exec.dockerized(options) { "ubuntu" }.exec() },
                { exec.dockerized({ "ubuntu" }) { optionsInit() }.exec() },
                { exec.dockerized(options) { "ubuntu" }.logging() },
                { exec.dockerized({ "ubuntu" }) { optionsInit() }.logging() },

                { with(Ubuntu) { exec.dockerized(options)() } },
                { with(Ubuntu) { exec.dockerized { optionsInit() } }() },
                { with(Ubuntu) { exec.dockerized(options).exec() } },
                { with(Ubuntu) { exec.dockerized { optionsInit() } }.exec() },
                { with(Ubuntu) { exec.dockerized(options).logging() } },
                { with(Ubuntu) { exec.dockerized { optionsInit() } }.logging() },
            ) { execVariant ->
                expecting {
                    CommandLine("cat", "host/${HtmlFile.name}").execVariant()
                } that {
                    io.out.ansiRemoved.isEqualTo(HtmlFile.text)
                }
            }
        }
    }
}


private fun Path.createExec(
    uniqueId: UniqueId,
    command: String,
    vararg args: String,
    processor: Processor<DockerExec> = noopProcessor(),
): DockerExec =
    docker({
        image by Ubuntu
        options { name { uniqueId.simplified } }
        commandLine by CommandLine(this@createExec, command, *args)
    }, processor = processor)

private fun DockerExec.waitForCondition(
    errorMessage: String,
    test: DockerExec.() -> Boolean,
) {
    poll {
        test()
    }.every(100.milliseconds).forAtMost(4.seconds) {
        fail(errorMessage)
    }
}

private fun DockerExec.waitForOutputOrFail(
    errorMessage: String,
    stillRunningErrorMessage: String = errorMessage,
    test: List<IO>.() -> Boolean,
) {
    poll {
        io.toList().test()
    }.every(100.milliseconds).forAtMost(8.seconds) {
        if (alive) fail(stillRunningErrorMessage)
        fail(errorMessage)
    }
}
