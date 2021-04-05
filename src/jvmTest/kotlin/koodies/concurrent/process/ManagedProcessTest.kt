package koodies.concurrent.process

import koodies.concurrent.process
import koodies.concurrent.process.ManagedProcess.Evaluated
import koodies.concurrent.process.ManagedProcess.Evaluated.*
import koodies.concurrent.process.Process.ProcessState.Prepared
import koodies.concurrent.process.Process.ProcessState.Running
import koodies.concurrent.process.UserInput.enter
import koodies.concurrent.scriptPath
import koodies.concurrent.toManagedProcess
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
import koodies.text.ANSI.ansiRemoved
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
import strikt.api.Assertion.Builder
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThanOrEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isSameInstanceAs
import strikt.assertions.isSuccess
import strikt.assertions.isTrue
import strikt.assertions.message
import wait
import java.nio.file.Path
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
        fun `should not start on its own`(uniqueId: UniqueId) = testEach<Pair<ManagedProcess.() -> Any?, Builder<Any?>.() -> Unit>>(
            Pair({}, { }),
            Pair({ metaStream }, { isNotNull() }),
            Pair({ started }, { isEqualTo(false) }),
            Pair({ state }, { isA<Prepared>().status.isEqualTo("Process has not yet started.") }),
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
        fun `should start implicitly`(uniqueId: UniqueId) = testEach<Pair<ManagedProcess.() -> Any?, Builder<Any?>.() -> Unit>>(
            Pair({ pid }, { isA<Long>().isGreaterThan(0) }),
            Pair({ inputStream }, { isNotNull() }),
            Pair({ outputStream }, { isNotNull() }),
            Pair({ errorStream }, { isNotNull() }),
            Pair({ apply { runCatching { exitValue } } }, { isA<ManagedProcess>().evaluated.exitCode.isEqualTo(0) }),
            Pair({ onExit.get() }, { isA<Successful>().exitCode.isEqualTo(0) }),
            Pair({ waitFor() }, { isEqualTo(0) }),
            Pair({ waitForTermination() }, { isA<Successful>().exitCode.isEqualTo(0) }),
        ) { (operation, assertion) ->
            withTempDir(uniqueId) {
                val (process, file) = createLazyFileCreatingProcess()
                test { expectThat(process.operation()).assertion() }
                test { expect { poll { file.exists() }.every(100.milliseconds).forAtMost(8.seconds) }.isTrue() }
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
        fun `should have running state`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(sleep = 5.seconds).start()
            expectThat(process).hasState<Running> {
                status.isEqualTo("Process ${process.pid} is running.")
                runningPid.isGreaterThan(0)
            }
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

        private val shared = "commandLine={}, processTerminationCallback={}, destroyOnShutdown={}"

        @Test
        fun `should format initial process`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess()
            expectThat("$process").matchesCurlyPattern("ManagedJavaProcess(delegate=not yet started, started=❌, $shared)")
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
            })

            kotlin.runCatching {
                process.process({ async }) { io ->
                    if (io !is IO.META && io !is IO.INPUT) {
                        kotlin.runCatching { enter("just read $io") }
                            .recover { if (it.message?.contains("stream closed", ignoreCase = true) != true) throw it }
                    }
                }

                poll { process.ioLog.getCopy().size >= 7 }.every(100.milliseconds)
                    .forAtMost(15.seconds) { fail("Less than 6x I/O logged within 8 seconds.") }
                process.stop()
                process.waitForTermination()
                expectThat(process) {
                    killed.log.get("logged %s") { getCopy() }.contains(
                        IO.OUT typed "test out",
                        IO.ERR typed "test err",
                        IO.INPUT typed "just read ${IO.OUT typed "test out"}",
                        IO.INPUT typed "just read ${IO.ERR typed "test err"}",
                    )
                }
            }.onFailure {
                // fails from time to time, therefore this unconventional introspection code
                println(
                    """
                    Logged instead:
                    ${process.ioLog.getCopy()}
                    """.trimIndent().wrapWithBorder())
            }.getOrThrow()
        }
    }

    @Nested
    inner class Termination {

        @Test
        fun `by waiting for`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(0)
            expectThat(process.waitFor()).isEqualTo(0)
        }

        @Test
        fun `by waiting for termination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(0)
            expectThat(process.waitForTermination()) {
                isA<Evaluated>().and {
                    status.matchesCurlyPattern("Process ${process.pid} terminated {}")
                    pid.isGreaterThan(0)
                    exitCode.isEqualTo(0)
                    io.isNotEmpty()
                }
            }
        }

        @Test
        fun `should return same termination on multiple calls`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(0)
            expectThat(process.waitForTermination()) {
                isSameInstanceAs(process.waitForTermination())
                isSameInstanceAs(process.state)
            }
        }

        @TestFactory
        fun `by destroying using`(uniqueId: UniqueId) = listOf(
            Builder<ManagedProcess>::stopped,
            Builder<ManagedProcess>::killed,
        ).testWithTempDir(uniqueId) { destroyOperation ->
            expect {
                measureTime {
                    val process = createLoopingManagedProcess()
                    that(process) {
                        destroyOperation.invoke(this).waitedFor.hasState<Failed> {
                            exitCode.isGreaterThan(0)
                        }
                        not { alive }.get { this.alive }.isFalse()
                        exitState.not { isEqualTo(0) }
                    }
                }.also { that(it).isLessThanOrEqualTo(5.seconds) }
            }
        }

        @Test
        fun `should provide exit code`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(exitValue = 42)
            expectThat(process).evaluated.exitCode.isEqualTo(42)
        }

        @Test
        fun `should not be alive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val process = createCompletingManagedProcess(0)
            expectThat(process).evaluated.not { get { process }.alive }
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
                }.onExit).wait().isSuccess()
                    .isA<Exceptional>()
                    .exception
                    .isA<RuntimeException>()
                    .message.isEqualTo("test")
            }
        }

        @Nested
        inner class OfSuccessfulProcess {

            @Test
            fun `should meta log on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createCompletingManagedProcess(0)
                expectThat(process).completesSuccessfully()
                    .io.get { takeLast(2) }.any { contains("terminated successfully") }
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

            @Test
            fun `should exit with Successful termination`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val process = createCompletingManagedProcess(0)
                expectThat(process).completesSuccessfully().and {
                    status.matchesCurlyPattern("Process ${process.pid} terminated successfully at {}.")
                    pid.isGreaterThan(0)
                    exitCode.isEqualTo(0)
                    io.isNotEmpty()
                }
            }
        }

        @Nested
        inner class ExitCodeValidation {

            @Nested
            inner class Configuration {

                @Test
                fun `should fail on non-0 exit code by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    val process = createCompletingManagedProcess(42)
                    expectThat(process.waitForTermination()).isA<Failed>().io.any {
                        contains("terminated with exit code 42.")
                    }
                }

                @Test
                fun `should succeed on 0 exit code by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    val process = createCompletingManagedProcess(0)
                    expectThat(process.waitForTermination()).isA<Successful>().io.any { contains("terminated successfully") }
                }
            }

            @Nested
            inner class OnExitCodeMismatch {

                @Test
                fun `should meta log on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    val process = createCompletingManagedProcess(42)
                    expectThat(process.waitForTermination()).isA<Failed>().io.any {
                        contains("terminated with exit code 42.")
                    }
                }

                @Test
                fun `should meta log dump`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    val process = createCompletingManagedProcess(42)
                    expectThat(process.waitForTermination()).isA<Failed>()
                        .io<IO>().containsDump()
                }

                @Test
                fun `should return exit value on waitFor`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    val process = createCompletingManagedProcess(42)
                    expectCatching { process.waitFor() }.isSuccess().isEqualTo(42)
                }

                @Test
                fun `should fail on exit`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    val process = createCompletingManagedProcess(42)
                    expectThat(process.waitForTermination()).isA<Failed>()
                }

                @Test
                fun `should call callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    var callbackCalled = false
                    val process = createCompletingManagedProcess(42, processTerminationCallback = { callbackCalled = true })
                    expect {
                        expectThat(process.waitForTermination()).isA<Failed>()
                        expectThat(callbackCalled).isTrue()
                    }
                }

                @Test
                fun `should call post-termination callback`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    var callbackEx: Any? = null
                    val process = createCompletingManagedProcess(42).addPostTerminationCallback { ex -> callbackEx = ex }
                    expect {
                        expectThat(process.onExit).wait().isSuccess().isSameInstanceAs(callbackEx)
                    }
                }

                @Test
                fun `should exit with failed exit state`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                    val process = createCompletingManagedProcess(42)
                    expectThat(process.onExit).wait().isSuccess()
                        .isA<Failed>() and {
                        status.lines().first().matchesCurlyPattern("Process ${process.pid} terminated with exit code ${process.exitValue}. Expected 0.")
                        status.containsDump()
                        commandLine.toStringContains("should_exit_with_failed_exit_state")
                        pid.isGreaterThan(0)
                        exitCode.isEqualTo(42)
                        io.isNotEmpty()
                    }
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
    processTerminationCallback: ProcessTerminationCallback? = null,
) = process(CommandLine(
    redirects = emptyList(),
    environment = emptyMap(),
    workingDirectory = this,
    command = "/bin/sh",
    arguments = listOf("-c", shellScript.buildTo(scriptPath()).asString()),
), processTerminationCallback)

fun Path.createLoopingManagedProcess(
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess = process(shellScript = createLoopingScript()
)

fun koodies.concurrent.process.ManagedProcess.processThrowing(): koodies.concurrent.process.ManagedProcess {
    var countdown = 3
    return process({ sync }, {
        if (countdown-- <= 0) throw RuntimeException("test")
    })
}

fun Path.createCompletingManagedProcess(
    exitValue: Int = 0,
    sleep: Duration = Duration.ZERO,
    processTerminationCallback: ProcessTerminationCallback? = null,
): ManagedProcess = process(
    processTerminationCallback = processTerminationCallback,
    shellScript = createCompletingScript(exitValue, sleep))

fun Path.createLazyFileCreatingProcess(): Pair<ManagedProcess, Path> {
    val nonExistingFile = randomPath(extension = ".txt")
    val fileCreatingCommandLine = CommandLine(emptyMap(), this, "touch", nonExistingFile.asString())
    return fileCreatingCommandLine.toManagedProcess() to nonExistingFile
}


val <T : ManagedProcess> Builder<T>.alive: Builder<T>
    get() = assert("is alive") { if (it.alive) pass() else fail("is not alive: ${it.ioLog.dump()}") }

val <T : ManagedProcess> Builder<T>.log get() = get("log %s") { ioLog }

private fun Assertion.Builder<ManagedProcess>.completesWithIO() = log.logs(IO.OUT typed "test out", IO.ERR typed "test err")

val <T : ManagedProcess> Builder<T>.io
    get() = get("logged IO") { io.merged.ansiRemoved }

@JvmName("throwableContainsDump")
fun <T : Throwable> Builder<T>.containsDump(vararg containedStrings: String) =
    message.isNotNull().containsDump(*containedStrings)

fun Builder<String>.containsDump(vararg containedStrings: String = arrayOf(".sh")) {
    compose("contains dump") {
        contains("dump has been written")
        containedStrings.forEach { contains(it) }
        contains(".log")
        contains(".no-ansi.log")
    }.then { if (allPassed) pass() else fail() }
}


fun Builder<IOLog>.logs(vararg io: IO) = logs(io.toList())
fun Builder<IOLog>.logs(io: Collection<IO>) = logsWithin(io = io)
fun Builder<IOLog>.logs(predicate: List<IO>.() -> Boolean) = logsWithin(predicate = predicate)

fun Builder<IOLog>.logsWithin(timeFrame: Duration = 5.seconds, vararg io: IO) = logsWithin(timeFrame, io.toList())
fun Builder<IOLog>.logsWithin(timeFrame: Duration = 5.seconds, io: Collection<IO>) =
    assert("logs $io within $timeFrame") { ioLog ->
        when (poll {
            ioLog.getCopy().containsAll(io)
        }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("logged ${ioLog.getCopy()} instead")
        }
    }

fun Builder<IOLog>.logsWithin(timeFrame: Duration = 5.seconds, predicate: List<IO>.() -> Boolean) =
    assert("logs within $timeFrame") { ioLog ->
        when (poll {
            ioLog.getCopy().predicate()
        }.every(100.milliseconds).forAtMost(5.seconds)) {
            true -> pass()
            else -> fail("did not log within $timeFrame")
        }
    }


inline val <reified T : Process> Builder<T>.waitedFor: Builder<T>
    get() = get("with waitFor() called") { also { waitFor() } }

inline val <reified T : Process> Builder<T>.stopped: Builder<T>
    get() = get("with stop() called") { stop() }.isA()

inline val <reified T : Process> Builder<T>.killed: Builder<T>
    get() = get("with kill() called") { kill() }.isA()

inline val <reified T : ManagedProcess> Builder<T>.evaluated: Builder<Evaluated>
    get() = get("terminated") { onExit.get() }.isA()

inline fun <reified T : ManagedProcess> Builder<T>.completesSuccessfully(): Builder<Successful> =
    evaluated.isA<Successful>()

inline fun <reified T : ManagedProcess> Builder<T>.fails(): Builder<Failed> =
    evaluated.isA<Failed>().assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitCode
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

inline fun <reified T : ManagedProcess> Builder<T>.fails(expected: Int): Builder<Failed> =
    evaluated.isA<Failed>().assert("unsuccessfully with exit code $expected") {
        when (val actual = it.exitCode) {
            expected -> pass()
            0 -> fail("completed successfully")
            else -> fail("completed unsuccessfully with exit code $actual")
        }
    }


inline fun <reified T : ManagedProcess> Builder<T>.started(): Builder<T> =
    assert("have started") {
        when (it.started) {
            true -> pass()
            else -> fail("has not started")
        }
    }

inline fun <reified T : ManagedProcess> Builder<T>.notStarted(): Builder<T> =
    assert("not have started") {
        when (!it.started) {
            true -> pass()
            else -> fail("has started")
        }
    }

public inline fun <reified T : Process.ProcessState> Builder<out ManagedProcess>.hasState(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<out ManagedProcess> =
    compose("state") {
        get { state }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }

public inline fun <reified T : Process.ProcessState> Builder<out ManagedProcess>.hasState(
): Builder<out ManagedProcess> =
    compose("state") {
        get { state }.isA<T>()
    }.then { if (allPassed) pass() else fail() }

public inline val Builder<out Process.ProcessState>.status
    get(): Builder<String> =
        get("status") { status }

public inline val Builder<Running>.runningPid
    get(): Builder<Long> =
        get("pid") { pid }

public inline val <T : Process.ProcessState.Terminated> Builder<T>.pid
    get(): Builder<Long> =
        get("pid") { pid }
public inline val <T : Process.ProcessState.Terminated> Builder<T>.exitCode
    get(): Builder<Int> =
        get("exit code") { exitCode }
public inline val <T : Process.ProcessState.Terminated> Builder<T>.io
    get(): Builder<List<IO>> =
        get("io") { io }

public inline fun <reified T : IO> Builder<out Process.ProcessState.Terminated>.io() =
    get("IO of specified type") { io.merge<T>() }

public inline val Builder<Failed>.commandLine
    get(): Builder<CommandLine> =
        get("command line") { commandLine }

public inline val Builder<Exceptional>.exception
    get(): Builder<Throwable> =
        get("exception") { exception }
