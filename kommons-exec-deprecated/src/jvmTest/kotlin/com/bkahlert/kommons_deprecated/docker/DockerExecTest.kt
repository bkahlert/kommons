package com.bkahlert.kommons_deprecated.docker

import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons_deprecated.collections.synchronizedListOf
import com.bkahlert.kommons_deprecated.docker.CleanUpMode.ThanksForCleaningUp
import com.bkahlert.kommons_deprecated.docker.DockerRunCommandLine.Options
import com.bkahlert.kommons_deprecated.docker.TestImages.BusyBox
import com.bkahlert.kommons_deprecated.docker.TestImages.Ubuntu
import com.bkahlert.kommons_deprecated.exec.CommandLine
import com.bkahlert.kommons_deprecated.exec.ExecTerminationCallback
import com.bkahlert.kommons_deprecated.exec.IO
import com.bkahlert.kommons_deprecated.exec.IO.Output
import com.bkahlert.kommons_deprecated.exec.Process.State.Exited
import com.bkahlert.kommons_deprecated.exec.Process.State.Running
import com.bkahlert.kommons_deprecated.exec.Processor
import com.bkahlert.kommons_deprecated.exec.Processors.spanningProcessor
import com.bkahlert.kommons_deprecated.exec.alive
import com.bkahlert.kommons_deprecated.exec.enter
import com.bkahlert.kommons_deprecated.exec.exitCode
import com.bkahlert.kommons_deprecated.exec.hasState
import com.bkahlert.kommons_deprecated.test.Smoke
import com.bkahlert.kommons_deprecated.time.poll
import io.kotest.matchers.string.shouldMatch
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isLessThan
import strikt.assertions.isTrue
import strikt.assertions.size
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class DockerExecTest {

    @DockerRequiring([BusyBox::class]) @Test
    fun `should override toString`(@TempDir tempDir: Path, simpleId: SimpleId) {
        val dockerExec = tempDir.createExec(simpleId, "echo", "test")
        dockerExec.toString() shouldMatch """
            DockerExec \{
             {4}container: DockerContainer \{ name: "DockerExecTest.should_override_toString-Path" },
             {4}exec: JavaExec\(.*\)
            }
        """.trimIndent().toRegex()
    }

    @DockerRequiring([BusyBox::class]) @Test
    fun `should start docker`(@TempDir tempDir: Path, simpleId: SimpleId) {
        val dockerExec = tempDir.createExec(simpleId, "echo", "test")
        dockerExec.waitForOutputOrFail(
            "Process terminated without logging: ${dockerExec.io.ansiRemoved}.",
            "Did not log \"test\" output within 8 seconds."
        ) {
            any { it is Output && it.ansiRemoved == "test" }
        }
    }

    @Nested
    inner class Lifecycle {

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and pass arguments`(@TempDir tempDir: Path, simpleId: SimpleId) {
            val dockerExec = tempDir.createExec(simpleId, "echo", "test")
            dockerExec.waitForOutputOrFail(
                "Process terminated without logging: ${dockerExec.io.ansiRemoved}.",
                "Did not log \"test\" output within 8 seconds."
            ) {
                any { it is Output && it.ansiRemoved == "test" }
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and process input`(@TempDir tempDir: Path, simpleId: SimpleId) {
            var entered = false
            val dockerExec = tempDir.createExec(simpleId, "/bin/sh", "-c", "echo 'test 1' && cat") { exec, callback ->
                callback { io ->
                    if (io !is IO.Meta) {
                        if (!entered) {
                            entered = true
                            exec.enter("test 2")
                            exec.inputStream.close()
                        }
                    }
                }
            }

            dockerExec.waitForOutputOrFail("Did not log self-induced \"test\" output within 8 seconds.") {
                filterIsInstance<Output>().contains(Output typed "test 1") and contains(Output typed "test 2")
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should start docker and process output`(@TempDir tempDir: Path, simpleId: SimpleId) {
            val dockerExec = tempDir.createExec(simpleId, "echo", "Hello\nWorld")

            dockerExec.waitForOutputOrFail("Did not log any output within 8 seconds.") {
                filterIsInstance<Output>().contains(Output typed "Hello") and contains(Output typed "World")
            }
        }

        @DockerRequiring([BusyBox::class]) @Smoke @Test
        fun `should start docker and process output produced by own input`(@TempDir tempDir: Path, simpleId: SimpleId) {
            var times = 0
            val dockerExec = tempDir.createExec(simpleId, "/bin/sh", "-c", "echo 'test 1' && cat") { exec, callback ->
                callback { io ->
                    if (io !is IO.Meta) {
                        if (times < 3) {
                            times++
                            exec.enter("test ${io.ansiRemoved.last().toString().toInt() * 2}")
                        } else {
                            exec.inputStream.close()
                        }
                    }
                }
            }

            dockerExec.waitForOutputOrFail("Did not log self-produced \"test\" output within 8 seconds.") {
                filterIsInstance<Output>()
                    .contains(Output typed "test 1")
                    .and(contains(Output typed "test 2"))
                    .and(contains(Output typed "test 4"))
                    .and(contains(Output typed "test 8"))
            }
        }

        @Nested
        inner class IsRunning {

            private fun Path.unprocessedProcess(
                simpleId: SimpleId,
                command: String,
                vararg args: String,
                callback: ExecTerminationCallback? = null,
            ): DockerExec = CommandLine(command, *args)
                .dockerized(BusyBox, Options(name = DockerContainer("$simpleId")))
                .toExec(false, emptyMap(), this, callback)

            @DockerRequiring([BusyBox::class], mode = ThanksForCleaningUp) @Test
            fun `should return true on running container`(@TempDir tempDir: Path, simpleId: SimpleId) {
                val runningProcess = tempDir.unprocessedProcess(simpleId, "sleep", "10")
                runningProcess.waitForCondition("Did not start in time.") { state is Running }
                expectThat(runningProcess.alive).isTrue()
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should return false on completed container`(@TempDir tempDir: Path, simpleId: SimpleId) {
                val completedProcess = tempDir.unprocessedProcess(simpleId, "sleep", "1")
                completedProcess.apply {
                    waitForCondition("Did not start in time.") { state is Running }
                    waitForCondition("Did not complete in time.") { state is Exited }
                }
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should stop running container`(@TempDir tempDir: Path, simpleId: SimpleId) {
                val runningProcess = tempDir.unprocessedProcess(simpleId, "sleep", "10")
                runningProcess.waitForCondition("Did not start in time.") { state is Running }

                val passed = measureTime { runningProcess.stop(1.seconds) }
                expectThat(passed).isLessThan(8.seconds)
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should kill running container`(@TempDir tempDir: Path, simpleId: SimpleId) {
                val runningProcess = tempDir.unprocessedProcess(simpleId, "sleep", "10")
                runningProcess.apply {
                    waitForCondition("Did not start in time.") { state is Running }
                }

                val passed = measureTime { runningProcess.kill() }
                expectThat(passed).isLessThan(4.seconds)
            }

            @DockerRequiring([BusyBox::class]) @Test
            fun `should call callback on termination`(@TempDir tempDir: Path, simpleId: SimpleId) {
                var calledBack = false
                val runningProcess = tempDir.unprocessedProcess(simpleId, "exit", "0") { calledBack = true }
                runningProcess.waitForCondition("Did not call back.", 8.seconds) { calledBack }
            }
        }

        @DockerRequiring([BusyBox::class]) @Test
        fun `should have failed state on non 0 exit code`(@TempDir tempDir: Path, simpleId: SimpleId) {
            val dockerExec = tempDir.createExec(simpleId, "invalid")
            expectThat(dockerExec).hasState<Exited.Failed> { exitCode.isEqualTo(127) }
        }

        @Slow @DockerRequiring([BusyBox::class]) @Test
        fun `should remove docker container after completion`(@TempDir tempDir: Path, simpleId: SimpleId) {
            val dockerExec = tempDir.createExec(simpleId, "echo", "was alive")

            dockerExec.apply {
                expectThat(io.ansiRemoved).contains("was alive")
                expectThat(container).hasState<DockerContainer.State.NotExistent>()
            }
        }
    }

    @Slow @DockerRequiring([BusyBox::class]) @Test
    fun `should not produce incorrect empty lines`(@TempDir tempDir: Path, simpleId: SimpleId) {
        var killed = false
        val output = synchronizedListOf<IO>()
        tempDir.createExec(
            simpleId, "/bin/sh", "-c", """
                while true; do
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                    echo "looping"
                sleep 1
                done
            """.trimIndent()
        ) { exec, callback ->
            callback { io ->
                if (io !is IO.Meta) {
                    output.add(io)
                    if (output.size > 100 && !killed) {
                        killed = true
                        exec.kill()
                    }
                }
            }
        }
        expectThat(output).size.isGreaterThanOrEqualTo(100)
        expectThat(output.filter { it.isBlank() }.size).isLessThan(25)
    }
}


private fun Path.createExec(
    simpleId: SimpleId,
    command: String,
    vararg args: String,
    processor: Processor<DockerExec> = spanningProcessor(),
): DockerExec =
    CommandLine(command, *args)
        .dockerized(Ubuntu, Options(name = DockerContainer.from("$simpleId")))
        .exec.processing(workingDirectory = this, processor = processor)

private fun DockerExec.waitForCondition(
    errorMessage: String,
    atMost: Duration = 4.seconds,
    test: DockerExec.() -> Boolean,
) {
    poll {
        test()
    }.every(100.milliseconds).forAtMost(atMost) {
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
