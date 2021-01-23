package koodies.concurrent.process

import koodies.concurrent.process
import koodies.concurrent.process.UserInput.enter
import koodies.concurrent.scriptPath
import koodies.exception.rootCause
import koodies.exception.rootCauseMessage
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.shell.ShellScript
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.containsExactlyInSomeOrder
import koodies.test.matchesCurlyPattern
import koodies.test.testWithTempDir
import koodies.test.toStringContains
import koodies.test.withTempDir
import koodies.text.lines
import koodies.text.styling.Boxes.Companion.wrapWithBox
import koodies.time.poll
import koodies.time.sleep
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.cause
import strikt.assertions.contains
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import java.nio.file.Path
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class ManagedProcessTest {

    @Nested
    inner class Creation {
        @Test
        fun `should not start on its own`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectCatching {
                createThrowingManagedProcess()
                200.milliseconds.sleep()
            }.isSuccess()
        }
    }


    @Nested
    inner class Startup {

        @Test
        fun `should start if accessed directly`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess()
            expectThat(process).completed.exitValue.isEqualTo(0)
        }

        @Test
        fun `should provide string on direct toString`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess()
            @Suppress("LongLine")
            expectThat("$process")
                .matchesCurlyPattern("ManagedJavaProcess[delegate=Process[pid={}, exitValue={}]; result={}; started=false; commandLine={}; expectedExitValue=0; processTerminationCallback={}; destroyOnShutdown={}]")
        }

        @Test
        fun `should be alive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(sleep = 5.seconds)
            expectThat(process).alive
            process.kill()
        }

        @Test
        fun `should meta log documents`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess()
            expectThat(process).log.logs {
                any {
                    it.contains("📄")
                    it.contains("file:")
                    it.contains(".sh")
                }
            }
        }

        @Test
        fun `should provide PID`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(42)
            expectThat(process).get { pid }.isGreaterThan(0)
        }

        @Test
        fun `should provide IO`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createLoopingManagedProcess().silentlyProcess()
            expectThat(process).log.logs(IO.Type.OUT typed "test out", IO.Type.ERR typed "test err")
            process.kill()
        }
    }

    @Nested
    inner class Interaction {

        @Slow @Test
        fun `should provide output processor access to own running process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process: ManagedProcess = process(shellScript = ShellScript {
                !"""
                 while true; do
                    >&1 echo "test out"
                    >&2 echo "test err"

                    read -p "Prompt: " READ
                    >&2 echo "${'$'}READ"
                    >&1 echo "${'$'}READ"
                done
                """.trimIndent()
            }, expectedExitValue = null)

            kotlin.runCatching {
                process.process { io ->
                    if (io.type != IO.Type.META) {
                        kotlin.runCatching {
                            enter("just read $io")
                        }.recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                    }
                }

                poll { process.ioLog.logged.size >= 7 }.every(100.milliseconds)
                    .forAtMost(800.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
                process.stop()
                process.waitForTermination()
                expectThat(process) {
                    killed
                        .log.get("logged %s") { logged.drop(3).take(4) }.containsExactlyInSomeOrder {
                            +(IO.Type.OUT typed "test out") + (IO.Type.ERR typed "test err")
                            +(IO.Type.IN typed "just read ${IO.Type.OUT.format("test out")}") + (IO.Type.IN typed "just read ${IO.Type.ERR.format("test err")}")
                        }
                }
            }.onFailure {
                // fails from time to time, therefore this unconventional introspection code
                println(
                    """
                    Logged instead:
                    ${process.ioLog.logged}
                    
                    ... of what was evaluated:
                    ${process.ioLog.logged.drop(3).take(4)}
                    """.trimIndent().wrapWithBox())
            }.getOrThrow()
        }
    }

    @Nested
    inner class Termination {

        @TestFactory
        fun `by waiting using`(uniqueId: UniqueId) = listOf(
            Assertion.Builder<ManagedProcess>::waitedFor,
        ).testWithTempDir(uniqueId) { waitOperation ->
            expect {
                measureTime {
                    that(createCompletingManagedProcess()) {
                        waitOperation.invoke(this).completesSuccessfully().not { alive }
                    }
                }.also { that(it).isLessThanOrEqualTo(5.seconds) }
            }
        }

        @TestFactory
        fun `by destroying using`(uniqueId: UniqueId) = listOf(
            Assertion.Builder<ManagedProcess>::stopped,
            Assertion.Builder<ManagedProcess>::killed,
        ).testWithTempDir(uniqueId) { destroyOperation ->
            expect {
                measureTime {
                    that(createLoopingManagedProcess()) {
                        catching { destroyOperation.invoke(this).waitedFor }.isFailure()
                        not { alive }
                        exitValue.not { isEqualTo(0) }
                    }
                }.also { that(it).isLessThanOrEqualTo(5.seconds) }
            }
        }

        @Test
        fun `should provide exit code`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(exitValue = 42, expectedExitValue = 42)
            expectThat(process).completed.exitValue.isEqualTo(42)
        }

        @Test
        fun `should not be alive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(0)
            expectThat(process).completed.not { alive }
        }

        @Nested
        inner class OfSuccessfulProcess {

            @Test
            fun `should meta log on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createCompletingManagedProcess(0)
                expectThat(process).completesSuccessfully()
                    .isA<ManagedProcess>().and { get { ioLog.logged.takeLast(2) } any { contains("terminated successfully") } }
            }

            @Test
            fun `should call callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackCalled = false
                expect {
                    that(createCompletingManagedProcess(exitValue = 0, processTerminationCallback = {
                        callbackCalled = true
                    })).completesSuccessfully()
                    100.milliseconds.sleep()
                    expectThat(callbackCalled).isTrue()
                }
            }
        }

        @Nested
        inner class OnExitCodeMismatch {

            @Test
            fun `should meta log on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createCompletingManagedProcess(42)
                expect {
                    catching { process.waitFor() }.isFailure()
                    expectThat(process).io.contains("terminated with exit code 42.")
                }
            }

            @Test
            fun `should meta log dump`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createCompletingManagedProcess(42)
                expect {
                    catching { process.waitFor() }.isFailure()
                    that(process).io.containsDump()
                }
            }

            @Test
            fun `should throw on waitFor`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createCompletingManagedProcess(42)
                expectCatching { process.waitFor() }.isFailure()
                    .isA<CompletionException>()
                    .cause.isA<ProcessExecutionException>()
                    .message.isNotNull()
                    .lines().first().matchesCurlyPattern("Process {} terminated with exit code 42. Expected 0.")
            }


            @Test
            fun `should throw on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createCompletingManagedProcess(42)
                expectCatching { process.onExit.get() }.isFailure()
                    .isA<ExecutionException>()
                    .rootCauseMessage
                    .isNotNull()
                    .get { lines() }
                    .first().matchesCurlyPattern("Process {} terminated with exit code 42. Expected 0.")
            }

            @Test
            fun `should call callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackCalled = false
                val process = createCompletingManagedProcess(42, processTerminationCallback = { callbackCalled = true })
                expect {
                    catching { process.onExit.get() }.isFailure()
                    expectThat(callbackCalled).isTrue()
                }
            }
        }

        @Nested
        inner class OfFailedProcess {

            @Nested
            inner class ThrownException {

                @Test
                fun `should occur on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    expectCatching { createThrowingManagedProcess().silentlyProcess().onExit.get() }.failed.and {
                        message.isNotNull()
                    }
                }

                @Test
                fun `should contain dump in message`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    expectCatching { createThrowingManagedProcess().silentlyProcess().onExit.get() }.failed.and {
                        get { toString() }.containsDump()
                    }
                }

                @Test
                fun `should have proper root cause`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    expectCatching { createThrowingManagedProcess().silentlyProcess().onExit.get() }.failed.and {
                        rootCause.isA<ProcessExecutionException>().message.toStringContains("terminated with exit code 0. Expected 123.")
                    }
                }
            }

            @Test
            fun `should meta log on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createThrowingManagedProcess().silentlyProcess()
                expect {
                    catching { process.onExit.get() }.failed
                    that(process).io.containsDump()
                }
            }

            @Test
            fun `should call callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackCalled = false
                val process = createThrowingManagedProcess(processTerminationCallback = { callbackCalled = true }).silentlyProcess()
                expect {
                    catching { process.onExit.get() }.failed
                    that(callbackCalled).isTrue()
                }
            }
        }
    }
}

private object NoopManagedProcess : ManagedProcess
by CommandLine(emptyMap(), Locations.Temp, "").toManagedProcess() {
    init {
        start()
    }
}

val ManagedProcess.Companion.Noop: ManagedProcess get() = NoopManagedProcess

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
    !"""
        >&1 echo "test out"
        >&2 echo "test err"
        ${sleep.takeIf { it.isPositive() }?.let { "sleep ${sleep.inSeconds}" } ?: ""}
        exit $exitValue
    """.trimIndent()
}

fun Path.process(
    shellScript: ShellScript,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
) = process(CommandLine(
    redirects = emptyList(),
    environment = emptyMap(),
    workingDirectory = this,
    command = "/bin/sh",
    arguments = listOf("-c", shellScript.buildTo(scriptPath()).asString()),
), expectedExitValue, processTerminationCallback)

fun Path.createLoopingManagedProcess(): ManagedProcess = process(shellScript = createLoopingScript())
fun Path.createThrowingManagedProcess(
    processTerminationCallback: (() -> Unit)? = null,
) = process(
    shellScript = createCompletingScript(),
    expectedExitValue = 123,
    processTerminationCallback = processTerminationCallback,
)

fun Path.createCompletingManagedProcess(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = process(
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    shellScript = createCompletingScript(exitValue, sleep)).also {
    it.alive // call something to trigger execution
}


val <T : ManagedProcess> Assertion.Builder<T>.alive: Assertion.Builder<T>
    get() = assert("is alive") { if (it.alive) pass() else fail("is not alive: ${it.ioLog.dump()}") }

val <T : ManagedProcess> Assertion.Builder<T>.log get() = get("log %s") { ioLog }

val <T : ManagedProcess> Assertion.Builder<T>.io
    get() = get("merged log") {
        ioLog.logged.joinToString("\n")
    }


fun Assertion.Builder<String>.containsDump() {
    compose("contains dump") {
        contains("dump has been written")
        contains(".sh")
        contains(".log")
        contains(".no-ansi.log")
    }.then { if (allPassed) pass() else fail() }
}


fun Assertion.Builder<IOLog>.logs(vararg io: IO) = logs(io.toList())
fun Assertion.Builder<IOLog>.logs(io: Collection<IO>) = logsWithin(io = io)
fun Assertion.Builder<IOLog>.logs(predicate: List<IO>.() -> Boolean) = logsWithin(predicate = predicate)

fun Assertion.Builder<IOLog>.logsWithin(timeFrame: Duration = 5.seconds, vararg io: IO) = logsWithin(timeFrame, io.toList())
fun Assertion.Builder<IOLog>.logsWithin(timeFrame: Duration = 5.seconds, io: Collection<IO>) =
    assert("logs $io within $timeFrame") { ioLog ->
        when (poll {
            ioLog.logged.containsAll(io)
        }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("logged ${ioLog.logged} instead")
        }
    }

fun Assertion.Builder<IOLog>.logsWithin(timeFrame: Duration = 5.seconds, predicate: List<IO>.() -> Boolean) =
    assert("logs within $timeFrame") { ioLog ->
        when (poll {
            ioLog.logged.predicate()
        }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("did not log within $timeFrame")
        }
    }

inline val <reified T : Process> Assertion.Builder<T>.exitValue: Assertion.Builder<Int>
    get() = get("with exit value %s") { exitValue }

inline val <reified T : Process> Assertion.Builder<T>.waitedFor: Assertion.Builder<T>
    get() = get("with waitFor() called") { also { waitFor() } }

inline val <reified T : Process> Assertion.Builder<T>.stopped: Assertion.Builder<T>
    get() = get("with stop() called") { stop() }.isA()

inline val <reified T : Process> Assertion.Builder<T>.killed: Assertion.Builder<T>
    get() = get("with kill() called") { kill() }.isA()

inline val <reified T : Process> Assertion.Builder<T>.completed: Assertion.Builder<T>
    get() = get("completed") { onExit.get() }.isA()

inline val <reified T : Process> Assertion.Builder<Result<T>>.failed: Assertion.Builder<ExecutionException>
    get() = get("failed") { exceptionOrNull() }.isA()

inline fun <reified T : Process> Assertion.Builder<T>.completesSuccessfully(): Assertion.Builder<T> =
    completed.assert("successfully") {
        val actual = it.exitValue
        when (actual == 0) {
            true -> pass()
            else -> fail("completed with $actual")
        }
    }

inline fun <reified T : Process> Assertion.Builder<T>.completesUnsuccessfully(): Assertion.Builder<T> =
    completed.assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitValue
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

inline fun <reified T : Process> Assertion.Builder<T>.completesUnsuccessfully(expected: Int): Assertion.Builder<T> =
    completed.assert("unsuccessfully with exit code $expected") {
        when (val actual = it.exitValue) {
            expected -> pass()
            0 -> fail("completed successfully")
            else -> fail("completed unsuccessfully with exit code $actual")
        }
    }
