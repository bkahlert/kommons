package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.exec.IO.Error
import com.bkahlert.kommons.exec.IO.Input
import com.bkahlert.kommons.exec.IO.Meta
import com.bkahlert.kommons.exec.IO.Output
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.Process.ExitState.ExitStateHandler
import com.bkahlert.kommons.exec.Process.State.Excepted
import com.bkahlert.kommons.exec.Process.State.Exited
import com.bkahlert.kommons.exec.Process.State.Exited.Failed
import com.bkahlert.kommons.exec.Process.State.Exited.Succeeded
import com.bkahlert.kommons.exec.Process.State.Running
import com.bkahlert.kommons.io.createTempFile
import com.bkahlert.kommons.io.delete
import com.bkahlert.kommons.minus
import com.bkahlert.kommons.runtime.wait
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.Smoke
import com.bkahlert.kommons.test.junit.SimpleId
import com.bkahlert.kommons.test.shouldMatchGlob
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.toStringContainsAll
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.lines
import com.bkahlert.kommons.text.matchesGlob
import com.bkahlert.kommons.text.removeAnsi
import com.bkahlert.kommons.time.poll
import com.bkahlert.kommons.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.exists
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime

class JavaExecTest {

    @Nested
    inner class Startup {

        @TestFactory
        fun `should be running`(simpleId: SimpleId) = withTempDir(simpleId) {
            val (exec, file) = createLazyFileCreatingExec()
            expectThat(exec).hasState<Running>()
            expectThat(poll { file.exists() }.every(100.milliseconds).forAtMost(8.seconds)).isTrue()
        }

        @TestFactory
        fun `should process`(simpleId: SimpleId) = testEachOld<Exec.() -> Exec>(
            { processSilently().apply { waitFor() } },
            { processSynchronously() },
            { processAsynchronously().apply { waitFor() } },
        ) { operation ->
            withTempDir(simpleId) {
                val exec = createCompletingExec().operation()
                expecting { exec.state } that { isA<Exited>() }
                expecting { exec } that { completesWithIO() }
                expecting { poll { exec.successful }.every(100.milliseconds).forAtMost(8.seconds) } that { isTrue() }
            }
        }

        @Test
        fun `should be alive`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(sleep = 5.seconds)
            expectThat(exec).alive
            exec.kill()
        }

        @Test
        fun `should have start`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(sleep = 5.seconds)
            expectThat(exec).hasState<Running> {
                start.timePassed.isLessThan(2.seconds)
            }
            exec.kill()
        }

        @Test
        fun `should have running state`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(sleep = 5.seconds)
            expectThat(exec).hasState<Running> {
                status.isEqualTo("Process ${exec.pid} is running.")
                runningPid.isGreaterThan(0)
            }
            exec.kill()
        }

        @Test
        fun `should provide PID`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(42)
            expectThat(exec).get { pid }.isGreaterThan(0)
        }

        @Test
        fun `should provide working directory`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec()
            expectThat(exec).workingDirectory.isEqualTo(this)
        }

        @Test
        fun `should provide command line`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec()
            expectThat(exec).commandLine.toStringContainsAll(
                ">&1 echo \"test out\"",
                ">&2 echo \"test err\"",
                "exit 0"
            )
        }

        @Test
        fun `should provide IO`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec().processSilently().also { it.waitFor() }
            expectThat(exec).log.logs(Output typed "test out", Error typed "test err")
        }

        @Test
        fun `should redirect err if specified`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(redirectErrorStream = true).processSilently().also { it.waitFor() }
            expectThat(exec).log.logs(Output typed "test out", Output typed "test err")
        }
    }

    @Nested
    inner class ToString {

        private val shared = "commandLine=*, execTerminationCallback=❌, destroyOnShutdown=✅"

        @Test
        fun `should format running exec`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec: Exec = createCompletingExec()
            "$exec".ansiRemoved shouldMatchGlob "JavaExec(process=Process(pid=*, exitValue=*), successful=⏳️, $shared)"
        }

        @Test
        fun `should format terminated exec`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec: Exec = createCompletingExec().also { it.waitFor() }
            "$exec" shouldMatchGlob "JavaExec(process=Process(pid=*, exitValue=*), successful=✅, $shared)"
        }
    }

    @Nested
    inner class Interaction {

        @Slow @Test
        fun `should provide output processor access to own running exec`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec: Exec = process(ShellScript {
                !"""
                 while true; do
                    >&1 echo "test out"
                    >&2 echo "test err"

                    read -p "Prompt: " READ
                    >&2 echo "${'$'}READ"
                    >&1 echo "${'$'}READ"
                done
                """.trimIndent()
            })

            kotlin.runCatching {
                exec.process(ProcessingMode(async = true)) { exec, callback ->
                    callback { io ->
                        if (io !is Meta && io !is Input) {
                            kotlin.runCatching { exec.enter("just read $io") }
                                .recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                        }
                    }
                }

                poll { exec.io.toList().size >= 7 }.every(100.milliseconds)
                    .forAtMost(15.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
                exec.stop()
                exec.waitFor()
                expectThat(exec) {
                    killed.io.get("logged %s") { toList() }.contains(
                        Output typed "test out",
                        Error typed "test err",
                        Input typed "just read ${Output typed "test out"}",
                        Input typed "just read ${Error typed "test err"}",
                    )
                }
            }.onFailure {
                // fails from time to time, therefore this unconventional introspection code
                println(
                    """
                    Logged instead:
                    ${exec.io.toList()}
                    """.trimIndent()
                )
            }.getOrThrow()
        }
    }

    @Nested
    inner class Termination {

        @Test
        fun `by polling state`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(0)
            poll { exec.state is Exited }.every(0.5.seconds).forAtMost(8.seconds) { fail { "Did not terminate." } }
            expectThat(exec).hasState<Exited>()
        }

        @Test
        fun `by waiting for`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(0)
            expectThat(exec.waitFor()).isA<Succeeded>()
        }

        @Test
        fun `by waiting for termination`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(0)
            expectThat(exec.waitFor()) {
                isA<ExitState>() and {
                    status.get { this.ansiRemoved shouldMatchGlob "Process ${exec.pid} terminated *" }
                    pid.isGreaterThan(0)
                    exitCode.isEqualTo(0)
                    io.isEmpty() // because exec wasn't processed
                }
            }
        }

        @Test
        fun `should return same termination on multiple calls`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(0)
            expectThat(exec.waitFor()) {
                isSameInstanceAs(exec.state)
            }
        }

        @TestFactory
        fun `by destroying using`(simpleId: SimpleId) = testEachOld(
            Builder<Exec>::stopped,
            Builder<Exec>::killed,
        ) { destroyOperation ->
            expecting {
                measureTime {
                    withTempDir(simpleId) {
                        val exec = createLoopingExec()
                        expectThat(exec) {
                            destroyOperation.invoke(this).joined.hasState<Failed> {
                                exitCode.isGreaterThan(0)
                            }
                            not { alive }.get { this.alive }.isFalse()
                            exitCodeOrNull.not { isEqualTo(0) }
                        }
                    }
                }
            } that {
                isLessThanOrEqualTo(5.seconds)
            }
        }

        @Test
        fun `should provide exit code`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(exitValue = 42)
            expectThat(exec).exited.exitCode.isEqualTo(42)
        }

        @Test
        fun `should have runtime`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = CommandLine("sleep", "1").exec()
            expectThat(exec).exited.get { runtime }.isGreaterThan(Duration.ZERO)
        }

        @Test
        fun `should not be alive`(simpleId: SimpleId) = withTempDir(simpleId) {
            val exec = createCompletingExec(0)
            expectThat(exec).exited.not { get { exec }.alive }
        }

        @Nested
        inner class PreTerminationCallback {

            @Test
            fun `should be called`(simpleId: SimpleId) = withTempDir(simpleId) {
                var callbackExec: Exec? = null
                expect {
                    that(createCompletingExec().addPreTerminationCallback {
                        callbackExec = this
                    }).succeeds()
                    100.milliseconds.sleep()
                    expectThat(callbackExec).isNotNull()
                }
            }

            @Test
            fun `should be propagate exceptions`(simpleId: SimpleId) = withTempDir(simpleId) {
                expectThat(createCompletingExec().addPreTerminationCallback {
                    throw RuntimeException("test")
                }.onExit).wait().isSuccess()
                    .isA<Excepted>().get { exception }.isNotNull().message.isEqualTo("test")
            }
        }

        @Nested
        inner class OfSuccessfulExec {

            @Test
            fun `should succeed on 0 exit code by default`(@TempDir tempDir: Path) {
                val exec = tempDir.createCompletingExec(0)
                expectThat(exec.waitFor()).isA<Succeeded>()
            }

            @Test
            fun `should call callback`(simpleId: SimpleId) = withTempDir(simpleId) {
                var callbackCalled = false
                val exec = createCompletingExec(exitValue = 0, execTerminationCallback = {
                    callbackCalled = true
                })
                expectThat(exec).succeeds()

                poll { callbackCalled }.every(0.1.seconds).forAtMost(1.seconds)
                expectThat(callbackCalled).isTrue()
            }

            @Test
            fun `should call post-termination callback`(simpleId: SimpleId) = withTempDir(simpleId) {
                var callbackExec: Exec? = null
                expect {
                    that(createCompletingExec(exitValue = 0).addPostTerminationCallback {
                        callbackExec = this
                    }).succeeds()
                    100.milliseconds.sleep()
                    expectThat(callbackExec).isNotNull()
                }
            }

            @Smoke @Test
            fun `should exit with Successful termination`(@TempDir tempDir: Path) {
                val exec = tempDir.createCompletingExec(0)
                expectThat(exec).succeeds() and {
                    start.timePassed.isLessThan(2.seconds)
                    end.timePassed.isLessThan(2.seconds)
                    runtime.isLessThan(2.seconds)
                    toString().ansiRemoved.matchesGlob("Process ${exec.pid} terminated successfully at *")
                    status.get { this.ansiRemoved shouldMatchGlob "Process ${exec.pid} terminated successfully at *" }
                    pid.isGreaterThan(0)
                    exitCode.isEqualTo(0)
                    io.isEmpty() // because exec wasn't processed
                }
            }
        }

        @Nested
        inner class FailedExec {

            @Test
            fun `should fail on non-0 exit code by default`(simpleId: SimpleId) = withTempDir(simpleId) {
                val exec = createCompletingExec(42)
                expectThat(exec.waitFor()).isA<Failed>().io.any { removeAnsi.contains("terminated with exit code 42") }
            }

            @Test
            fun `should meta log on exit`(simpleId: SimpleId) = withTempDir(simpleId) {
                val exec = createCompletingExec(42)
                expectThat(exec.waitFor()).isA<Failed>().io.any { removeAnsi.contains("terminated with exit code 42") }
            }

            @Test
            fun `should meta log dump`(simpleId: SimpleId) = withTempDir(simpleId) {
                val exec = createCompletingExec(42)
                expectThat(exec.waitFor()).isA<Failed>()
                    .io().containsDump()
            }

            @Test
            fun `should return exit state on waitFor`(simpleId: SimpleId) = withTempDir(simpleId) {
                val exec = createCompletingExec(42)
                expectThat(exec.waitFor()).isA<ExitState>()
            }

            @Test
            fun `should fail on exit`(simpleId: SimpleId) = withTempDir(simpleId) {
                val exec = createCompletingExec(42)
                expectThat(exec.waitFor()).isA<Failed>()
            }

            @Test
            fun `should call callback`(simpleId: SimpleId) = withTempDir(simpleId) {
                var callbackCalled = false
                val exec = createCompletingExec(exitValue = 42, execTerminationCallback = {
                    callbackCalled = true
                })
                expectThat(exec.waitFor()).isA<Failed>()

                poll { callbackCalled }.every(0.1.seconds).forAtMost(1.seconds)
                expectThat(callbackCalled).isTrue()
            }

            @Test
            fun `should call post-termination callback`(simpleId: SimpleId) = withTempDir(simpleId) {
                var callbackEx: Any? = null
                val exec = createCompletingExec(42).addPostTerminationCallback { ex -> callbackEx = ex }
                expect {
                    expectThat(exec.onExit).wait().isSuccess().isSameInstanceAs(callbackEx)
                }
            }

            @Smoke @Test
            fun `should exit with failed exit state`(@TempDir tempDir: Path) {
                val exec = tempDir.createCompletingExec(42)
                expectThat(exec.onExit).wait().isSuccess()
                    .isA<Failed>() and {
                    start.timePassed.isLessThan(2.seconds)
                    end.timePassed.isLessThan(2.seconds)
                    runtime.isLessThan(2.seconds)
                    status.lines().first().get { this.ansiRemoved shouldMatchGlob "Process ${exec.pid} terminated with exit code ${exec.exitCode}" }
                    containsDump()
                    pid.isGreaterThan(0)
                    exitCode.isEqualTo(42)
                    io.isNotEmpty()
                }
            }
        }

        @Nested
        inner class FatalExec {

            @Nested
            inner class OnExitHandlerException {

                private fun Path.fatallyFailingExec(
                    execTerminationCallback: ExecTerminationCallback? = null,
                ): Exec =
                    createCompletingExec(
                        exitValue = 42,
                        exitStateHandler = { _, _, _ -> throw RuntimeException("handler error") },
                        execTerminationCallback = execTerminationCallback
                    )

                @Test
                fun `should exit fatally on exit handler exception`(simpleId: SimpleId) = withTempDir(simpleId) {
                    val exec = fatallyFailingExec()
                    expectThat(exec.waitFor()).isA<Excepted>() and {
                        start.timePassed.isLessThan(2.seconds)
                        end.timePassed.isLessThan(2.seconds)
                        runtime.isLessThan(2.seconds)
                        get { exception }.isNotNull().message.isEqualTo("handler error")
                        status.removeAnsi.isEqualTo("Unexpected error terminating process ${exec.pid} with exit code 42:$LF\thandler error")
                        io.any { contains("handler error") }
                    }
                }

                @Test
                fun `should meta log on exit`(simpleId: SimpleId) = withTempDir(simpleId) {
                    val exec = fatallyFailingExec()
                    expectThat(exec.waitFor()).isA<Excepted>().containsDump()
                }

                @Test
                fun `should call callback`(simpleId: SimpleId) = withTempDir(simpleId) {
                    var callbackCalled = false
                    val exec = fatallyFailingExec { callbackCalled = true }
                    expectThat(exec.onExit).wait().isSuccess().isA<Excepted>()

                    poll { callbackCalled }.every(0.1.seconds).forAtMost(1.seconds)
                    expectThat(callbackCalled).isTrue()
                }

                @Smoke @Test
                fun `should call post-termination callback`(simpleId: SimpleId) = withTempDir(simpleId) {
                    var termination: Any? = null
                    val exec = fatallyFailingExec().addPostTerminationCallback { termination = it }
                    expectThat(exec.onExit).wait()
                        .isSuccess()
                        .isA<Excepted>()
                        .isSameInstanceAs(termination)
                }
            }
        }
    }
}

fun createLoopingScript() = ShellScript {
    !"""
        while true; do
            >&1 echo "test out"
            >&2 echo "test err"
            sleep 1
        done
    """.trimIndent()
}

fun createCompletingScript(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
) = ShellScript {
    !""">&1 echo "test out""""
    !""">&2 echo "test err""""
    sleep.takeIf { it.isPositive() }?.also { !"sleep ${it.toDouble(DurationUnit.SECONDS)}" }
    !"""exit $exitValue"""
}

private fun Path.process(
    shellScript: ShellScript,
    redirectErrorStream: Boolean = false,
    environment: Map<String, String> = emptyMap(),
    exitStateHandler: ExitStateHandler? = null,
    execTerminationCallback: ExecTerminationCallback? = null,
): Exec = shellScript.toCommandLine(environment, this)
    .let { CommandLine(it.command, it.arguments, name = shellScript.name, exitStateHandler = exitStateHandler) }
    .toExec(redirectErrorStream, environment, this, execTerminationCallback)

fun Path.createLoopingExec(): Exec = process(shellScript = createLoopingScript())

fun Path.createCompletingExec(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
    redirectErrorStream: Boolean = false,
    environment: Map<String, String> = emptyMap(),
    exitStateHandler: ExitStateHandler? = null,
    execTerminationCallback: ExecTerminationCallback? = null,
): Exec = process(
    createCompletingScript(exitValue, sleep),
    redirectErrorStream,
    environment,
    exitStateHandler,
    execTerminationCallback,
)

fun Path.createLazyFileCreatingExec(): Pair<Exec, Path> {
    val nonExistingFile = createTempFile(suffix = ".txt").apply { delete() }
    val fileCreatingCommandLine = CommandLine("touch", nonExistingFile.pathString)
    return fileCreatingCommandLine.toExec(false, emptyMap(), this, null) to nonExistingFile
}


val <T : Exec> Builder<T>.alive: Builder<T>
    get() = assert("is alive") { if (it.alive) pass() else fail("is not alive: ${it.io}") }

val <T : Exec> Builder<T>.workingDirectory: Builder<Path?>
    get() = get("working directory") { workingDirectory }

val <T : Exec> Builder<T>.commandLine: Builder<CommandLine>
    get() = get("command line") { commandLine }

val <T : Exec> Builder<T>.log: DescribeableBuilder<List<IO>> get() = get("log %s") { io.toList() }

private fun Builder<Exec>.completesWithIO() = log.logs(Output typed "test out", Error typed "test err")

val <T : Exec> Builder<T>.io: DescribeableBuilder<List<IO>>
    get() = get("logged IO") { io.toList() }

val Builder<List<IO>>.output: DescribeableBuilder<List<Output>>
    get() = get("out") { filterIsInstance<Output>() }

val Builder<List<IO>>.error: DescribeableBuilder<List<Error>>
    get() = get("err") { filterIsInstance<Error>() }

val Builder<out List<IO>>.ansiRemoved: DescribeableBuilder<String>
    get() = get("ANSI escape codes removed") { IOSequence(this).ansiRemoved }

val Builder<out List<IO>>.ansiKept: DescribeableBuilder<String>
    get() = get("ANSI escape codes kept") { IOSequence(this).ansiKept }

@JvmName("failureContainsDump")
fun <T : Failed> Builder<T>.containsDump() =
    with({ dump }) { isNotNull() and { containsDump() } }

@JvmName("fatalContainsDump")
fun Builder<Excepted>.containsDump() =
    with({ dump }) { containsDump() }

fun Builder<String>.containsDump() {
    compose("contains dump") {
        contains("dump has been written")
        contains(".log")
        contains(".ansi-removed.log")
    }.then { if (allPassed) pass() else fail() }
}


fun Builder<List<IO>>.logs(vararg io: IO) = logs(io.toList())
fun Builder<List<IO>>.logs(io: Collection<IO>) = logsWithin(io = io)

fun Builder<List<IO>>.logsWithin(timeFrame: Duration = 5.seconds, io: Collection<IO>) =
    assert("logs $io within $timeFrame") { ioLog ->
        when (poll {
            ioLog.toList().containsAll(io)
        }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("logged ${ioLog.toList()} instead")
        }
    }

inline val <reified T : Process> Builder<T>.stopped: Builder<T>
    get() = get("with stop() called") { stop() }.isA()

inline val <reified T : Process> Builder<T>.killed: Builder<T>
    get() = get("with kill() called") { kill() }.isA()

inline fun <reified T : Process.State> Builder<out Process>.hasState(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<out Process> =
    compose("state") {
        get { state }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }

inline fun <reified T : Process.State> Builder<out Process>.hasState(
): Builder<out Process> =
    compose("state") {
        get { state }.isA<T>()
    }.then { if (allPassed) pass() else fail() }


inline val Builder<out Process.State>.start
    get(): Builder<Instant> = get("start") { start }
inline val Builder<out Process.State>.status
    get(): Builder<String> = get("status") { status }

inline val Builder<Running>.runningPid
    get(): Builder<Long> = get("pid") { pid }

inline val Builder<out ExitState>.end
    get(): Builder<Instant> = get("end") { end }
inline val Builder<out ExitState>.runtime
    get(): Builder<Duration> = get("runtime") { runtime }

inline val <T : ExitState> Builder<T>.pid
    get(): Builder<Long> = get("pid") { pid }
inline val <T : ExitState> Builder<T>.exitCode
    get(): Builder<Int> = get("exit code") { exitCode }
inline val <T : ExitState> Builder<T>.io
    get(): Builder<List<IO>> = get("io") { io.toList() }


inline val Builder<out Failed>.dump: Builder<String?>
    get(): Builder<String?> = get("dump") { dump }

inline val Builder<out Excepted>.exception: Builder<Throwable?>
    get(): Builder<Throwable?> = get("exception") { exception }


fun Builder<out ExitState>.io() =
    get("IO with kept ANSI escape codes") { io.ansiKept }


val Builder<Instant>.timePassed
    get() = get("time passed since now") { Now.minus(this) }
