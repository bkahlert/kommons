package koodies.docker

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ProcessState.Running
import koodies.concurrent.process.Process.ProcessState.Terminated
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors.noopProcessor
import koodies.concurrent.process.UserInput.enter
import koodies.concurrent.process.exitCode
import koodies.concurrent.process.hasState
import koodies.concurrent.process.out
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.TestImages.BusyBox
import koodies.docker.TestImages.Ubuntu
import koodies.test.Slow
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.poll
import koodies.times
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import strikt.assertions.size
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class DockerProcessTest {

    @DockerRequiring([BusyBox::class]) @Test
    fun `should start docker`(uniqueId: UniqueId) {
        val dockerProcess = createProcess(uniqueId, "echo", "test")
        dockerProcess.waitForOutputOrFail(
            "Process terminated without logging: ${dockerProcess.ioLog.dump()}.",
            "Did not log \"test\" output within 8 seconds.") {
            any { it is IO.OUT && it.unformatted == "test" }
        }
    }

    @DockerRequiring([BusyBox::class]) @Test
    fun `should override toString`(uniqueId: UniqueId) {
        val dockerProcess = createProcess(uniqueId, "echo", "test")
        expectThat(dockerProcess).toStringMatchesCurlyPattern("DockerProcess { container = {} managedProcess = {} }")
    }

    @Nested
    inner class Lifecycle {

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and pass arguments`(uniqueId: UniqueId) {
            val dockerProcess = createProcess(uniqueId, "echo", "test")
            dockerProcess.waitForOutputOrFail(
                "Process terminated without logging: ${dockerProcess.ioLog.dump()}.",
                "Did not log \"test\" output within 8 seconds.") {
                any { it is IO.OUT && it.unformatted == "test" }
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and process input`(uniqueId: UniqueId) {
            var entered = false
            val dockerProcess = createProcess(uniqueId, "/bin/sh", "-c", "echo 'test 1' && cat") {
                if (it !is IO.META) {
                    if (!entered) {
                        entered = true
                        enter("test 2")
                        inputStream.close()
                    }
                }
            }

            dockerProcess.waitForOutputOrFail("Did not log self-induced \"test\" output within 8 seconds.") {
                out.containsAll(listOf(IO.OUT typed "test 1", IO.OUT typed "test 2"))
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and process output`(uniqueId: UniqueId) {
            val dockerProcess = createProcess(uniqueId, "echo", "Hello\nWorld")

            dockerProcess.waitForOutputOrFail("Did not log any output within 8 seconds.") {
                out.containsAll(listOf(IO.OUT typed "Hello", IO.OUT typed "World"))
            }
        }

        @DockerRequiring([BusyBox::class]) @Smoke @Test
        fun `should start docker and process output produced by own input`(uniqueId: UniqueId) {
            var times = 0
            val dockerProcess = createProcess(uniqueId, "/bin/sh", "-c", "echo 'test 1' && cat") {
                if (it !is IO.META) {
                    if (times < 3) {
                        times++
                        enter("test ${it.unformatted.last().toString().toInt() * 2}")
                    } else {
                        inputStream.close()
                    }
                }
            }

            dockerProcess.waitForOutputOrFail("Did not log self-produced \"test\" output within 8 seconds.") {
                out.containsAll(listOf(
                    IO.OUT typed "test 1",
                    IO.OUT typed "test 2",
                    IO.OUT typed "test 4",
                    IO.OUT typed "test 8",
                ))
            }
        }

        @Nested
        inner class IsRunning {

            private fun unprocessedProcess(uniqueId: UniqueId, command: String, vararg args: String): DockerProcess {
                val dockerRunCommandLine = DockerRunCommandLine(
                    image = BusyBox,
                    options = DockerRunCommandLine.Options(name = DockerContainer(uniqueId.simplified)),
                    commandLine = CommandLine(command, *args))
                return DockerProcess.from(dockerRunCommandLine)
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should return false on not yet started container container`(uniqueId: UniqueId) {
                val unprocessedProcess = unprocessedProcess(uniqueId, "sleep", "10")
                expectThat(unprocessedProcess.started).isFalse()
                expectThat(unprocessedProcess.alive).isFalse()
            }

            @DockerRequiring([BusyBox::class], mode = ThanksForCleaningUp) @Test
            fun `should return true on running container`(uniqueId: UniqueId) {
                val runningProcess = unprocessedProcess(uniqueId, "sleep", "10")
                runningProcess.start().waitForCondition("Did not start in time.") { state is Running }
                expectThat(runningProcess.alive).isTrue()
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should return false on completed container`(uniqueId: UniqueId) {
                val completedProcess = unprocessedProcess(uniqueId, "sleep", "1")
                completedProcess.start().apply {
                    waitForCondition("Did not start in time.") { state is Running }
                    waitForCondition("Did not complete in time.") { state is Terminated }
                }
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should stop running container`(uniqueId: UniqueId) {
                val runningProcess = unprocessedProcess(uniqueId, "sleep", "10")
                runningProcess.start().apply {
                    waitForCondition("Did not start in time.") { state is Running }
                }

                val passed = measureTime { runningProcess.stop(timeout = 1.seconds) }
                expectThat(passed).isLessThan(3.seconds)
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should kill running container`(uniqueId: UniqueId) {
                val runningProcess = unprocessedProcess(uniqueId, "sleep", "10")
                runningProcess.start().apply {
                    waitForCondition("Did not start in time.") { state is Running }
                }

                val passed = measureTime { runningProcess.kill(gracefulStopTimeout = 0.seconds) }
                expectThat(passed).isLessThan(1.seconds)
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should have failed state on non 0 exit code`(uniqueId: UniqueId) {
            val completedProcess = createProcess(uniqueId, "invalid")
            expectThat(completedProcess).hasState<ExitState.Failure> { exitCode.isEqualTo(127) }
        }

        @Slow @DockerRequiring([BusyBox::class]) @Test
        fun `should remove docker container after completion`(uniqueId: UniqueId) {
            val completedProcess = createProcess(uniqueId, "sleep", "1")
            completedProcess.apply {
                waitForCondition("Did not start in time.") { container.state is DockerContainer.State.Existent.Running }
                waitForCondition("Did not stop in time.") { state is Terminated }
                expectThat(container).hasState<DockerContainer.State.NotExistent>()
            }
        }
    }

    @Slow @DockerRequiring([BusyBox::class]) @Test
    fun `should not produce incorrect empty lines`(uniqueId: UniqueId) {
        val output = synchronizedListOf<IO>()
        val dockerProcess = createProcess(uniqueId, "/bin/sh", "-c", """
                while true; do
                ${20.times { "echo \"looping\"" }.joinToString("; ")}
                sleep 1
                done
            """.trimIndent()) {
            if (it !is IO.META) {
                output.add(it)
                if (output.size > 100) {
                    kill()
                }
            }
        }
        expectThat(output).size.isGreaterThanOrEqualTo(100)
        expectThat(output.filter { it.isBlank() }.size).isLessThan(25)
    }
}


private fun createProcess(
    uniqueId: UniqueId,
    command: String,
    vararg args: String,
    processor: Processor<DockerProcess> = noopProcessor(),
): DockerProcess =
    docker({
        image by Ubuntu
        options { name { uniqueId.simplified } }
        commandLine by CommandLine(command, *args)
    }, processor = processor)

private fun DockerProcess.waitForCondition(
    errorMessage: String,
    test: DockerProcess.() -> Boolean,
) {
    poll {
        test()
    }.every(100.milliseconds).forAtMost(2.seconds) {
        fail(errorMessage)
    }
}

private fun DockerProcess.waitForOutputOrFail(
    errorMessage: String,
    stillRunningErrorMessage: String = errorMessage,
    test: List<IO>.() -> Boolean,
) {
    poll {
        ioLog.getCopy().test()
    }.every(100.milliseconds).forAtMost(8.seconds) {
        if (alive) fail(stillRunningErrorMessage)
        fail(errorMessage)
    }
}
