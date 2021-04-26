package koodies.exec

import koodies.concurrent.process.IO
import koodies.concurrent.process.ProcessingMode.Interactivity.NonInteractive
import koodies.exec.ExecTerminationTestCallback.Companion.expectThatProcessAppliesTerminationCallback
import koodies.exec.ExitStateHandlerTest.Companion.expectThatProcessAppliesExitStateHandler
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.Process.ExitState.Failure
import koodies.exec.Process.ExitState.Success
import koodies.exec.Process.ProcessState.Terminated
import koodies.io.path.Locations
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.logging.expectLogged
import koodies.test.DynamicTestsWithSubjectBuilder
import koodies.test.Smoke
import koodies.test.output.TestLogger
import koodies.test.test
import koodies.test.tests
import koodies.text.LineSeparators.LF
import koodies.text.mapLines
import koodies.text.toStringMatchesCurlyPattern
import koodies.text.withoutPrefix
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

@Execution(SAME_THREAD)
class ExecutorTest {

    @Nested
    inner class CommandLineExecutor {

        private val succeedingCommandLine = CommandLine(mapOf("TEST_PROP" to "TEST_VALUE"), Locations.Temp, "printenv")
        private val failingCommandLine = CommandLine("exit", "42")

        @Nested
        inner class ExecSync {

            @Nested
            inner class ExecOnly {

                @TestFactory
                fun `succeeding command line`() = test({
                    succeedingCommandLine.exec()
                }) {
                    expectThatProcess { starts() }
                    expectThatProcess { succeeds() }
                    expectThatProcess { logsSuccessfulIO() }
                    expectThatProcess { runsSynchronously() }
                    expectThatProcessAppliesExitStateHandler { succeedingCommandLine.exec(exitStateHandler = it) }
                    expectThatProcessAppliesTerminationCallback(null) { succeedingCommandLine.exec(execTerminationCallback = it) }
                }

                @TestFactory
                fun `failing command line`() = test({
                    failingCommandLine.exec()
                }) {
                    expectThatProcess { starts() }
                    expectThatProcess { fails() }
                    expectThatProcess { logsFailedIO() }
                    expectThatProcess { runsSynchronously() }
                    expectThatProcessAppliesExitStateHandler { failingCommandLine.exec(exitStateHandler = it) }
                    expectThatProcessAppliesTerminationCallback(null) { failingCommandLine.exec(execTerminationCallback = it) }
                }
            }

            @Nested
            inner class ExecLogging {

                @TestFactory
                fun `succeeding command line`() = test({
                    succeedingCommandLine.exec.logging()
                }) {
                    expectThatProcess { starts() }
                    expectThatProcess { succeeds() }
                    expectThatProcess { logsSuccessfulIO() }
                    expectThatProcess { runsSynchronously() }
                    expectThatProcessAppliesExitStateHandler { succeedingCommandLine.exec.logging(exitStateHandler = it) }
                    expectThatProcessAppliesTerminationCallback(null) { succeedingCommandLine.exec.logging(execTerminationCallback = it) }
                }

                @TestFactory
                fun `failing command line`() = test({
                    failingCommandLine.exec.logging()
                }) {
                    expectThatProcess { starts() }
                    expectThatProcess { fails() }
                    expectThatProcess { logsFailedIO() }
                    expectThatProcess { runsSynchronously() }
                    expectThatProcessAppliesExitStateHandler { failingCommandLine.exec.logging(exitStateHandler = it) }
                    expectThatProcessAppliesTerminationCallback(null) { failingCommandLine.exec.logging(execTerminationCallback = it) }
                }

                @TestFactory
                fun `should log to background by default`() = tests {
                    succeedingCommandLine.exec.logging() asserting { BACKGROUND.expectLogged.logsSuccessfulIO() }
                    failingCommandLine.exec.logging() asserting { BACKGROUND.expectLogged.logsFailedIO() }
                }

                @TestFactory
                fun `should log to specified logger if specified`() = tests {
                    fun logger() = InMemoryLogger(border = NONE)
                    logger().also { succeedingCommandLine.exec.logging(it) } asserting { logsSuccessfulIO() }
                    logger().also { failingCommandLine.exec.logging(it) } asserting { logsFailedIO() }
                }

                @Test
                fun TestLogger.`should log to receiver logger if available`() {
                    with(succeedingCommandLine) { logging.exec() }
                    expectLogged.logsSuccessfulIO()
                }

                @Test
                fun `should apply custom logging options`() {
                    val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                    succeedingCommandLine.exec.logging(logger) { block { caption { "custom caption" }; border = SOLID } }
                    logger.expectLogged.contains("╭──╴custom caption")
                }
            }

            @Nested
            inner class ExecProcessing {

                @TestFactory
                fun `succeeding command line`() = test({
                    succeedingCommandLine.exec.processing { }
                }) {
                    expectThatProcess { starts() }
                    expectThatProcess { succeeds() }
                    expectThatProcess { logsSuccessfulIO() }
                    expectThatProcess { runsSynchronously() }
                    expectThatProcessAppliesExitStateHandler { succeedingCommandLine.exec.processing(exitStateHandler = it) {} }
                    expectThatProcessAppliesTerminationCallback(null) { succeedingCommandLine.exec.processing(execTerminationCallback = it) {} }
                }

                @TestFactory
                fun `failing command line`() = test({
                    failingCommandLine.exec.processing { }
                }) {
                    expectThatProcess { starts() }
                    expectThatProcess { fails() }
                    expectThatProcess { logsFailedIO() }
                    expectThatProcess { runsSynchronously() }
                    expectThatProcessAppliesExitStateHandler { failingCommandLine.exec.processing(exitStateHandler = it) {} }
                    expectThatProcessAppliesTerminationCallback(null) { failingCommandLine.exec.processing(execTerminationCallback = it) {} }
                }

                @Test
                fun TestLogger.`should only log success`() {
                    succeedingCommandLine.exec.processing(parentLogger = this) {}
                    expectLogged.isEqualTo("printenv ✔︎")
                }

                @Test
                fun TestLogger.`should log dump on failure`() {
                    failingCommandLine.exec.processing(parentLogger = this) {}
                    expectLogged.containsDump(*emptyArray())
                }

                @Test
                fun TestLogger.`should log to receiver logger if available`() {
                    with(succeedingCommandLine) { logging.processing {} }
                    expectLogged.isEqualTo("printenv ✔︎")
                }

                @Test
                fun TestLogger.`should dump to receiver logger if available`() {
                    with(failingCommandLine) { logging.processing {} }
                    expectLogged.containsDump(*emptyArray())
                }

                @Test
                fun `should apply custom logging options`() {
                    val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                    succeedingCommandLine.exec.processing(parentLogger = logger,
                        loggingOptionsInit = { block { caption { "custom caption" }; border = SOLID } }) {}
                    logger.expectLogged.contains("╭──╴custom caption")
                }

                @Test
                fun `should process IO`() {
                    val processed = mutableListOf<IO>()
                    succeedingCommandLine.exec.processing { io -> processed.add(io) }
                    expectThat(processed).contains(IO.OUT typed "TEST_PROP=TEST_VALUE")
                }
            }
        }

        @Execution(CONCURRENT)
        @Nested
        inner class ExecAsync {

            @Nested
            inner class ExecOnly {

                @TestFactory
                fun `succeeding command line`() = test({
                    succeedingCommandLine.exec.async()
                }) {
                    expectThatProcess { joined.starts() }
                    expectThatProcess { joined.succeeds() }
                    expectThatProcess { joined.logsSuccessfulIO() }
                    expectThatProcess { runsAsynchronously() }
                    expectThatProcessAppliesExitStateHandler { succeedingCommandLine.exec.async(exitStateHandler = it).apply { waitFor() } }
                    expectThatProcessAppliesTerminationCallback(null) { succeedingCommandLine.exec.async(execTerminationCallback = it).apply { waitFor() } }
                }

                @TestFactory
                fun `failing command line`() = test({
                    failingCommandLine.exec.async()
                }) {
                    expectThatProcess { joined.starts() }
                    expectThatProcess { joined.fails() }
                    expectThatProcess { joined.logsFailedIO() }
                    expectThatProcess { runsAsynchronously() }
                    expectThatProcessAppliesExitStateHandler { failingCommandLine.exec.async(exitStateHandler = it).apply { waitFor() } }
                    expectThatProcessAppliesTerminationCallback(null) { failingCommandLine.exec.async(execTerminationCallback = it).apply { waitFor() } }
                }
            }

            @Nested
            inner class ExecLogging {

                @TestFactory
                fun `succeeding command line`() = test({
                    succeedingCommandLine.exec.async.logging()
                }) {
                    expectThatProcess { joined.starts() }
                    expectThatProcess { joined.succeeds() }
                    expectThatProcess { joined.logsSuccessfulIO() }
                    expectThatProcess { runsAsynchronously() }
                    expectThatProcessAppliesExitStateHandler { succeedingCommandLine.exec.async.logging(exitStateHandler = it).apply { waitFor() } }
                    expectThatProcessAppliesTerminationCallback(null) {
                        succeedingCommandLine.exec.async.logging(execTerminationCallback = it).apply { waitFor() }
                    }
                }

                @TestFactory
                fun `failing command line`() = test({
                    failingCommandLine.exec.async.logging()
                }) {
                    expectThatProcess { joined.starts() }
                    expectThatProcess { joined.fails() }
                    expectThatProcess { joined.logsFailedIO() }
                    expectThatProcess { runsAsynchronously() }
                    expectThatProcessAppliesExitStateHandler { failingCommandLine.exec.async.logging(exitStateHandler = it).apply { waitFor() } }
                    expectThatProcessAppliesTerminationCallback(null) {
                        failingCommandLine.exec.async.logging(execTerminationCallback = it).apply { waitFor() }
                    }
                }

                @TestFactory
                fun `should log to background by default`() = tests {
                    succeedingCommandLine.exec.async.logging().apply { waitFor() } asserting { BACKGROUND.expectLogged.logsSuccessfulIO("⌛️ ") }
                    failingCommandLine.exec.async.logging().apply { waitFor() } asserting { BACKGROUND.expectLogged.logsFailedIO("⌛️ ") }
                }

                @TestFactory
                fun `should log to specified logger if specified`() = tests {
                    fun logger() = InMemoryLogger(border = NONE)
                    logger().also { succeedingCommandLine.exec.async.logging(it).apply { waitFor() } } asserting { logsSuccessfulIO("⌛️ ", 3) }
                    logger().also { failingCommandLine.exec.async.logging(it).apply { waitFor() } } asserting { logsFailedIO("⌛️ ", 3) }
                }

                @Test
                fun TestLogger.`should log to receiver logger if available`() {
                    with(succeedingCommandLine) { logging.async.exec().apply { waitFor() } }
                    expectLogged.logsSuccessfulIO("⌛️ ")
                }

                @Test
                fun `should apply custom logging options`() {
                    val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                    succeedingCommandLine.exec.async.logging(logger) { block { caption { "custom caption" }; border = SOLID } }.apply { waitFor() }
                    logger.expectLogged.contains("╭──╴custom caption")
                }
            }

            @Nested
            inner class ExecProcessing {

                @TestFactory
                fun `succeeding command line`() = test({
                    succeedingCommandLine.exec.async.processing { }
                }) {
                    expectThatProcess { joined.starts() }
                    expectThatProcess { joined.succeeds() }
                    expectThatProcess { joined.logsSuccessfulIO() }
                    expectThatProcess { runsAsynchronously() }
                    expectThatProcessAppliesExitStateHandler { succeedingCommandLine.exec.async.processing(exitStateHandler = it) {}.apply { waitFor() } }
                    expectThatProcessAppliesTerminationCallback(null) {
                        succeedingCommandLine.exec.async.processing(execTerminationCallback = it) {}.apply { waitFor() }
                    }
                }

                @TestFactory
                fun `failing command line`() = test({
                    failingCommandLine.exec.processing { }
                }) {
                    expectThatProcess { joined.starts() }
                    expectThatProcess { joined.fails() }
                    expectThatProcess { joined.logsFailedIO() }
//                    expectThatProcess { runsAsynchronously() } // too fast
                    expectThatProcessAppliesExitStateHandler { failingCommandLine.exec.async.processing(exitStateHandler = it) {}.apply { waitFor() } }
                    expectThatProcessAppliesTerminationCallback(null) {
                        failingCommandLine.exec.async.processing(execTerminationCallback = it) {}.apply { waitFor() }
                    }
                }

                @TestFactory
                fun TestLogger.`should only log async computation`() = tests {
                    succeedingCommandLine.exec.async.processing(parentLogger = this@`should only log async computation`) {}.apply { waitFor() } asserting {
                        expectLogged.contains("▶ printenv$LF⌛️ async computation")
                    }
                    failingCommandLine.exec.async.processing(parentLogger = this@`should only log async computation`) {}.apply { waitFor() } asserting {
                        expectLogged.contains("▶ exit 42$LF⌛️ async computation")
                    }
                }

                @TestFactory
                fun TestLogger.`should only to receiver logger if available`() = tests {
                    with(succeedingCommandLine) {
                        logging.async.processing(parentLogger = this@`should only to receiver logger if available`) {}.apply { waitFor() }
                    } asserting {
                        expectLogged.contains("▶ printenv$LF⌛️ async computation")
                    }
                    with(failingCommandLine) {
                        logging.async.processing(parentLogger = this@`should only to receiver logger if available`) {}.apply { waitFor() }
                    } asserting {
                        expectLogged.contains("▶ exit 42$LF⌛️ async computation")
                    }
                }

                @Test
                fun `should apply custom logging options`() {
                    val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                    succeedingCommandLine.exec.async.processing(parentLogger = logger,
                        loggingOptionsInit = { block { caption { "custom caption" }; border = SOLID } }) {}.apply { waitFor() }
                    logger.expectLogged.contains("╭──╴custom caption")
                }

                @Test
                fun `should process IO`() {
                    val processed = mutableListOf<IO>()
                    succeedingCommandLine.exec.async.processing { io -> processed.add(io) }.apply { waitFor() }
                    expectThat(processed).contains(IO.OUT typed "TEST_PROP=TEST_VALUE")
                }
            }

            @Smoke @Test
            fun `should apply custom processing options`() {
                val processed = mutableListOf<IO>()
                CommandLine("cat").exec.mode { async(NonInteractive("Hello Cat!$LF".byteInputStream())) }.processing { io -> processed.add(io) }
                    .apply { waitFor() }
                expectThat(processed).contains(IO.OUT typed "Hello Cat!")
            }
        }
    }
}

private class ExitStateHandlerTest : ExitStateHandler {
    override fun handle(terminated: Terminated): ExitState = Success(123L, terminated.io)

    companion object {
        fun DynamicTestsWithSubjectBuilder<*>.expectThatProcessAppliesExitStateHandler(
            exec: (ExitStateHandler) -> Exec,
        ) {
            val exitStateHandler = ExitStateHandlerTest()
            val exitState = exec(exitStateHandler).exitState
            expecting { exitState } that { isA<Success>() }
            expecting { exitState } that { isNotNull().pid.isEqualTo(123L) }
        }
    }
}

private class ExecTerminationTestCallback : ExecTerminationCallback {
    var called: Boolean = false
    var exception: Throwable? = null
    override fun invoke(exception: Throwable?) {
        called = true
        this.exception = exception
    }

    companion object {
        fun DynamicTestsWithSubjectBuilder<*>.expectThatProcessAppliesTerminationCallback(
            expectedException: Throwable?,
            exec: (ExecTerminationCallback) -> Exec,
        ) {
            val callback = ExecTerminationTestCallback()
            exec(callback)
            expecting { callback.called } that { isTrue() }
            expecting { callback.exception } that { isEqualTo(expectedException) }
        }
    }
}

private fun DynamicTestsWithSubjectBuilder<() -> Exec>.expectThatProcess(assertions: Builder<Exec>.() -> Unit) =
    expecting { invoke() }.that(assertions)


inline fun <reified T : Exec> Builder<T>.starts(): Builder<T> =
    assert("has started") {
        when (it.started) {
            true -> pass()
            else -> fail("has not started")
        }
    }

inline val <reified T : Exec> Builder<T>.exited: Builder<ExitState> get() = get("exited") { onExit.get() }.isA()
inline fun <reified T : Exec> Builder<T>.logsIO(curlyPattern: String): Builder<String> = exited.io().toStringMatchesCurlyPattern(curlyPattern)

@JvmName("logsIOInMemoryLogger")
inline fun <reified T : InMemoryLogger> Builder<T>.logsIO(ignorePrefix: String, curlyPattern: String, dropFirst: Int = 2, dropLast: Int = 1): Builder<String> =
    get { toString(null, false, dropFirst).lines().dropLast(dropLast).joinToString(LF) }.logsIO(ignorePrefix, curlyPattern)

@JvmName("logsIOString")
fun Builder<String>.logsIO(ignorePrefix: String, curlyPattern: String): Builder<String> =
    get { mapLines { it.withoutPrefix(ignorePrefix) } }.toStringMatchesCurlyPattern(curlyPattern)


inline fun <reified T : Exec> Builder<T>.succeeds(): Builder<Success> = exited.isA()
inline fun <reified T : Exec> Builder<T>.logsSuccessfulIO(): Builder<String> = logsIO(successfulIO)

@JvmName("logsSuccessfulIOInMemoryLogger")
inline fun <reified T : InMemoryLogger> Builder<T>.logsSuccessfulIO(ignorePrefix: String = "· ", dropFirst: Int = 2): Builder<String> =
    logsIO(ignorePrefix, successfulIO, dropFirst)

@JvmName("logsSuccessfulIOString")
private fun Builder<String>.logsSuccessfulIO(ignorePrefix: String = "· "): Builder<String> =
    logsIO(ignorePrefix, "{{}}$LF$successfulIO$LF{{}}")

val successfulIO = """
    Executing printenv
    {{}}
    TEST_PROP=TEST_VALUE
    {{}}
    Process {} terminated successfully at {}
""".trimIndent()

inline fun <reified T : Exec> Builder<T>.fails(): Builder<Failure> =
    exited.isA<Failure>().assert("unsuccessfully with non-zero exit code") {
        val actual = it.exitCode
        when (actual != 0) {
            true -> pass()
            else -> fail("completed successfully")
        }
    }

inline fun <reified T : Exec> Builder<T>.logsFailedIO(): Builder<String> = logsIO(failedIO) and { containsDump(containedStrings = emptyArray()) }

@JvmName("logsFailedIOInMemoryLogger")
inline fun <reified T : InMemoryLogger> Builder<T>.logsFailedIO(ignorePrefix: String = "· ", dropFirst: Int = 2): Builder<String> =
    logsIO(ignorePrefix, failedIO, dropFirst) and { containsDump(containedStrings = emptyArray()) }

@JvmName("logsFailedIOString")
private fun Builder<String>.logsFailedIO(ignorePrefix: String = "· "): Builder<String> =
    logsIO(ignorePrefix, "{{}}$LF$failedIO$LF{{}}") and { containsDump(containedStrings = emptyArray()) }

val failedIO = """
    Executing exit 42
    Process {} terminated with exit code 42
    {{}}
""".trimIndent()

inline val Builder<out Process>.exitState get() = get("exit state") { exitState }
inline fun <reified T : Exec> Builder<T>.runsSynchronously(): Builder<ExitState> = exitState.isNotNull()
inline fun <reified T : Exec> Builder<T>.runsAsynchronously(): Builder<Nothing> = exitState.isNull()


inline val <reified T : Process> Builder<T>.joined: Builder<T>
    get() = get("joined using waitFor") { also { waitFor() } }
