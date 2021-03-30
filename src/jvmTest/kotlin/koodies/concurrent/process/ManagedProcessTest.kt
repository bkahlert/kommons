package koodies.concurrent.process

import koodies.concurrent.process
import koodies.concurrent.process.UserInput.enter
import koodies.concurrent.scriptPath
import koodies.concurrent.toManagedProcess
import koodies.exception.rootCause
import koodies.exception.rootCauseMessage
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.io.path.randomPath
import koodies.shell.ShellScript
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.testEach
import koodies.test.testWithTempDir
import koodies.test.toStringContains
import koodies.test.withTempDir
import koodies.text.Semantics.Document
import koodies.text.lines
import koodies.text.matchesCurlyPattern
import koodies.text.styling.wrapWithBorder
import koodies.text.toStringMatchesCurlyPattern
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
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import wait
import java.nio.file.Path
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class ManagedProcessTest {

    @Nested
    inner class Startup {

        @Execution(CONCURRENT) @TestFactory
        fun `should not start on its own`(uniqueId: UniqueId) = testEach<Pair<ManagedProcess.() -> Any?, Assertion.Builder<Any?>.() -> Unit>>(
            Pair({}, { }),
            Pair({ metaStream }, { isNotNull() }),
            Pair({ started }, { isEqualTo(false) }),
            Pair({ alive }, { isEqualTo(false) }),
            Pair({ successful }, { isNull() }),
            Pair({ addPreTerminationCallback { } }, { isNotNull() }),
            Pair({ addPostTerminationCallback { } }, { isNotNull() }),
            Pair({ toString() }, { isNotNull().toStringMatchesCurlyPattern("{}Process({})") }),
        ) { (operation, assertion) ->
            withTempDir(uniqueId) {
                val (process, file) = createLazyFileCreatingProcess()
                test { expectThat(process.operation()).assertion() }
                test { expect { poll { file.exists() }.every(100.milliseconds).forAtMost(8.seconds) }.isFalse() }
            }
        }

        @TestFactory
        fun `should start implicitly`(uniqueId: UniqueId) = testEach<Pair<ManagedProcess.() -> Any?, Assertion.Builder<Any?>.() -> Unit>>(
            Pair({ pid }, { isA<Long>().isGreaterThan(0) }),
            Pair({ inputStream }, { isNotNull() }),
            Pair({ outputStream }, { isNotNull() }),
            Pair({ errorStream }, { isNotNull() }),
            Pair({ apply { runCatching { exitValue } } }, { isA<ManagedProcess>().completed.exitValue.isEqualTo(0) }),
            Pair({ onExit.get() }, { isA<ManagedProcess>().completed.exitValue.isEqualTo(0) }),
            Pair({ waitFor() }, { isEqualTo(0) }),
            Pair({ waitForTermination() }, { isEqualTo(0) }),
        ) { (operation, assertion) ->
            withTempDir(uniqueId) {
                val (process, file) = createLazyFileCreatingProcess()
                test { expectThat(process.operation()).assertion() }
                test { expectThat(poll { file.exists() }.every(100.milliseconds).forAtMost(8.seconds)).isTrue() }
            }
        }

        @TestFactory
        fun `should start explicitly`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val (process, file) = createLazyFileCreatingProcess()
            expectCatching { process.start() }.isSuccess()
            expectThat(poll { file.exists() }.every(100.milliseconds).forAtMost(8.seconds)).isTrue()
        }

        @TestFactory
        fun `should start implicitly and process`(uniqueId: UniqueId) = testEach<ManagedProcess.() -> ManagedProcess>(
            { also { output() } },
            { processSilently().apply { 1.seconds.sleep() } },
            { processSynchronously {} },
            { processAsynchronously().apply { 1.seconds.sleep() } },
        ) { operation ->
            withTempDir(uniqueId) {
                val process = createCompletingManagedProcess()
                test { expectThat(process.operation()).get { started }.isTrue() }
                test { expectThat(process).completesWithIO() }
                test { expectThat(poll { process.successful == true }.every(100.milliseconds).forAtMost(8.seconds)).isTrue() }
            }
        }

        @Test
        fun `should be alive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(sleep = 5.seconds).start()
            expectThat(process).alive
            process.kill()
        }

        @Test
        fun `should meta log documents`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess().start()
            expectThat(process).log.logs {
                any {
                    it.contains(Document)
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
            val process = createLoopingManagedProcess().processSilently()
            expectThat(process).log.logs(IO.OUT typed "test out", IO.ERR typed "test err")
            process.kill()
        }
    }

    @Nested
    inner class ToString {

        private val shared = "commandLine={}, expectedExitValue=0, processTerminationCallback={}, destroyOnShutdown={}"

        @Test
        fun `should format initial process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess()
            expectThat("$process").matchesCurlyPattern("ManagedJavaProcess(delegate=not yet initialized, started=❌, $shared)")
        }

        @Test
        fun `should format running process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess().start()
            expectThat("$process").matchesCurlyPattern("ManagedJavaProcess(delegate=Process(pid={}, exitValue={}), successful={}, started=✅, $shared)")
        }
    }

    @Nested
    inner class Interaction {

        @Slow @Test
        fun `should provide output processor access to own running process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process: ManagedProcess = process(ShellScript {
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
                process.process({ async }) { io ->
                    if (io !is IO.META && io !is IO.IN) {
                        kotlin.runCatching { enter("just read $io") }
                            .recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                    }
                }

                poll { process.ioLog.logged.size >= 7 }.every(100.milliseconds)
                    .forAtMost(15.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
                process.stop()
                process.waitForTermination()
                expectThat(process) {
                    killed.log.get("logged %s") { logged }.contains(
                        IO.OUT typed "test out",
                        IO.ERR typed "test err",
                        IO.IN typed "just read ${IO.OUT typed "test out"}",
                        IO.IN typed "just read ${IO.ERR typed "test err"}",
                    )
                }
            }.onFailure {
                // fails from time to time, therefore this unconventional introspection code
                println(
                    """
                    Logged instead:
                    ${process.ioLog.logged}
                    """.trimIndent().wrapWithBorder())
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
        inner class PreTerminationCallback {

            @Test
            fun `should be called`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackProcess: ManagedProcess? = null
                expect {
                    that(createCompletingManagedProcess().addPreTerminationCallback {
                        callbackProcess = this
                    }).completesSuccessfully()
                    100.milliseconds.sleep()
                    expectThat(callbackProcess).isNotNull()
                }
            }

            @Test
            fun `should be propagate exceptions`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                expectThat(createCompletingManagedProcess().addPreTerminationCallback {
                    throw RuntimeException("test")
                }.onExit).wait().isFailure()
                    .isA<ExecutionException>()
                    .rootCauseMessage
                    .isNotNull()
                    .isEqualTo("test")
            }
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

            @Test
            fun `should call post-termination callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackProcess: ManagedProcess? = null
                expect {
                    that(createCompletingManagedProcess(exitValue = 0).addPostTerminationCallback { _ ->
                        callbackProcess = this
                    }).completesSuccessfully()
                    100.milliseconds.sleep()
                    expectThat(callbackProcess).isNotNull()
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
                expectThat(process.onExit).wait().isFailure()
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
                    expectThat(process.onExit).wait().isFailure()
                    expectThat(callbackCalled).isTrue()
                }
            }

            @Test
            fun `should call post-termination callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackEx: Any? = null
                val process = createCompletingManagedProcess(42).addPostTerminationCallback { ex -> callbackEx = ex }
                expect {
                    expectThat(process.onExit).wait().isFailure()
                    that(callbackEx).isNotNull()
                        .isA<ProcessExecutionException>()
                        .rootCauseMessage
                        .isNotNull()
                        .get { lines() }
                        .first().matchesCurlyPattern("Process {} terminated with exit code 42. Expected 0.")
                }
            }
        }

        @Nested
        inner class OfFailedProcess {

            @Nested
            inner class ThrownException {

                @Test
                fun `should occur on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    expectThat(createThrowingManagedProcess().processSilently().onExit).wait().isFailure().and {
                        message.isNotNull()
                    }
                }

                @Test
                fun `should contain dump in message`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    expectThat(createThrowingManagedProcess().processSilently().onExit).wait().isFailure().and {
                        get { toString() }.containsDump()
                    }
                }

                @Test
                fun `should have proper root cause`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    expectThat(createThrowingManagedProcess().processSilently().onExit).wait().isFailure().and {
                        rootCause.isA<ProcessExecutionException>().message.toStringContains("terminated with exit code 0. Expected 123.")
                    }
                }
            }

            @Test
            fun `should meta log on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createThrowingManagedProcess().processSilently()
                expect {
                    that(process.onExit).wait().isFailure()
                    that(process).io.containsDump()
                }
            }

            @Test
            fun `should call callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackCalled = false
                val process = createThrowingManagedProcess(processTerminationCallback = { callbackCalled = true }).processSilently()
                expect {
                    that(process.onExit).wait().isFailure()
                    that(callbackCalled).isTrue()
                }
            }

            @Test
            fun `should call post-termination callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                var callbackEx: Any? = null
                val process = createThrowingManagedProcess().addPostTerminationCallback { ex -> callbackEx = ex }.processSilently()
                expect {
                    that(process.onExit).wait().isFailure()
                    that(callbackEx).isNotNull()
                        .isA<ProcessExecutionException>()
                        .rootCause.isA<ProcessExecutionException>().message.toStringContains("terminated with exit code 0. Expected 123.")
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

private fun Path.process(
    shellScript: ShellScript,
    expectedExitValue: Int? = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
) = process(CommandLine(
    redirects = emptyList(),
    environment = emptyMap(),
    workingDirectory = this,
    command = "/bin/sh",
    arguments = listOf("-c", shellScript.buildTo(scriptPath()).asString()),
), expectedExitValue, processTerminationCallback)

fun Path.createLoopingManagedProcess(): ManagedProcess = process(shellScript = createLoopingScript())
fun Path.createThrowingManagedProcess(
    processTerminationCallback: ProcessTerminationCallback? = null,
) = process(
    shellScript = createCompletingScript(),
    expectedExitValue = 123,
    processTerminationCallback = processTerminationCallback,
)

fun Path.createCompletingManagedProcess(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
    expectedExitValue: Int = 0,
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess = process(
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    shellScript = createCompletingScript(exitValue, sleep))

fun Path.createLazyFileCreatingProcess(): Pair<ManagedProcess, Path> {
    val nonExistingFile = randomPath(extension = ".txt")
    val fileCreatingCommandLine = CommandLine(emptyMap(), this, "touch", nonExistingFile.asString())
    return fileCreatingCommandLine.toManagedProcess() to nonExistingFile
}


val <T : ManagedProcess> Assertion.Builder<T>.alive: Assertion.Builder<T>
    get() = assert("is alive") { if (it.alive) pass() else fail("is not alive: ${it.ioLog.dump()}") }

val <T : ManagedProcess> Assertion.Builder<T>.log get() = get("log %s") { ioLog }

private fun Assertion.Builder<ManagedProcess>.completesWithIO() = log.logs(IO.OUT typed "test out", IO.ERR typed "test err")

val <T : ManagedProcess> Assertion.Builder<T>.io
    get() = get("logged IO") { logged<IO>() }


fun Assertion.Builder<String>.containsDump(vararg containedStrings: String = arrayOf(".sh")) {
    compose("contains dump") {
        contains("dump has been written")
        containedStrings.forEach { contains(it) }
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
