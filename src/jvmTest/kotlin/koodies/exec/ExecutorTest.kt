package koodies.exec

import koodies.exec.ExecTerminationTestCallback.Companion.expectThatProcessAppliesTerminationCallback
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.Failure
import koodies.exec.Process.ExitState.Success
import koodies.exec.ProcessingMode.Interactivity.NonInteractive
import koodies.io.path.Locations
import koodies.io.path.pathString
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
import koodies.text.LineSeparators.mapLines
import koodies.text.randomString
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

    private val executable = CommandLine("printenv", "TEST_PROP")

    private inline val Executor<out Exec>.testProp: Executor<out Exec> get() = env("TEST_PROP", "TEST_VALUE")

    @Smoke @Nested
    inner class Executables {

        @Test
        fun `should exec command line`() {
            val exec = CommandLine("echo", "Hello, Command Line!").exec()
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
        fun `should exec using specified environment`() {
            val random = randomString()
            val exec = CommandLine("printenv", "RANDOM_PROP").exec.env("RANDOM_PROP", random)(Locations.Temp)
            expectThat(exec.io.output.ansiRemoved).isEqualTo(random)
        }

        @Test
        fun `should exec using specified working directory`() {
            val exec = CommandLine("pwd").exec(Locations.Temp)
            val tempPaths = setOf(Locations.Temp.pathString, Locations.Temp.toRealPath().pathString)
            expectThat(tempPaths).contains(exec.io.output.ansiRemoved)
        }
    }

    @Nested
    inner class ExecSync {

        @Nested
        inner class ExecOnly {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec(execTerminationCallback = it) }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { logsFailedIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec(execTerminationCallback = it) }
            }
        }

        @Nested
        inner class ExecLogging {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.testProp.logging(execTerminationCallback = it) }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.logging()
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { logsFailedIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.logging(execTerminationCallback = it) }
            }

            @TestFactory
            fun `should log to background by default`() = tests {
                executable.exec.testProp.logging() asserting { BACKGROUND.expectLogged.logsSuccessfulIO() }
                executable.exec.logging() asserting { BACKGROUND.expectLogged.logsFailedIO() }
            }

            @TestFactory
            fun `should log to specified logger if specified`() = tests {
                fun logger() = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                logger().also { executable.exec.testProp.logging(it) } asserting { logsSuccessfulIO() }
                logger().also { executable.exec.logging(it) } asserting { logsFailedIO() }
            }

            @Test
            fun TestLogger.`should log to receiver logger if available`() {
                with(executable) { logging.testProp() }
                expectLogged.logsSuccessfulIO()
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                executable.exec.testProp.logging(logger) { block { caption { "custom caption" }; border = SOLID } }
                logger.expectLogged.contains("╭──╴custom caption")
            }
        }

        @Nested
        inner class ExecProcessing {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.processing { }
            }) {
                expectThatProcess { starts() }
                expectThatProcess { succeeds() }
                expectThatProcess { logsSuccessfulIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.testProp.processing(execTerminationCallback = it) {} }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.processing { }
            }) {
                expectThatProcess { starts() }
                expectThatProcess { fails() }
                expectThatProcess { logsFailedIO() }
                expectThatProcess { runsSynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.processing(execTerminationCallback = it) {} }
            }

            @Test
            fun TestLogger.`should only log success`() {
                executable.exec.testProp.processing(parentLogger = this) {}
                expectLogged.isEqualTo("printenv TEST_PROP ✔︎")
            }

            @Test
            fun TestLogger.`should log dump on failure`() {
                executable.exec.processing(parentLogger = this) {}
                expectLogged.containsDump(*emptyArray())
            }

            @Test
            fun TestLogger.`should log to receiver logger if available`() {
                with(executable) { logging.testProp.processing {} }
                expectLogged.isEqualTo("printenv TEST_PROP ✔︎")
            }

            @Test
            fun TestLogger.`should dump to receiver logger if available`() {
                with(executable) { logging.processing {} }
                expectLogged.containsDump(*emptyArray())
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                executable.exec.testProp.processing(parentLogger = logger,
                    loggingOptionsInit = { block { caption { "custom caption" }; border = SOLID } }) {}
                logger.expectLogged.contains("╭──╴custom caption")
            }

            @Test
            fun `should process IO`() {
                val processed = mutableListOf<IO>()
                executable.exec.testProp.processing { io -> processed.add(io) }
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
                executable.exec.testProp.async()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.testProp.async(execTerminationCallback = it).apply { waitFor() } }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.async()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.logsFailedIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) { executable.exec.async(execTerminationCallback = it).apply { waitFor() } }
            }
        }

        @Nested
        inner class ExecLogging {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.async.testProp.logging()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.testProp.logging(execTerminationCallback = it).apply { waitFor() }
                }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.async.logging()
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.logsFailedIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.logging(execTerminationCallback = it).apply { waitFor() }
                }
            }

            @TestFactory
            fun `should log to background by default`() = tests {
                executable.exec.async.testProp.logging().apply { waitFor() } asserting { BACKGROUND.expectLogged.logsSuccessfulIO("⏳️ ") }
                executable.exec.async.logging().apply { waitFor() } asserting { BACKGROUND.expectLogged.logsFailedIO("⏳️ ") }
            }

            @TestFactory
            fun `should log to specified logger if specified`() = tests {
                fun logger() = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                logger().also { executable.exec.testProp.async.logging(it).apply { waitFor() } } asserting { logsSuccessfulIO("⏳️ ", 3) }
                logger().also { executable.exec.async.logging(it).apply { waitFor() } } asserting { logsFailedIO("⏳️ ", 3) }
            }

            @Test
            fun TestLogger.`should log to receiver logger if available`() {
                with(executable) { logging.testProp.async().apply { waitFor() } }
                expectLogged.logsSuccessfulIO("⏳️ ")
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                executable.exec.async.testProp.logging(logger) { block { caption { "custom caption" }; border = SOLID } }.apply { waitFor() }
                logger.expectLogged.contains("╭──╴custom caption")
            }
        }

        @Nested
        inner class ExecProcessing {

            @TestFactory
            fun `succeeding command line`() = test({
                executable.exec.testProp.async.processing { }
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.succeeds() }
                expectThatProcess { joined.logsSuccessfulIO() }
                expectThatProcess { runsAsynchronously() }
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.testProp.processing(execTerminationCallback = it) {}.apply { waitFor() }
                }
            }

            @TestFactory
            fun `failing command line`() = test({
                executable.exec.processing { }
            }) {
                expectThatProcess { joined.starts() }
                expectThatProcess { joined.fails() }
                expectThatProcess { joined.logsFailedIO() }
//                    expectThatProcess { runsAsynchronously() } // too fast
                expectThatProcessAppliesTerminationCallback(null) {
                    executable.exec.async.processing(execTerminationCallback = it) {}.apply { waitFor() }
                }
            }

            @TestFactory
            fun TestLogger.`should only log async computation`() = tests {
                executable.exec.async.testProp.processing(parentLogger = this@`should only log async computation`) {}.apply { waitFor() } asserting {
                    expectLogged.contains("▶ printenv TEST_PROP$LF⏳️ async computation")
                }
                executable.exec.async.processing(parentLogger = this@`should only log async computation`) {}.apply { waitFor() } asserting {
                    expectLogged.contains("▶ printenv TEST_PROP$LF⏳️ async computation")
                }
            }

            @TestFactory
            fun TestLogger.`should only to receiver logger if available`() = tests {
                with(executable) {
                    logging.async.testProp.processing(parentLogger = this@`should only to receiver logger if available`) {}.apply { waitFor() }
                } asserting {
                    expectLogged.contains("▶ printenv TEST_PROP$LF⏳️ async computation")
                }
                with(executable) {
                    logging.async.processing(parentLogger = this@`should only to receiver logger if available`) {}.apply { waitFor() }
                } asserting {
                    expectLogged.contains("▶ printenv TEST_PROP$LF⏳️ async computation")
                }
            }

            @Test
            fun `should apply custom logging options`() {
                val logger = InMemoryLogger(border = NONE).withUnclosedWarningDisabled
                executable.exec.async.testProp.processing(parentLogger = logger,
                    loggingOptionsInit = { block { caption { "custom caption" }; border = SOLID } }) {}.apply { waitFor() }
                logger.expectLogged.contains("╭──╴custom caption")
            }

            @Test
            fun `should process IO`() {
                val processed = mutableListOf<IO>()
                executable.exec.async.testProp.processing { io -> processed.add(io) }.apply { waitFor() }
                expectThat(processed).contains(IO.Output typed "TEST_VALUE")
            }
        }

        @Smoke @Test
        fun `should apply custom processing options`() {
            val processed = mutableListOf<IO>()
            CommandLine("cat").exec.testProp.mode { async(NonInteractive("Hello Cat!$LF".byteInputStream())) }.processing { io -> processed.add(io) }
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
        kotlin.runCatching { it.pid }.fold({
            if (it != 0L) pass()
            else fail(it, "non-ß PID expected")
        }, {
            fail(cause = it)
        })
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
    Executing printenv TEST_PROP
    Process {} terminated with exit code 1
    {{}}
""".trimIndent()

inline val Builder<out Process>.exitState get() = get("exit state") { exitState }
inline fun <reified T : Exec> Builder<T>.runsSynchronously(): Builder<ExitState> = exitState.isNotNull()
inline fun <reified T : Exec> Builder<T>.runsAsynchronously(): Builder<Nothing> = exitState.isNull()


inline val <reified T : Process> Builder<T>.joined: Builder<T>
    get() = get("joined using waitFor") { also { waitFor() } }
