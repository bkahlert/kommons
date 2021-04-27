package koodies.docker

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.IO
import koodies.concurrent.process.IO.OUT
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.UserInput.enter
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.DockerRunCommandLine.Options
import koodies.docker.TestImages.BusyBox
import koodies.docker.TestImages.Ubuntu
import koodies.exec.CommandLine
import koodies.exec.ExecTerminationCallback
import koodies.exec.Process.ExitState
import koodies.exec.Process.ProcessState.Running
import koodies.exec.Process.ProcessState.Terminated
import koodies.exec.alive
import koodies.exec.exitCode
import koodies.exec.hasState
import koodies.exec.started
import koodies.test.Slow
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.poll
import koodies.times
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
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
            any { it is IO.OUT && it.unformatted == "test" }
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
                any { it is IO.OUT && it.unformatted == "test" }
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
