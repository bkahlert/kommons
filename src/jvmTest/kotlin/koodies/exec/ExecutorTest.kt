package koodies.exec

import koodies.concurrent.process.IO
import koodies.concurrent.process.ProcessingMode.Interactivity.NonInteractive
import koodies.concurrent.process.output
import koodies.exec.ExecTerminationTestCallback.Companion.expectThatProcessAppliesTerminationCallback
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Failure
import koodies.exec.Process.ExitState.Success
import koodies.io.path.Locations
import koodies.logging.FixedWidthRenderingLogger.Border.NONE
import koodies.logging.FixedWidthRenderingLogger.Border.SOLID
import koodies.logging.InMemoryLogger
import koodies.logging.LoggingContext.Companion.BACKGROUND
import koodies.logging.RenderingLogger.Companion.withUnclosedWarningDisabled
import koodies.logging.expectLogged
import koodies.shell.ShellScript
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
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class ExecutorTest {

    private val succeedingExecutable = CommandLine(mapOf("TEST_PROP" to "TEST_VALUE"), Locations.Temp, "printenv", "TEST_PROP")
    private val failingExecutable = CommandLine(Locations.Temp, "exit", "42")

    @Smoke @Nested
    inner class Executables {

        @Test
        fun `should exec command line`() {
            val exec = CommandLine(Locations.Temp, "echo", "Hello, Command Line!").exec()
            expectThat(exec.io.output.ansiRemoved)
                .isEqualTo("Hello, Command Line!")
        }

        @Test
        fun `should exec shell script`() {
            val exec = ShellScript { !"echo 'Hello, Shell Script!' | cat" }.exec()
            expectThat(exec.io.output.ansiRemoved)
                .isEqualTo("Hello, Shell Script!")
        }

        @Test
        fun InMemoryLogger.`should add missing shebang`() {
            val exec = ShellScript { !"cat $0" }.exec.logging(this)
            expectThat(exec.io.output.ansiRemoved).isEqualTo("#!/bin/sh\ncat \$0")
        }
    }

    @Nested
    inner class ExecSync {

        @Nested
        inner class ExecOnly {

            @TestFactory
            fun `succeeding command line`() = test({
                succeedingExecutable.exec()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { succeedingExecutable.exec(execTerminationCallback = it) }
            }

            @TestFactory
            fun `failing command line`() = test({
                failingExecutable.exec()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { logsFailedIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { failingExecutable.exec(execTerminationCallback = it) }
            }
        }

        @Nested
        inner class ExecLogging {

            @TestFactory
            fun `succeeding command line`() = test({
                succeedingExecutable.exec.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { succeedingExecutable.exec.logging(execTerminationCallback = it) }
            }

            @TestFactory
            fun `failing command line`() = test({
                failingExecutable.exec.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { logsFailedIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { failingExecutable.exec.logging(execTerminationCallback = it) }
            }

            @TestFactory
            fun `should log to background by default`() = tests {
                succeedingExecutable.exec.logging() asserting { BACKGROUND.expectLogged.logsSuccessfulIO() }
                failingExecutable.exec.logging() asserting { BACKGROUND.expectLogged.logsFailedIO() }
            }

            @TestFactory
            fun `should log to specified logger if specified`() = tests {
                fun logger() = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                logger().also { succeedingExecutable.exec.logging(it) } asserting { logsSuccessfulIO() }
                logger().also { failingExecutable.exec.logging(it) } asserting { logsFailedIO() }
            }

            @Test
            fun TestLogger.`should log to receiver logger if available`() {
                with(succeedingExecutable) { logging() }
                expectLogged.logsSuccessfulIO()
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                succeedingExecutable.exec.logging(logger) { block { caption { "custom caption" }; border = SOLID } }
                logger.expectLogged.contains("╭──╴custom caption")
            }
        }

        @Nested
        inner class ExecProcessing {

            @TestFactory
            fun `succeeding command line`() = test({
                succeedingExecutable.exec.processing { }
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { succeedingExecutable.exec.processing(execTerminationCallback = it) {} }
            }

            @TestFactory
            fun `failing command line`() = test({
                failingExecutable.exec.processing { }
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { logsFailedIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { failingExecutable.exec.processing(execTerminationCallback = it) {} }
            }

            @Test
            fun TestLogger.`should only log success`() {
                succeedingExecutable.exec.processing(parentLogger = this) {}
                expectLogged.isEqualTo("printenv TEST_PROP ✔︎")
            }

            @Test
            fun TestLogger.`should log dump on failure`() {
                failingExecutable.exec.processing(parentLogger = this) {}
                expectLogged.containsDump(*emptyArray())
            }

            @Test
            fun TestLogger.`should log to receiver logger if available`() {
                with(succeedingExecutable) { logging.processing {} }
                expectLogged.isEqualTo("printenv TEST_PROP ✔︎")
            }

            @Test
            fun TestLogger.`should dump to receiver logger if available`() {
                with(failingExecutable) { logging.processing {} }
                expectLogged.containsDump(*emptyArray())
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                succeedingExecutable.exec.processing(parentLogger = logger,
                    loggingOptionsInit = { block { caption { "custom caption" }; border = SOLID } }) {}
                logger.expectLogged.contains("╭──╴custom caption")
            }

            @Test
            fun `should process IO`() {
                val processed = mutableListOf<IO>()
                succeedingExecutable.exec.processing { io -> processed.add(io) }
                expectThat(processed).contains(IO.Output typed "TEST_VALUE")
            }
        }
    }

    @Nested
    inner class ExecAsync {

        @Nested
        inner class ExecOnly {

            @TestFactory
            fun `succeeding command line`() = test({
                succeedingExecutable.exec.async()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { succeedingExecutable.exec.async(execTerminationCallback = it).apply { waitFor() } }
            }

            @TestFactory
            fun `failing command line`() = test({
                failingExecutable.exec.async()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.logsFailedIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { failingExecutable.exec.async(execTerminationCallback = it).apply { waitFor() } }
            }
        }

        @Nested
        inner class ExecLogging {

            @TestFactory
            fun `succeeding command line`() = test({
                succeedingExecutable.exec.async.logging()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    succeedingExecutable.exec.async.logging(execTerminationCallback = it).apply { waitFor() }
                }
            }

            @TestFactory
            fun `failing command line`() = test({
                failingExecutable.exec.async.logging()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.logsFailedIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    failingExecutable.exec.async.logging(execTerminationCallback = it).apply { waitFor() }
                }
            }

            @TestFactory
            fun `should log to background by default`() = tests {
                succeedingExecutable.exec.async.logging().apply { waitFor() } asserting { BACKGROUND.expectLogged.logsSuccessfulIO("⌛️ ") }
                failingExecutable.exec.async.logging().apply { waitFor() } asserting { BACKGROUND.expectLogged.logsFailedIO("⌛️ ") }
            }

            @TestFactory
            fun `should log to specified logger if specified`() = tests {
                fun logger() = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                logger().also { succeedingExecutable.exec.async.logging(it).apply { waitFor() } } asserting { logsSuccessfulIO("⌛️ ", 3) }
                logger().also { failingExecutable.exec.async.logging(it).apply { waitFor() } } asserting { logsFailedIO("⌛️ ", 3) }
            }

            @Test
            fun TestLogger.`should log to receiver logger if available`() {
                with(succeedingExecutable) { logging.async().apply { waitFor() } }
                expectLogged.logsSuccessfulIO("⌛️ ")
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                succeedingExecutable.exec.async.logging(logger) { block { caption { "custom caption" }; border = SOLID } }.apply { waitFor() }
                logger.expectLogged.contains("╭──╴custom caption")
            }
        }

        @Nested
        inner class ExecProcessing {

            @TestFactory
            fun `succeeding command line`() = test({
                succeedingExecutable.exec.async.processing { }
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    succeedingExecutable.exec.async.processing(execTerminationCallback = it) {}.apply { waitFor() }
                }
            }

            @TestFactory
            fun `failing command line`() = test({
                failingExecutable.exec.processing { }
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.logsFailedIO() }
//                    expectThatProcess { runsAsynchronously() } // too fast
                expectThatProcessAppliesTerminationCallback(null) {
                    failingExecutable.exec.async.processing(execTerminationCallback = it) {}.apply { waitFor() }
                }
            }

            @TestFactory
            fun TestLogger.`should only log async computation`() = tests {
                succeedingExecutable.exec.async.processing(parentLogger = this@`should only log async computation`) {}.apply { waitFor() } asserting {
                    expectLogged.contains("▶ printenv TEST_PROP$LF⌛️ async computation")
                }
                failingExecutable.exec.async.processing(parentLogger = this@`should only log async computation`) {}.apply { waitFor() } asserting {
                    expectLogged.contains("▶ exit 42$LF⌛️ async computation")
                }
            }

            @TestFactory
            fun TestLogger.`should only to receiver logger if available`() = tests {
                with(succeedingExecutable) {
                    logging.async.processing(parentLogger = this@`should only to receiver logger if available`) {}.apply { waitFor() }
                } asserting {
                    expectLogged.contains("▶ printenv TEST_PROP$LF⌛️ async computation")
                }
                with(failingExecutable) {
                    logging.async.processing(parentLogger = this@`should only to receiver logger if available`) {}.apply { waitFor() }
                } asserting {
                    expectLogged.contains("▶ exit 42$LF⌛️ async computation")
                }
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                succeedingExecutable.exec.async.processing(parentLogger = logger,
                    loggingOptionsInit = { block { caption { "custom caption" }; border = SOLID } }) {}.apply { waitFor() }
                logger.expectLogged.contains("╭──╴custom caption")
            }

            @Test
            fun `should process IO`() {
                val processed = mutableListOf<IO>()
                succeedingExecutable.exec.async.processing { io -> processed.add(io) }.apply { waitFor() }
                expectThat(processed).contains(IO.Output typed "TEST_VALUE")
            }
        }

        @Smoke @Test
        fun `should apply custom processing options`() {
            val processed = mutableListOf<IO>()
            CommandLine("cat").exec.mode { async(NonInteractive("Hello Cat!$LF".byteInputStream())) }.processing { io -> processed.add(io) }
                .apply { waitFor() }
            expectThat(processed).contains(IO.Output typed "Hello Cat!")
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
    Executing printenv TEST_PROP
    TEST_VALUE
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
